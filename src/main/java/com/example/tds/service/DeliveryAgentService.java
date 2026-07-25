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
import org.springframework.beans.factory.annotation.Value;
import com.example.tds.repository.DeliveryAgentRepository;
import com.example.tds.dto.responses.DeliveryAgentResponse;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.dto.requests.common.OtpVerifyRequest;
import com.example.tds.dto.requests.common.UpdateNameRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.example.tds.dto.requests.common.OtpGenerationRequest;
import com.example.tds.repository.OrderRepository;
import com.example.tds.repository.OrderRouteRepository;
import com.example.tds.entity.OrderEntity;
import com.example.tds.entity.OrderRouteEntity;
import com.example.tds.entity.SubscriptionEntity;
import com.example.tds.entity.ChefEntity;
import com.example.tds.entity.UserEntity;
import com.example.tds.enums.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.tds.dto.responses.GoogleMapsRouteResponse;
import com.example.tds.dto.responses.LocationResponse;

import org.springframework.data.geo.*;
import com.example.tds.enums.DeliveryAgentStatus;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryAgentService {
    private final JwtUtility jwt;
    private final StringRedisTemplate template;
    private final OrderRepository orderRepository;
    private final DeliveryAgentMapper deliveryAgentMapper;
    private final OrderRouteRepository orderRouteRepository;
    private final GeoOperations<String, UUID> geoOperations;
    private final DeliveryAgentRepository deliveryAgentRepository;
    private final GoogleMapsRoutingService googleMapsRoutingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${order.channel.name}")
    private String orderChannelName;

    private final String deliveryAgentsKeyName = "delivery_agents_location";

    public static final String AGENT_LOCATION_CHANNEL_PREFIX = "agent_location_";
    public static final String CLOSE_CONNECTION_MESSAGE_PREFIX = "CLOSE_";

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

    @Transactional
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

    @Transactional
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

    public void updateDeliveryAgentLocation(double longitude, double latitude, UUID deliveryAgentId) {
        geoOperations.add(
                deliveryAgentsKeyName,
                new Point(longitude, latitude),
                deliveryAgentId
        );

        try {
            LocationResponse location = LocationResponse.builder()
                    .longitude(longitude)
                    .latitude(latitude)
                    .build();

            String locationJson = objectMapper.writeValueAsString(location);
            template.convertAndSend(AGENT_LOCATION_CHANNEL_PREFIX + deliveryAgentId, locationJson);
        } catch (Exception e) {
            log.error("Failed to publish location to redis", e);
        }
    }

    public LocationResponse getDeliveryAgentLocation(UUID deliveryAgentId) {
        List<Point> positions = geoOperations.position(deliveryAgentsKeyName, deliveryAgentId);

        if (positions != null && !positions.isEmpty() && positions.get(0) != null) {
            Point location = positions.get(0);
            return LocationResponse.builder()
                    .longitude(location.getX())
                    .latitude(location.getY())
                    .build();
        }

        throw new ResourceNotFoundException("Location for delivery agent not found");
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

        int minRadius = Math.max(radiusKm - 1, 0);

        List<UUID> deliveryAgentIds = ids.getContent().stream()
                .filter(result -> {
                    Distance distance = result.getDistance();
                    return distance.getValue() > minRadius && distance.getValue() <= radiusKm;
                })
                .map(result -> result.getContent().getName())
                .toList();

        return deliveryAgentRepository.findByDeliveryAgentIdInAndStatus(
                deliveryAgentIds,
                DeliveryAgentStatus.ACTIVE
        );
    }

    public void handleUpdateFcmToken(UUID deliveryAgentId, String fcmToken) {
        DeliveryAgentEntity agent = deliveryAgentRepository.findByDeliveryAgentId(deliveryAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Agent not found"));

        agent.setFcmToken(fcmToken);

        deliveryAgentRepository.save(agent);
    }

    public void publishDeliveryRequest(UUID deliveryAgentId, UUID orderId) {
        String channelName = orderChannelName + "_" + orderId;
        template.convertAndSend(channelName, "delivery_agent_" + deliveryAgentId);
    }

    @Transactional
    public GoogleMapsRouteResponse getRouteForOrder(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus targetStatus = order.getStatus();

        if(targetStatus != OrderStatus.ASSIGNED && targetStatus != OrderStatus.PICKED_UP) {
            return null;
        }

        OrderRouteEntity routeEntity = orderRouteRepository.findByOrder_OrderIdAndStatusFor(orderId, targetStatus).orElse(null);

        if (routeEntity != null) {
            GoogleMapsRouteResponse response = new GoogleMapsRouteResponse();
            response.setDistanceInMeters(routeEntity.getDistanceMeters());
            response.setDurationInSeconds(routeEntity.getDurationSeconds());
            response.setEncodedPolyline(routeEntity.getEncodedPolyline());
            response.setInstructions(routeEntity.getInstructions());
            return response;
        }

        SubscriptionEntity subscription = order.getSubscription();
        UserEntity user = subscription.getUser();
        ChefEntity chef = subscription.getMealPlan().getChef();

        double startLng;
        double startLat;
        double endLng;
        double endLat;

        if (targetStatus == OrderStatus.ASSIGNED) {
            if (order.getDeliveryAgent() == null) {
                throw new BadRequestException("Delivery agent not assigned yet");
            }

            LocationResponse agentLocation = getDeliveryAgentLocation(order.getDeliveryAgent().getDeliveryAgentId());
            startLng = agentLocation.getLongitude();
            startLat = agentLocation.getLatitude();
            endLng = chef.getLocation().getX();
            endLat = chef.getLocation().getY();
        } else {
            startLng = chef.getLocation().getX();
            startLat = chef.getLocation().getY();
            endLng = user.getLocation().getX();
            endLat = user.getLocation().getY();
        }

        GoogleMapsRouteResponse googleMapsRouteResponse = googleMapsRoutingService.getRoute(startLng, startLat, endLng, endLat);

        OrderRouteEntity newRouteEntity = OrderRouteEntity.builder()
                .order(order)
                .statusFor(targetStatus)
                .encodedPolyline(googleMapsRouteResponse.getEncodedPolyline())
                .distanceMeters((int) googleMapsRouteResponse.getDistanceInMeters())
                .durationSeconds(googleMapsRouteResponse.getDurationInSeconds())
                .instructions(googleMapsRouteResponse.getInstructions())
                .build();

        orderRouteRepository.save(newRouteEntity);

        return googleMapsRouteResponse;
    }
}
