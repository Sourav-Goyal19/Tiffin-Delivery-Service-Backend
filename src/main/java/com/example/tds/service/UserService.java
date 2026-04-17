package com.example.tds.service;

import com.example.tds.dto.requests.UpdateLocationRequest;
import com.example.tds.dto.requests.UserLoginRequest;
import com.example.tds.dto.requests.UserSignUpRequest;
import com.example.tds.dto.responses.UserResponse;
import com.example.tds.entity.UserEntity;
import com.example.tds.exception.BadRequestException;
import com.example.tds.exception.ResourceNotFoundException;
import com.example.tds.exception.UnauthorizedException;
import com.example.tds.mapper.UserMapper;
import com.example.tds.repository.UserRepository;
import com.example.tds.utilities.JwtUtility;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtility jwt;

    long refreshTokenExpiry = 7 * 24 * 60 * 60 * 1000L;
    long accessTokenExpiry = 3 * 60 * 60 * 1000L;

    public UserResponse handleSignUp(UserSignUpRequest signUpDto){
        UserEntity user = userMapper.toUserEntity(signUpDto);

        String email = user.getEmail();
        UserEntity existingUser = userRepository.findByEmail(email);

        if(existingUser != null){
            throw new BadRequestException("This email already exists.");
        }

        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);

        UserEntity response = userRepository.save(user);

        return userMapper.toUserResponse(response);
    }

    public UserResponse handleLogin(UserLoginRequest loginDto){
        UserEntity user = userMapper.toUserEntity(loginDto);

        UserEntity existingUser = userRepository.findByEmail(user.getEmail());

        if (existingUser == null){
            throw new BadRequestException("No existing user found");
        }

        boolean matched = passwordEncoder.matches(user.getPassword(), existingUser.getPassword());

        if(!matched){
            throw new BadRequestException("Incorrect password");
        }

        Map<String, Object> claims = Map.of(
                "id", existingUser.getId(),
                "name", existingUser.getName(),
                "email", existingUser.getEmail()
        );


        String refreshToken = jwt.generateToken(existingUser.getId(), claims, refreshTokenExpiry);
        String accessToken = jwt.generateToken(existingUser.getId(), claims, accessTokenExpiry);

       UserResponse response = userMapper.toUserResponse(existingUser);
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

    public UserResponse handleUpdateLocation(UUID userId, UpdateLocationRequest locationRequest){
        UserEntity existingUser = userRepository.findById(userId)
                .orElseThrow(()->new ResourceNotFoundException("User not found"));

        userRepository.updateLocation(
                userId,
                locationRequest.getAddress(),
                locationRequest.getLongitude(),
                locationRequest.getLatitude()
        );

        UserEntity updatedUser = userRepository.findByEmail(existingUser.getEmail());

        return userMapper.toUserResponse(updatedUser);
    }
}
