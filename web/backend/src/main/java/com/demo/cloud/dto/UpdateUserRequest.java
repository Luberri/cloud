package com.demo.cloud.dto;

public record UpdateUserRequest(
    String email,       
    String newEmail,     
    String newFullName,  
    String newPassword  
) {}