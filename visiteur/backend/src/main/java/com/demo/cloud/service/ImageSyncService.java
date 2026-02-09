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

            for (QueryDocumentSnapshot doc : documents) {
                String firebaseId = doc.getId();
                
                // Trouver le road_issue correspondant dans PostgreSQL
                Optional<RoadIssue> optionalIssue = roadIssueRepository.findByFirebaseId(firebaseId);
                if (!optionalIssue.isPresent()) {
                    continue;
                }
                
                RoadIssue localIssue = optionalIssue.get();

                // Récupérer le tableau 'photos' depuis Firestore
                List<String> photos = (List<String>) doc.get("photos");
                if (photos != null && !photos.isEmpty()) {
                    
                    // Créer un sous-dossier pour ce signalement
                    Path issuePhotosDir = Paths.get(PHOTOS_BASE_PATH, localIssue.getId().toString());
                    Files.createDirectories(issuePhotosDir);
                    
                    for (int i = 0; i < photos.size(); i++) {
                        String firebaseUrl = photos.get(i);
                        
                        // Ignorer les blob URLs (ce sont des URLs temporaires du mobile)
                        if (firebaseUrl.startsWith("blob:")) {
                            continue;
                        }
                        
                        String storagePath = extractStoragePath(firebaseUrl);
                        
                        // Vérifier si l'image existe déjà
                        if (!issueImageRepository.existsByRoadIssueIdAndStoragePath(
                                localIssue.getId(), storagePath)) {
                            
                            // Télécharger l'image depuis Firebase
                            String localFileName = downloadImageFromFirebase(
                                firebaseUrl, 
                                issuePhotosDir, 
                                i
                            );
                            
                            if (localFileName != null) {
                                IssueImage image = new IssueImage();
                                image.setRoadIssueId(localIssue.getId());
                                image.setStoragePath(localIssue.getId() + "/" + localFileName);
                                
                                // URL locale pour accéder à l'image
                                image.setDownloadUrl("/photos/" + localIssue.getId() + "/" + localFileName);
                                
                                // Récupérer uploadedBy si disponible
                                String reportedBy = doc.getString("reportedBy");
                                if (reportedBy != null) {
                                    try {
                                        image.setUploadedBy(UUID.fromString(reportedBy));
                                    } catch (IllegalArgumentException e) {
                                        // Ignorer si ce n'est pas un UUID valide
                                    }
                                }
                                
                                issueImageRepository.save(image);
                                imagesPulled++;
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Erreur Firebase Auth: " + e.getMessage());
            System.err.println("💡 Vérifiez votre fichier serviceAccountKey.json");
            throw new RuntimeException("Erreur lors du PULL des images depuis Firebase: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Erreur I/O lors du téléchargement des images", e);
        }

        return imagesPulled;
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
}