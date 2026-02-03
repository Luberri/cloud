package com.demo.cloud.service;

import com.demo.cloud.config.SecurityProperties;
import com.demo.cloud.entity.Role;
import com.demo.cloud.entity.User;
import com.demo.cloud.entity.UserSession;
import com.demo.cloud.repository.RoleRepository;
import com.demo.cloud.repository.UserRepository;
import com.demo.cloud.repository.UserSessionRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository sessionRepository;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final FirebaseAuthClient firebaseAuthClient;

    public AuthService(UserRepository repo,
                       RoleRepository roleRepo,
                       UserSessionRepository sessionRepo,
                       JwtService jwt,
                       SecurityProperties secProps,
                       FirebaseAuthClient firebaseAuthClient) {
        this.userRepository = repo;
        this.roleRepository = roleRepo;
        this.sessionRepository = sessionRepo;
        this.jwtService = jwt;
        this.securityProperties = secProps;
        this.firebaseAuthClient = firebaseAuthClient;
    }

    // =====================
    // CAS 1 : LOGIN POSTGRES (OFFLINE)
    // =====================
    public String loginLocal(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (user.getLockedUntil() != null) {
            throw new RuntimeException("Compte bloqué. Contactez un administrateur pour débloquer votre compte.");
        }

        if (user.getPasswordHash() == null || !user.getPasswordHash().equals(password)) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            if (user.getFailedLoginAttempts() >= securityProperties.getMaxLoginAttempts()) {
                user.setLockedUntil(LocalDateTime.of(9999, 12, 31, 23, 59));
            }

            userRepository.save(user);
            throw new RuntimeException("Mot de passe incorrect");
        }

        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        // Générer JWT et sauvegarder la session
        String token = jwtService.generateToken(email);
        saveSession(user, token);

        return token;
    }

    // =====================
    // CAS 2 : LOGIN FIREBASE (ONLINE)
    // =====================
    public String loginFirebase(String firebaseIdToken) {
        try {
            // 1. Vérifier le token Firebase
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken);
            String email = decodedToken.getEmail();
            String uid = decodedToken.getUid();

            if (email == null) {
                throw new RuntimeException("Email Firebase introuvable dans le token");
            }

            // 2. Récupérer ou créer l'utilisateur local
            User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUserFromFirebase(email, uid));

            // 3. Vérifier le blocage
            if (user.getLockedUntil() != null) {
                throw new RuntimeException("Compte bloqué. Contactez un administrateur pour débloquer votre compte.");
            }

            // 4. Générer JWT applicatif et sauvegarder la session
            String token = jwtService.generateToken(email);
            saveSession(user, token);

            return token;

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Token Firebase invalide : " + e.getMessage());
        }
    }

    private User createUserFromFirebase(String email, String uid) {
        Role userRole = roleRepository.findByCode("USER")
            .orElseThrow(() -> new RuntimeException("Rôle USER introuvable"));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(""); // Pas de mot de passe pour les users Firebase
        user.setFullName(email); // On peut enrichir avec displayName Firebase si dispo
        user.setFirebaseUid(uid); // Sauvegarder le Firebase UID
        user.setRole(userRole);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    private void saveSession(User user, String token) {
        UserSession session = new UserSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setToken(token);
        session.setExpiresAt(LocalDateTime.now().plusHours(1)); // 1h
        session.setCreatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    // =====================
    // AUTRES MÉTHODES (register, update, unlock)
    // =====================
    public List<User> getBlockedUsers() {
        return userRepository.findByLockedUntilIsNotNull();
    }

    public User registerUserPostgres(String email, String password, String fullName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email déjà utilisé");
        }

        Role userRole = roleRepository.findByCode("USER")
            .orElseThrow(() -> new RuntimeException("Rôle USER introuvable"));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(password);
        user.setFullName(fullName);
        user.setRole(userRole);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    public String registerUserFirebase(String email, String password, String fullName) {
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(fullName);

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);

            FirebaseAuth.getInstance().setCustomUserClaims(
                userRecord.getUid(),
                Map.of("role", "USER")
            );

            return userRecord.getUid();

        } catch (FirebaseAuthException e) {
            if (e.getErrorCode().equals("EMAIL_ALREADY_EXISTS")) {
                throw new RuntimeException("Email déjà utilisé");
            }
            throw new RuntimeException("Erreur Firebase : " + e.getMessage());
        }
    }

    public void updateUser(String email, String newEmail, String newFullName, String newPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (newEmail != null && !newEmail.isBlank()) {
            if (userRepository.findByEmail(newEmail).isPresent()) {
                throw new RuntimeException("Nouvel email déjà utilisé");
            }
            user.setEmail(newEmail);
        }
        if (newFullName != null && !newFullName.isBlank()) {
            user.setFullName(newFullName);
        }
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(newPassword);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void updateUserFirebase(String email, String newEmail, String newFullName, String newPassword) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);

            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(userRecord.getUid());
            boolean toUpdate = false;

            if (newEmail != null && !newEmail.isBlank()) {
                request.setEmail(newEmail);
                toUpdate = true;
            }
            if (newFullName != null && !newFullName.isBlank()) {
                request.setDisplayName(newFullName);
                toUpdate = true;
            }
            if (newPassword != null && !newPassword.isBlank()) {
                request.setPassword(newPassword);
                toUpdate = true;
            }

            if (toUpdate) {
                FirebaseAuth.getInstance().updateUser(request);
            }
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Erreur Firebase : " + e.getMessage());
        }
    }

    public void unlockAccount(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    public void unlockAccountById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    public void unlockAccountFirebase(String email) {
        try {
            UserRecord u = FirebaseAuth.getInstance().getUserByEmail(email);

            FirebaseAuth.getInstance().updateUser(
                new UserRecord.UpdateRequest(u.getUid()).setDisabled(false)
            );

            Map<String, Object> claims = new HashMap<>(u.getCustomClaims());
            claims.put("failedLoginAttempts", 0);
            claims.put("locked", false);
            FirebaseAuth.getInstance().setCustomUserClaims(u.getUid(), claims);

        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Erreur Firebase : " + e.getMessage());
        }
    }
}