package com.example.tds.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class CustomException extends RuntimeException{
    private final Map<String, Object> errors;

    public CustomException(String message, Map<String, Object> errors){
        super(message);
        this.errors = errors;
    }

}

