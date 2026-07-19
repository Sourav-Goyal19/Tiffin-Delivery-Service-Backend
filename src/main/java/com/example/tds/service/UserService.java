package com.example.tds.service;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.entity.UserEntity;
import com.example.tds.mapper.UserMapper;
import com.example.tds.utilities.JwtUtility;
import org.springframework.stereotype.Service;
import com.example.tds.repository.UserRepository;
import com.example.tds.dto.responses.UserResponse;
import com.example.tds.exception.BadRequestException;
import com.example.tds.exception.UnauthorizedException;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.dto.requests.common.OtpVerifyRequest;
import com.example.tds.dto.requests.common.UpdateNameRequest;
import com.example.tds.dto.requests.common.OtpGenerationRequest;
import org.springframework.transaction.annotation.Transactional;
import com.example.tds.dto.requests.common.UpdateLocationRequest;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final JwtUtility jwt;

    long refreshTokenExpiry = 7 * 24 * 60 * 60 * 1000L;
    long accessTokenExpiry = 3 * 60 * 60 * 1000L;

    public UserResponse handleUpdateName(UUID userId, UpdateNameRequest userNameUpdateRequest) {
        String name = userNameUpdateRequest.getName();

        UserEntity existingUser = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        existingUser.setName(name);

        userRepository.save(existingUser);

        return userMapper.toUserResponse(existingUser);
    }

    @Transactional
    public void handleOtpGeneration(OtpGenerationRequest generationRequest){
        String mobileNo = generationRequest.getMobileNo();

        UserEntity existingUser = userRepository.findByMobileNo(mobileNo)
                .orElse(null);

        if(existingUser == null){
            UserEntity newUser = new UserEntity();
            newUser.setMobileNo(mobileNo);

            existingUser = userRepository.save(newUser);
        }

        int otp = ThreadLocalRandom.current().nextInt(1000, 10000);
        existingUser.setOtp(otp);

        userRepository.save(existingUser);

        // TODO: Send OTP through email or sms
    }

    @Transactional
    public UserResponse handleOtpVerification(OtpVerifyRequest otpVerifyRequest){
        String mobileNo = otpVerifyRequest.getMobileNo();
        Integer otp = otpVerifyRequest.getOtp();

        UserEntity user = userRepository.findByMobileNo(mobileNo)
                .orElseThrow(()->new ResourceNotFoundException("User not found"));

        if(user.getOtp() == null || !user.getOtp().equals(otp)){
            throw new BadRequestException("Invalid OTP");
        }

        user.setOtp(null);
        user.setIsVerified(true);

        userRepository.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("name", user.getName());
        claims.put("mobileNo", user.getMobileNo());

        String refreshToken = jwt.generateToken(user.getId(), claims, refreshTokenExpiry);
        String accessToken = jwt.generateToken(user.getId(), claims, accessTokenExpiry);

       UserResponse response = userMapper.toUserResponse(user);
       response.setAccessToken(accessToken);
       response.setRefreshToken(refreshToken);

       return response;
    }

    public UserResponse handleGetUser(UserEntity currentUser){
        return userMapper.toUserResponse(currentUser);
    }

    public UserResponse handleRefresh(String refreshToken){
        if(refreshToken == null || refreshToken.isEmpty()){
            throw new UnauthorizedException("Refresh token not found. Please login again.");
        }

        if(!jwt.validateToken(refreshToken)){
            throw new UnauthorizedException("Refresh token invalid or expired. Please login again.");
        }

        Claims claims = jwt.extractAllClaims(refreshToken);

        String newRefreshToken = jwt.generateToken(claims.getSubject(), claims, refreshTokenExpiry);
        String newAccessToken = jwt.generateToken(claims.getSubject(), claims, accessTokenExpiry);

        UserResponse response = new UserResponse();

        response.setRefreshToken(newRefreshToken);
        response.setAccessToken(newAccessToken);

        return response;
    }

    @Transactional
    public UserResponse handleUpdateLocation(UUID userId, UpdateLocationRequest locationRequest){
        UserEntity existingUser = userRepository.findById(userId)
                .orElseThrow(()->new ResourceNotFoundException("User not found"));

        userRepository.updateLocation(
                userId,
                locationRequest.getAddress(),
                locationRequest.getLongitude(),
                locationRequest.getLatitude()
        );

        UserEntity updatedUser = userRepository.findByMobileNo(existingUser.getMobileNo())
                .orElseThrow(()->new ResourceNotFoundException("User not found"));

        return userMapper.toUserResponse(updatedUser);
    }

    public Map<String, Double> handleGetLocation(UUID userId){
        UserEntity existingUser = userRepository.findById(userId)
                .orElseThrow(()->new ResourceNotFoundException("User not found"));

        if (existingUser.getLocation() == null) {
            throw new ResourceNotFoundException("Location not found for user");
        }

        return Map.of(
                "longitude", existingUser.getLocation().getX(),
                "latitude", existingUser.getLocation().getY()
        );
    }
}
