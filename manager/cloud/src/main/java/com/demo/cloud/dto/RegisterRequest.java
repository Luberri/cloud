package com.demo.cloud.dto;

public record RegisterRequest(
    String email,
    String password,
    String fullName,
    boolean useFirebase  // true = Firebase, false = PostgreSQL
) {}