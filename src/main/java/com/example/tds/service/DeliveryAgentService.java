package com.example.tds.service;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.utilities.JwtUtility;
import org.springframework.stereotype.Service;
import com.example.tds.mapper.DeliveryAgentMapper;
import com.example.tds.entity.DeliveryAgentEntity;
import com.example.tds.exception.BadRequestException;
import com.example.tds.exception.UnauthorizedException;
import com.example.tds.repository.DeliveryAgentRepository;
import com.example.tds.dto.responses.DeliveryAgentResponse;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.dto.requests.common.OtpVerifyRequest;
import com.example.tds.dto.requests.common.UpdateNameRequest;
import com.example.tds.dto.requests.common.OtpGenerationRequest;
import com.example.tds.dto.requests.common.UpdateAgentLocationRequest;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.geo.*;
import com.example.tds.enums.DeliveryAgentCurrentStatus;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryAgentService {
    private final JwtUtility jwt;
    private final DeliveryAgentMapper deliveryAgentMapper;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final GeoOperations<String, UUID> geoOperations;

    private final String deliveryAgentsKeyName = "delivery_agents_location";

    long refreshTokenExpiry = 7 * 24 * 60 * 60 * 1000L;
    long accessTokenExpiry = 3 * 60 * 60 * 1000L;

    public DeliveryAgentResponse handleUpdateName(UUID deliveryAgentId, UpdateNameRequest updateNameRequest) {
        String name = updateNameRequest.getName();

        DeliveryAgentEntity existingAgent = deliveryAgentRepository.findById(deliveryAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Agent not found"));

        existingAgent.setName(name);

        deliveryAgentRepository.save(existingAgent);

        return deliveryAgentMapper.toDeliveryAgentResponse(existingAgent);
    }

    public void handleOtpGeneration(OtpGenerationRequest generationRequest) {
        String mobileNo = generationRequest.getMobileNo();
        DeliveryAgentEntity existingAgent = deliveryAgentRepository.findByMobileNo(mobileNo)
                .orElse(null);

        if (existingAgent == null) {
            DeliveryAgentEntity newAgent = new DeliveryAgentEntity();
            newAgent.setMobileNo(mobileNo);
            existingAgent = deliveryAgentRepository.save(newAgent);
        }

        int otp = ThreadLocalRandom.current().nextInt(1000, 10000);
        existingAgent.setOtp(otp);

        deliveryAgentRepository.save(existingAgent);

        // TODO: Send OTP through email or sms
    }

    public DeliveryAgentResponse handleOtpVerification(OtpVerifyRequest otpVerifyRequest) {
        String mobileNo = otpVerifyRequest.getMobileNo();
        int otp = otpVerifyRequest.getOtp();

        DeliveryAgentEntity agent = deliveryAgentRepository.findByMobileNo(mobileNo)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Agent not found"));

        if (agent.getOtp() == null || agent.getOtp() != otp) {
            throw new BadRequestException("Invalid OTP");
        }

        agent.setOtp(null);

        deliveryAgentRepository.save(agent);

        Map<String, Object> claims = Map.of(
                "deliveryAgentId", agent.getDeliveryAgentId(),
                "mobileNo", agent.getMobileNo()
        );

        String refreshToken = jwt.generateToken(agent.getDeliveryAgentId(), claims, refreshTokenExpiry);
        String accessToken = jwt.generateToken(agent.getDeliveryAgentId(), claims, accessTokenExpiry);

        DeliveryAgentResponse response = deliveryAgentMapper.toDeliveryAgentResponse(agent);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }

    public DeliveryAgentResponse handleGetAgent(DeliveryAgentEntity agentEntity) {
        return deliveryAgentMapper.toDeliveryAgentResponse(agentEntity);
    }

    public DeliveryAgentResponse handleRefresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new UnauthorizedException("Refresh token not found. Please login again.");
        }

        if (!jwt.validateToken(refreshToken)) {
            throw new UnauthorizedException("Refresh token invalid or expired. Please login again.");
        }

        Claims claims = jwt.extractAllClaims(refreshToken);

        String newRefreshToken = jwt.generateToken(claims.getSubject(), claims, refreshTokenExpiry);
        String newAccessToken = jwt.generateToken(claims.getSubject(), claims, accessTokenExpiry);

        return DeliveryAgentResponse
                .builder()
                .refreshToken(newRefreshToken)
                .accessToken(newAccessToken)
                .build();
    }

    public DeliveryAgentResponse handleUpdateLocation(UUID deliveryAgentId, UpdateAgentLocationRequest locationRequest) {
        DeliveryAgentEntity existingAgent = deliveryAgentRepository.findById(deliveryAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Agent not found"));

        deliveryAgentRepository.updateLocation(
                deliveryAgentId,
                locationRequest.getLongitude(),
                locationRequest.getLatitude()
        );

        DeliveryAgentEntity updatedAgent = deliveryAgentRepository.findByMobileNo(existingAgent.getMobileNo())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Agent not found"));

        return deliveryAgentMapper.toDeliveryAgentResponse(updatedAgent);
    }

    public void updateDeliveryAgentLocation(double longitude, double latitude, UUID deliveryAgentId) {
        geoOperations.add(
                deliveryAgentsKeyName,
                new Point(longitude, latitude),
                deliveryAgentId
        );
    }

    public List<DeliveryAgentEntity> findNearbyActiveAgents(Point location, int radiusKm) {
        Circle circle = new Circle(
                new Point(location.getX(), location.getY()),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        GeoRadiusCommandArgs args = GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeCoordinates()
                .includeDistance()
                .sortAscending();

        GeoResults<GeoLocation<UUID>> ids = geoOperations.radius(
                deliveryAgentsKeyName,
                circle,
                args
        );

        if (ids == null || ids.getContent().isEmpty()) {
            return List.of();
        }

        List<UUID> deliveryAgentIds = ids.getContent().stream()
                .map(result -> result.getContent().getName())
                .toList();

        return deliveryAgentRepository.findByDeliveryAgentIdInAndCurrentStatus(
                deliveryAgentIds,
                DeliveryAgentCurrentStatus.ACTIVE
        );
    }
}
