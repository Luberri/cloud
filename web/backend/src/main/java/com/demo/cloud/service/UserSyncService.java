package com.demo.cloud.service;

import com.demo.cloud.entity.SyncLog;
import com.demo.cloud.entity.User;
import com.demo.cloud.repository.SyncLogRepository;
import com.demo.cloud.repository.UserRepository;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ListUsersPage;
import com.google.firebase.auth.UserRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserSyncService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SyncLogRepository syncLogRepository;

    /**
     * PULL: Firebase Auth -> DB locale (pour que la DB ressemble à Firebase)
     */
    @Transactional
    public int syncUsersFromFirebase() {
        int count = 0;

        try {
            ListUsersPage page = FirebaseAuth.getInstance().listUsers(null);
            while (page != null) {
                for (ExportedUserRecord firebaseUser : page.getValues()) {
                    try {
                        if (firebaseUser.getEmail() == null || firebaseUser.getEmail().isBlank()) {
                            log("PULL", "user", "SKIP", "Firebase uid=" + firebaseUser.getUid() + " email manquant");
                            continue;
                        }

                        Optional<User> existing = userRepository.findByEmail(firebaseUser.getEmail());
                        User user = existing.orElseGet(() -> {
                            User u = new User();
                            u.setId(UUID.randomUUID()); // car l'entité User n'est pas @GeneratedValue
                            u.setEmail(firebaseUser.getEmail());
                            // password local: si tu veux "DB ressemble à Firebase", tu n'as pas le mdp en clair depuis Firebase
                            // donc on met une valeur placeholder
                            u.setPasswordHash("FIREBASE_ONLY");
                            return u;
                        });

                        user.setFirebaseUid(firebaseUser.getUid());

                        if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isBlank()) {
                            user.setFullName(firebaseUser.getDisplayName());
                        }

                        // Firebase disabled -> on "bloque" localement
                        user.setIsBlocked(firebaseUser.isDisabled());

                        userRepository.save(user);

                        log("PULL", "user", "SUCCESS",
                                "email=" + user.getEmail() + " <- firebaseUid=" + firebaseUser.getUid());
                        count++;
                    } catch (Exception e) {
                        log("PULL", "user", "ERROR",
                                "firebaseUid=" + firebaseUser.getUid() + " err=" + e.getMessage());
                    }
                }
                page = page.getNextPage();
            }
        } catch (Exception e) {
            log("PULL", "user", "ERROR", "Erreur globale: " + e.getMessage());
        }

        return count;
    }

    /**
     * PUSH: DB locale -> Firebase Auth (ce que tu demandes)
     * Prend password_hash comme mot de passe en clair.
     */
    @Transactional
    public int pushLocalUsersToFirebaseAuth() {
        int count = 0;

        for (User user : userRepository.findByFirebaseUidIsNull()) {
            try {
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    log("PUSH", "user", "SKIP", "User id=" + user.getId() + " email manquant");
                    continue;
                }

                String plainPassword = user.getPasswordHash(); // tu veux le garder "non hashé"
                if (plainPassword == null || plainPassword.length() < 6) {
                    log("PUSH", "user", "FAIL",
                            "email=" + user.getEmail() + " mot de passe < 6 (Firebase exige min 6)");
                    continue;
                }

                UserRecord.CreateRequest req = new UserRecord.CreateRequest()
                        .setEmail(user.getEmail())
                        .setPassword(plainPassword)
                        .setDisabled(Boolean.TRUE.equals(user.getIsBlocked()));

                if (user.getFullName() != null && !user.getFullName().isBlank()) {
                    req.setDisplayName(user.getFullName());
                }

                UserRecord created = FirebaseAuth.getInstance().createUser(req);

                user.setFirebaseUid(created.getUid());
                userRepository.save(user);

                log("PUSH", "user", "SUCCESS",
                        "email=" + user.getEmail() + " -> firebaseUid=" + created.getUid());
                count++;
            } catch (Exception e) {
                log("PUSH", "user", "ERROR", "email=" + user.getEmail() + " err=" + e.getMessage());
            }
        }

        return count;
    }

    private void log(String syncType, String entity, String status, String message) {
        SyncLog log = new SyncLog();
        log.setSyncType(syncType);
        log.setEntity(entity);
        log.setStatus(status);
        log.setMessage(message);
        syncLogRepository.save(log);
    }
}
