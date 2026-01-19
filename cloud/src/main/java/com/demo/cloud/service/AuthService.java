package com.demo.cloud.service;

import com.demo.cloud.entity.User;
import com.demo.cloud.entity.Role;
import com.demo.cloud.repository.UserRepository;
import com.demo.cloud.repository.RoleRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository repo, RoleRepository roleRepo, PasswordEncoder encoder) {
        this.userRepository = repo;
        this.roleRepository = roleRepo;
        this.passwordEncoder = encoder;
    }

    public void loginLocal(String email, String password) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (user.getLockedUntil() != null &&
            user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Compte bloqué");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            if (user.getFailedLoginAttempts() >= 3) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            }

            userRepository.save(user);
            throw new RuntimeException("Mot de passe incorrect");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    // =====================
    // INSCRIPTION POSTGRESQL (sans hash)
    // =====================
    public User registerUserPostgres(String email, String password, String fullName) {
        // 1. Vérifier unicité email
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // 2. Récupérer le rôle USER
        Role userRole = roleRepository.findByCode("USER")
            .orElseThrow(() -> new RuntimeException("Rôle USER introuvable"));

        // 3. Créer l'utilisateur (mot de passe en clair - NON RECOMMANDÉ)
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(password);  // SANS HASH
        user.setFullName(fullName);
        user.setRole(userRole);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 4. Sauvegarder
        return userRepository.save(user);
    }

    // =====================
    // INSCRIPTION FIREBASE (sans hash)
    // =====================
    public String registerUserFirebase(String email, String password, String fullName) {
        try {
            // 1. Vérifier unicité (Firebase génère une erreur si l'email existe)
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)  // Firebase gère le hash automatiquement
                .setDisplayName(fullName);

            // 2. Créer l'utilisateur dans Firebase
            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);

            // 3. Optionnel : assigner un custom claim pour le rôle
            FirebaseAuth.getInstance().setCustomUserClaims(
                userRecord.getUid(),
                java.util.Map.of("role", "USER")
            );

            return userRecord.getUid();  // Retourne l'UID Firebase

        } catch (FirebaseAuthException e) {
            if (e.getErrorCode().equals("EMAIL_ALREADY_EXISTS")) {
                throw new RuntimeException("Email déjà utilisé");
            }
            throw new RuntimeException("Erreur Firebase : " + e.getMessage());
        }
    }
}