package com.example.tds.service;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.entity.ChefEntity;
import com.example.tds.mapper.ChefMapper;
import com.example.tds.utilities.JwtUtility;
import org.springframework.stereotype.Service;
import com.example.tds.repository.ChefRepository;
import com.example.tds.dto.responses.ChefResponse;
import com.example.tds.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;
import com.example.tds.exception.UnauthorizedException;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.dto.requests.common.OtpVerifyRequest;
import com.example.tds.dto.requests.common.UpdateNameRequest;
import com.example.tds.dto.requests.common.OtpGenerationRequest;
import org.springframework.transaction.annotation.Transactional;
import com.example.tds.dto.requests.common.UpdateLocationRequest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChefService {
    private final JwtUtility jwt;
    private final ChefMapper chefMapper;
    private final ChefRepository chefRepository;
    private final StorageService storageService;

    long refreshTokenExpiry = 7 * 24 * 60 * 60 * 1000L;
    long accessTokenExpiry = 3 * 60 * 60 * 1000L;

    public ChefResponse handleUpdateName(UUID chefId, UpdateNameRequest updateNameRequest) {
        String name = updateNameRequest.getName();

        ChefEntity existingChef = chefRepository.findById(chefId)
                .orElseThrow(()-> new ResourceNotFoundException("Chef not found"));

        existingChef.setName(name);

        chefRepository.save(existingChef);

        return chefMapper.toChefResponse(existingChef);
    }

    @Transactional
    public void handleOtpGeneration(OtpGenerationRequest generationRequest){
        String mobileNo = generationRequest.getMobileNo();
        ChefEntity existingChef = chefRepository.findByMobileNo(mobileNo)
                .orElse(null);

        if(existingChef == null){
            ChefEntity newChef = new ChefEntity();
            newChef.setMobileNo(mobileNo);

            existingChef = chefRepository.save(newChef);
        }

        int otp = ThreadLocalRandom.current().nextInt(1000, 10000);
        existingChef.setOtp(otp);

        chefRepository.save(existingChef);

        // TODO: Send OTP through email or sms
    }

    @Transactional
    public ChefResponse handleOtpVerification(OtpVerifyRequest otpVerifyRequest){
        String mobileNo = otpVerifyRequest.getMobileNo();
        int otp = otpVerifyRequest.getOtp();

        ChefEntity chef = chefRepository.findByMobileNo(mobileNo)
                .orElseThrow(()->new ResourceNotFoundException("Chef not found"));

        if(chef.getOtp() != otp){
            throw new BadRequestException("Invalid OTP");
        }

        chef.setOtp(null);
        chef.setIsVerified(true);

        chefRepository.save(chef);

        Map<String, Object> claims = Map.of(
                "chefId", chef.getChefId(),
                "mobileNo", chef.getMobileNo()
        );

        String refreshToken = jwt.generateToken(chef.getChefId(), claims, refreshTokenExpiry);
        String accessToken = jwt.generateToken(chef.getChefId(), claims, accessTokenExpiry);

        ChefResponse response = chefMapper.toChefResponse(chef);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }

    public ChefResponse handleGetChef(ChefEntity chefEntity){
        return chefMapper.toChefResponse(chefEntity);
    }

    public ChefResponse handleRefresh(String refreshToken){
        if(refreshToken == null || refreshToken.isEmpty()){
            throw new UnauthorizedException("Refresh token not found. Please login again.");
        }

        if(!jwt.validateToken(refreshToken)){
            throw new UnauthorizedException("Refresh token invalid or expired. Please login again.");
        }

        Claims claims = jwt.extractAllClaims(refreshToken);

        String newRefreshToken = jwt.generateToken(claims.getSubject(), claims, refreshTokenExpiry);
        String newAccessToken = jwt.generateToken(claims.getSubject(), claims, accessTokenExpiry);

        return ChefResponse
                .builder()
                .refreshToken(newRefreshToken)
                .accessToken(newAccessToken)
                .build();
    }

    public ChefResponse handleUploadAvatar(UUID chefId, MultipartFile avatar){
        ChefEntity chef = chefRepository.findById(chefId)
                .orElseThrow(()-> new ResourceNotFoundException("Chef not found"));

        storageService.uploadFile("chefs-avatar", avatar.getOriginalFilename(), avatar, true);

        String avatarUrl = storageService.getPublicUrl("chefs-avatar", avatar.getOriginalFilename());
        chef.setAvatarUrl(avatarUrl);
        chefRepository.save(chef);

        return chefMapper.toChefResponse(chef);
    }

    @Transactional
    public ChefResponse handleUpdateLocation(UUID chefId, UpdateLocationRequest locationRequest){
        ChefEntity existingChef = chefRepository.findByChefId(chefId)
                .orElseThrow(()->new ResourceNotFoundException("Chef not found"));

        log.error(existingChef.toString());

        chefRepository.updateLocation(
                chefId,
                locationRequest.getAddress(),
                locationRequest.getLongitude(),
                locationRequest.getLatitude()
        );

        ChefEntity updatedChef = chefRepository.findByMobileNo(existingChef.getMobileNo())
                .orElseThrow(()->new ResourceNotFoundException("Chef not found"));

        return chefMapper.toChefResponse(updatedChef);
    }

    public Map<String, Double> handleGetLocation(UUID chefId) {
        ChefEntity existingChef = chefRepository.findById(chefId)
                .orElseThrow(()->new ResourceNotFoundException("Chef not found"));

        if (existingChef.getLocation() == null) {
            throw new ResourceNotFoundException("Location not found for chef");
        }

        return Map.of(
                "longitude", existingChef.getLocation().getX(),
                "latitude", existingChef.getLocation().getY()
        );
    }
}
