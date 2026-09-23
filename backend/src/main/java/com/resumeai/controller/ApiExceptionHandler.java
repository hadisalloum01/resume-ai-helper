package com.resumeai.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,Object> bad(IllegalArgumentException e){return body(e.getMessage());}
    @ExceptionHandler(MaxUploadSizeExceededException.class) @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    Map<String,Object> large(){return body("The uploaded file exceeds the 10 MB limit.");}
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Map<String,Object> other(Exception e){return body("An unexpected error occurred. Please check the backend logs.");}
    private Map<String,Object> body(String message){return Map.of("timestamp",Instant.now(),"message",message);}
}
