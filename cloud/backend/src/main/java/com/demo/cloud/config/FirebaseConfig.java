package com.demo.cloud.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(
                    getClass().getResourceAsStream("/firebase-service-account.json")))
                .build();
            FirebaseApp.initializeApp(options);
        }
    }
}