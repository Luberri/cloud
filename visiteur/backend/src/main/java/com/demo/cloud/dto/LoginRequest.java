package com.demo.cloud.dto;

public record LoginRequest(
    String email,
    String password
) {}