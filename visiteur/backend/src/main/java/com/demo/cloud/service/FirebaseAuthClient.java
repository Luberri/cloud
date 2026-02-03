package com.demo.cloud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class FirebaseAuthClient {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public FirebaseAuthClient(ObjectMapper objectMapper, @Value("${firebase.api-key}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public void signInWithPassword(String email, String password) {
        try {
            String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey;

            String body = objectMapper.createObjectNode()
                .put("email", email)
                .put("password", password)
                .put("returnSecureToken", true)
                .toString();

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                return; // OK
            }

            JsonNode root = objectMapper.readTree(res.body());
            String msg = root.path("error").path("message").asText("AUTH_ERROR");
            throw new RuntimeException("Firebase login error: " + msg);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Firebase login error: " + e.getMessage(), e);
        }
    }
}