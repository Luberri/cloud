package com.demo.cloud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private int maxLoginAttempts = 3;
    private int lockoutDurationMinutes = 15;
    private long jwtSessionDurationMs = 3600000;

    // Getters et Setters
    public int getMaxLoginAttempts() { return maxLoginAttempts; }
    public void setMaxLoginAttempts(int maxLoginAttempts) { 
        this.maxLoginAttempts = maxLoginAttempts; 
    }

    public int getLockoutDurationMinutes() { return lockoutDurationMinutes; }
    public void setLockoutDurationMinutes(int lockoutDurationMinutes) { 
        this.lockoutDurationMinutes = lockoutDurationMinutes; 
    }

    public long getJwtSessionDurationMs() { return jwtSessionDurationMs; }
    public void setJwtSessionDurationMs(long jwtSessionDurationMs) { 
        this.jwtSessionDurationMs = jwtSessionDurationMs; 
    }
}
