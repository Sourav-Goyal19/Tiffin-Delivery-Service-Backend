package com.example.tds.controller;

import com.example.tds.dto.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {
    @GetMapping
    public ResponseEntity<ApiResponse> homeRequest(){
//        System.out.println("Got a request");
        return ResponseEntity.ok().body(ApiResponse.builder()
                .message("Api is working fine.")
                .success(true)
                .build());
    }
}
