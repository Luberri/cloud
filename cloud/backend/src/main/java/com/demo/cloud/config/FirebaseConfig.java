package com.demo.cloud.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccountStream = getClass().getResourceAsStream("/service-account.json");
            if (serviceAccountStream == null) {
                throw new IllegalStateException("Missing Firebase service account file: src/main/resources/service-account.json");
            }
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(
                    serviceAccountStream))
                .build();
            FirebaseApp.initializeApp(options);
        }
    }
}