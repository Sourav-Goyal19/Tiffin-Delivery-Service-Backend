package com.example.tds.exception;

import com.example.tds.dto.responses.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex){
        return ErrorResponse.builder()
                .message(ex.getMessage())
                .success(false)
                .errors(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handlerNoResourceFound(ResourceNotFoundException ex){
        return ErrorResponse.builder()
                .message(ex.getMessage())
                .success(false)
                .errors(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException ex){
        return ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errors(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex){
        Map<Object, Object> validationErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(err -> validationErrors.put(err.getField(), err.getDefaultMessage()));

        return ErrorResponse.builder()
                .success(false)
                .message("Validation failed")
                .errors(validationErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthorization(UnauthorizedException ex){
        return ErrorResponse.builder()
                .errors(null)
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex){
        return ErrorResponse.builder()
                .errors(null)
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(CustomException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCustomException(CustomException ex){
        return ErrorResponse.builder()
                .errors(ex.getErrors())
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

//    @ExceptionHandler(Exception.class)
//    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
//    public ErrorResponse handleGeneric(Exception ex){
//        return ErrorResponse.builder()
//                .success(false)
//                .message("Internal Server Error")
//                .errors(null)
//                .timestamp(LocalDateTime.now())
//                .build();
//    }
}
