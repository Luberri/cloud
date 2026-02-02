package com.demo.cloud.controller;

import com.demo.cloud.service.AuthService;
import com.demo.cloud.dto.LoginRequest;
import com.demo.cloud.dto.FirebaseLoginRequest;
import com.demo.cloud.dto.RegisterRequest;
import com.demo.cloud.dto.UpdateUserRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.InetSocketAddress;
import java.net.Socket;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Gestion de l'authentification")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // =====================
    // CAS 1 : LOGIN POSTGRES (OFFLINE)
    // =====================
    @PostMapping("/login")
    @Operation(summary = "Login Postgres", description = "Authentification email/password via PostgreSQL. Retourne un JWT applicatif.")
    public ResponseEntity<String> loginLocal(@RequestBody LoginRequest req) {
        String token = authService.loginLocal(req.email(), req.password());
        return ResponseEntity.ok(token);
    }

    // =====================
    // CAS 2 : LOGIN FIREBASE (ONLINE)
    // =====================
    @PostMapping("/login/firebase")
    @Operation(summary = "Login Firebase", description = "Authentification via Firebase ID Token. Retourne un JWT applicatif.")
    public ResponseEntity<String> loginFirebase(@RequestBody FirebaseLoginRequest req) {
        String token = authService.loginFirebase(req.firebaseIdToken());
        return ResponseEntity.ok(token);
    }

    // =====================
    // REGISTER (AUTO : FIREBASE SI INTERNET, SINON POSTGRES)
    // =====================
    @PostMapping("/register")
    @Operation(summary = "Inscription", description = "Firebase si Internet, sinon PostgreSQL")
    public ResponseEntity<String> register(@RequestBody RegisterRequest req) {
        boolean hasInternet = isInternetAvailable();
        if (hasInternet) {
            String firebaseUid = authService.registerUserFirebase(
                req.email(),
                req.password(),
                req.fullName()
            );
            return ResponseEntity.ok("Utilisateur créé dans Firebase avec UID: " + firebaseUid);
        } else {
            authService.registerUserPostgres(
                req.email(),
                req.password(),
                req.fullName()
            );
            return ResponseEntity.ok("Utilisateur créé dans PostgreSQL");
        }
    }

    // =====================
    // UPDATE
    // =====================
    @PutMapping("/update")
    @Operation(summary = "Modifier infos user", description = "Firebase si Internet, sinon PostgreSQL")
    public ResponseEntity<String> update(@RequestBody UpdateUserRequest req) {
        boolean hasInternet = isInternetAvailable();
        if (hasInternet) {
            authService.updateUserFirebase(req.email(), req.newEmail(), req.newFullName(), req.newPassword());
            return ResponseEntity.ok("Utilisateur mis à jour dans Firebase");
        } else {
            authService.updateUser(req.email(), req.newEmail(), req.newFullName(), req.newPassword());
            return ResponseEntity.ok("Utilisateur mis à jour dans PostgreSQL");
        }
    }

    // =====================
    // UNLOCK
    // =====================
    @PostMapping("/unlock/{email}")
    @Operation(summary = "Débloquer un compte", description = "Débloque Firebase si Internet, sinon PostgreSQL")
    public ResponseEntity<String> unlockAccount(@PathVariable String email) {
        boolean hasInternet = isInternetAvailable();
        if (hasInternet) {
            authService.unlockAccountFirebase(email);
            return ResponseEntity.ok("Compte débloqué (Firebase)");
        } else {
            authService.unlockAccount(email);
            return ResponseEntity.ok("Compte débloqué (PostgreSQL)");
        }
    }

    // =====================
    // UTILITAIRE
    // =====================
    private boolean isInternetAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}