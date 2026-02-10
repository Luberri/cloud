package com.demo.cloud.service;

import com.demo.cloud.entity.IssueImage;
import com.demo.cloud.entity.RoadIssue;
import com.demo.cloud.repository.IssueImageRepository;
import com.demo.cloud.repository.RoadIssueRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class ImageSyncService {

    private final IssueImageRepository issueImageRepository;
    private final RoadIssueRepository roadIssueRepository;
    
    private static final String PHOTOS_BASE_PATH = "../photos";

    public ImageSyncService(IssueImageRepository issueImageRepository, 
                           RoadIssueRepository roadIssueRepository) {
        this.issueImageRepository = issueImageRepository;
        this.roadIssueRepository = roadIssueRepository;
        
        try {
            Files.createDirectories(Paths.get(PHOTOS_BASE_PATH));
        } catch (IOException e) {
            System.err.println("⚠️ Impossible de créer le dossier photos: " + e.getMessage());
        }
    }

    /**
     * PULL: Synchronise les images depuis Firebase vers PostgreSQL
     * ET télécharge les images physiquement
     */
    @Transactional
    public int pullImagesFromFirebase() {
        int imagesPulled = 0;
        
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = firestore.collection("road_issues").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            System.out.println("\n=== DÉBUT SYNCHRONISATION IMAGES ===");
            System.out.println("📥 " + documents.size() + " documents Firebase trouvés\n");

            for (QueryDocumentSnapshot doc : documents) {
                String firebaseId = doc.getId();
                String issueIdString = doc.getString("id");
                
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("🔍 Document Firebase: " + firebaseId);
                System.out.println("   Issue ID: " + issueIdString);
                
                // ✅ CORRECTION: Récupérer le champ photosBase64
                Object photosObj = doc.get("photosBase64");
                
                if (photosObj == null) {
                    System.out.println("   ⚠️ Aucun champ 'photosBase64'");
                    
                    // Vérifier aussi le champ 'photos' pour compatibilité
                    photosObj = doc.get("photos");
                    if (photosObj != null) {
                        System.out.println("   💡 Champ 'photos' trouvé (ancien format)");
                    } else {
                        System.out.println("   💡 Vérifiez que l'app mobile stocke bien en base64");
                        continue;
                    }
                }
                
                // ✅ Convertir l'objet en List<String>
                List<String> photosBase64 = null;
                
                if (photosObj instanceof List) {
                    photosBase64 = (List<String>) photosObj;
                } else if (photosObj instanceof ArrayList) {
                    photosBase64 = (ArrayList<String>) photosObj;
                } else if (photosObj.getClass().isArray()) {
                    // Si c'est un array natif, le convertir en List
                    Object[] array = (Object[]) photosObj;
                    photosBase64 = new ArrayList<>();
                    for (Object item : array) {
                        if (item instanceof String) {
                            photosBase64.add((String) item);
                        }
                    }
                } else {
                    System.out.println("   ⚠️ Type inattendu: " + photosObj.getClass().getName());
                    continue;
                }
                
                if (photosBase64 == null || photosBase64.isEmpty()) {
                    System.out.println("   ℹ️ Liste photosBase64 vide");
                    continue;
                }
                
                System.out.println("   📸 " + photosBase64.size() + " photo(s) trouvée(s)");
                
                // Afficher un aperçu de la première photo
                if (!photosBase64.isEmpty()) {
                    String firstPhoto = photosBase64.get(0);
                    int length = Math.min(firstPhoto.length(), 50);
                    System.out.println("   🔍 Aperçu base64[0]: " + firstPhoto.substring(0, length) + "...");
                    System.out.println("   📊 Taille base64[0]: " + firstPhoto.length() + " caractères");
                    
                    // Vérifier si c'est bien du base64
                    if (firstPhoto.matches("^[A-Za-z0-9+/]+={0,2}$")) {
                        System.out.println("   ✅ Format base64 valide");
                    } else {
                        System.out.println("   ⚠️ Le format ne semble pas être du base64 pur");
                    }
                }
                
                // Chercher le signalement dans PostgreSQL
                Optional<RoadIssue> optionalIssue = Optional.empty();
                
                // 1) Chercher par firebaseId
                if (firebaseId != null) {
                    optionalIssue = roadIssueRepository.findByFirebaseId(firebaseId);
                    if (optionalIssue.isPresent()) {
                        System.out.println("   ✅ Trouvé par firebaseId");
                    }
                }
                
                // 2) Chercher par UUID
                if (!optionalIssue.isPresent() && issueIdString != null) {
                    try {
                        UUID issueUuid = UUID.fromString(issueIdString);
                        optionalIssue = roadIssueRepository.findById(issueUuid);
                        if (optionalIssue.isPresent()) {
                            System.out.println("   ✅ Trouvé par UUID");
                            
                            // Mettre à jour le firebaseId
                            RoadIssue issue = optionalIssue.get();
                            if (issue.getFirebaseId() == null) {
                                issue.setFirebaseId(firebaseId);
                                roadIssueRepository.save(issue);
                                System.out.println("   📝 Firebase ID ajouté au signalement");
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("   ⚠️ UUID invalide: " + issueIdString);
                    }
                }
                
                if (!optionalIssue.isPresent()) {
                    System.out.println("   ❌ Signalement non trouvé dans PostgreSQL");
                    System.out.println("   💡 Exécutez d'abord: POST /sync/all");
                    continue;
                }
                
                RoadIssue localIssue = optionalIssue.get();
                System.out.println("   ✅ Signalement: " + localIssue.getTitle());
                
                // Créer le dossier pour les images
                Path issuePhotosDir = Paths.get(PHOTOS_BASE_PATH, localIssue.getId().toString());
                Files.createDirectories(issuePhotosDir);
                System.out.println("   📁 Dossier: " + issuePhotosDir.toAbsolutePath());
                
                // Traiter chaque photo
                for (int i = 0; i < photosBase64.size(); i++) {
                    String base64Data = photosBase64.get(i);
                    String storagePath = localIssue.getId() + "/image_" + i + ".jpg";
                    
                    // Vérifier si déjà synchronisée
                    if (issueImageRepository.existsByRoadIssueIdAndStoragePath(
                            localIssue.getId(), storagePath)) {
                        System.out.println("   ⏭️  Image " + i + " déjà synchronisée");
                        continue;
                    }
                    
                    String fileName = saveBase64Image(base64Data, issuePhotosDir, i);
                    
                    if (fileName != null) {
                        // Sauvegarder dans la base de données
                        IssueImage image = new IssueImage();
                        image.setRoadIssueId(localIssue.getId());
                        image.setStoragePath(storagePath);
                        image.setDownloadUrl("/photos/" + localIssue.getId() + "/" + fileName);
                        
                        String reportedBy = doc.getString("reportedBy");
                        if (reportedBy != null) {
                            try {
                                image.setUploadedBy(UUID.fromString(reportedBy));
                            } catch (IllegalArgumentException e) {
                                // Ignorer si UUID invalide
                            }
                        }
                        
                        issueImageRepository.save(image);
                        imagesPulled++;
                        System.out.println("   ✅ Image " + i + " synchronisée: " + fileName);
                    }
                }
            }
            
            System.out.println("\n=== FIN SYNCHRONISATION ===");
            System.out.println("✅ Total: " + imagesPulled + " image(s) synchronisée(s)\n");
            
        } catch (Exception e) {
            System.err.println("\n❌ ERREUR CRITIQUE:");
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur synchronisation images", e);
        }

        return imagesPulled;
    }

    private String saveBase64Image(String base64Data, Path destinationDir, int index) {
        try {
            System.out.println("      🔄 Décodage image " + index + "...");
            
            // Nettoyer la chaîne base64
            if (base64Data == null || base64Data.trim().isEmpty()) {
                System.err.println("      ❌ Base64 vide");
                return null;
            }
            
            // Supprimer les espaces et retours à la ligne
            base64Data = base64Data.replaceAll("\\s", "");
            
            // Vérifier la longueur minimale
            if (base64Data.length() < 100) {
                System.err.println("      ❌ Base64 trop court: " + base64Data.length() + " chars");
                return null;
            }
            
            System.out.println("      📊 Longueur base64: " + base64Data.length() + " chars");
            
            // Décoder
            byte[] imageBytes;
            try {
                imageBytes = Base64.getDecoder().decode(base64Data);
            } catch (IllegalArgumentException e) {
                System.err.println("      ❌ Erreur décodage: " + e.getMessage());
                System.err.println("      🔍 Premiers chars: " + base64Data.substring(0, Math.min(100, base64Data.length())));
                return null;
            }
            
            if (imageBytes.length == 0) {
                System.err.println("      ❌ Image vide après décodage");
                return null;
            }
            
            // Vérifier le magic number (FFD8 pour JPEG, 8950 pour PNG)
            if (imageBytes.length >= 2) {
                String hex = String.format("%02X%02X", imageBytes[0] & 0xFF, imageBytes[1] & 0xFF);
                System.out.println("      🔍 Magic number: " + hex);
                
                if (hex.equals("FFD8")) {
                    System.out.println("      ✅ Format JPEG détecté");
                } else if (hex.equals("8950")) {
                    System.out.println("      ✅ Format PNG détecté");
                } else {
                    System.err.println("      ⚠️ Format image inconnu (magic: " + hex + ")");
                }
            }
            
            String fileName = "image_" + index + ".jpg";
            Path filePath = destinationDir.resolve(fileName);
            
            // Sauvegarder
            Files.write(filePath, imageBytes);
            
            double sizeKB = imageBytes.length / 1024.0;
            System.out.println("      ✅ Sauvegardé: " + fileName + " (" + String.format("%.2f", sizeKB) + " KB)");
            
            return fileName;
            
        } catch (Exception e) {
            System.err.println("      ❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Télécharge une image depuis Firebase Storage vers le disque local
     * @param firebaseUrl URL Firebase de l'image
     * @param destinationDir Dossier de destination
     * @param index Index de l'image (pour nommer le fichier)
     * @return Le nom du fichier téléchargé, ou null en cas d'erreur
     */
    private String downloadImageFromFirebase(String firebaseUrl, Path destinationDir, int index) {
        try {
            // Déterminer l'extension du fichier
            String extension = ".jpg";
            if (firebaseUrl.contains(".png")) {
                extension = ".png";
            } else if (firebaseUrl.contains(".jpeg") || firebaseUrl.contains(".jpg")) {
                extension = ".jpg";
            }
            
            // Générer un nom de fichier unique
            String fileName = "image_" + index + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path filePath = destinationDir.resolve(fileName);
            
            // Télécharger l'image
            URL url = new URL(firebaseUrl);
            try (InputStream in = url.openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✓ Image téléchargée: " + filePath);
                return fileName;
            }
            
        } catch (IOException e) {
            System.err.println("✗ Erreur lors du téléchargement de " + firebaseUrl + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * PUSH: Synchronise les images depuis PostgreSQL vers Firebase
     */
    @Transactional
    public int pushImagesToFirebase() {
        int imagesPushed = 0;
        
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            List<RoadIssue> localIssues = roadIssueRepository.findAll();

            for (RoadIssue localIssue : localIssues) {
                if (localIssue.getFirebaseId() == null) {
                    continue;
                }

                List<IssueImage> localImages = issueImageRepository.findByRoadIssueId(localIssue.getId());
                
                if (localImages.isEmpty()) {
                    continue;
                }

                List<String> photosArray = new ArrayList<>();
                for (IssueImage img : localImages) {
                    // Convertir les URLs locales en URLs complètes
                    String url = img.getDownloadUrl();
                    if (url.startsWith("/photos/")) {
                        url = "http://localhost:8080" + url;
                    }
                    photosArray.add(url);
                }

                DocumentReference docRef = firestore.collection("road_issues")
                        .document(localIssue.getFirebaseId());
                
                ApiFuture<DocumentSnapshot> future = docRef.get();
                DocumentSnapshot document = future.get();
                
                if (document.exists()) {
                    List<String> existingPhotos = (List<String>) document.get("photos");
                    
                    Set<String> allPhotos = new HashSet<>();
                    if (existingPhotos != null) {
                        allPhotos.addAll(existingPhotos);
                    }
                    allPhotos.addAll(photosArray);
                    
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("photos", new ArrayList<>(allPhotos));
                    
                    docRef.update(updates).get();
                    imagesPushed += (allPhotos.size() - (existingPhotos != null ? existingPhotos.size() : 0));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Erreur Firebase Auth: " + e.getMessage());
            throw new RuntimeException("Erreur lors du PUSH des images vers Firebase: " + e.getMessage(), e);
        }

        return imagesPushed;
    }

    /**
     * Synchronisation bidirectionnelle
     */
    @Transactional
    public Map<String, Integer> syncImages() {
        Map<String, Integer> result = new HashMap<>();
        
        try {
            int pulled = pullImagesFromFirebase();
            result.put("imagesPulled", pulled);
            
            int pushed = pushImagesToFirebase();
            result.put("imagesPushed", pushed);
        } catch (Exception e) {
            result.put("imagesPulled", 0);
            result.put("imagesPushed", 0);
            result.put("error", 1);
            System.err.println("❌ Synchronisation des images échouée: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Extrait le chemin de stockage depuis l'URL
     */
    private String extractStoragePath(String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        
        // Si c'est un blob URL local
        if (photoUrl.startsWith("blob:")) {
            return photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
        }
        
        // Si c'est une URL Firebase Storage
        if (photoUrl.contains("firebasestorage.googleapis.com")) {
            try {
                int startIdx = photoUrl.indexOf("/o/") + 3;
                int endIdx = photoUrl.indexOf("?");
                if (startIdx > 2 && endIdx > startIdx) {
                    String encoded = photoUrl.substring(startIdx, endIdx);
                    return java.net.URLDecoder.decode(encoded, "UTF-8");
                }
            } catch (Exception e) {
                // Fallback
            }
        }
        
        // Fallback: utiliser l'URL comme identifiant
        return photoUrl.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * PULL: Synchronise les signalements depuis Firebase vers PostgreSQL
     */
    @Transactional
    public int pullRoadIssuesFromFirebase() {
        int issuesPulled = 0;
        
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = firestore.collection("road_issues").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot doc : documents) {
                String firebaseId = doc.getId();
                String issueId = doc.getString("id");
                
                // Vérifier si l'issue existe déjà
                Optional<RoadIssue> existing = issueId != null 
                    ? roadIssueRepository.findById(UUID.fromString(issueId))
                    : Optional.empty();
                
                if (existing.isPresent()) {
                    // Mettre à jour le firebaseId si manquant
                    RoadIssue issue = existing.get();
                    if (issue.getFirebaseId() == null) {
                        issue.setFirebaseId(firebaseId);
                        roadIssueRepository.save(issue);
                        System.out.println("✅ Firebase ID mis à jour pour: " + issue.getId());
                    }
                    continue;
                }
                
                // Créer une nouvelle issue
                RoadIssue newIssue = new RoadIssue();
                newIssue.setId(issueId != null ? UUID.fromString(issueId) : UUID.randomUUID());
                newIssue.setFirebaseId(firebaseId); // ✅ IMPORTANT
                newIssue.setTitle(doc.getString("title"));
                newIssue.setDescription(doc.getString("description"));
                
                // ... reste du code ...
                
                roadIssueRepository.save(newIssue);
                issuesPulled++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur PULL road_issues", e);
        }
        
        return issuesPulled;
    }
}