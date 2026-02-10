<template>
  <ion-page>
    <!-- Header moderne avec effet glass -->
    <ion-header class="modern-header">
      <ion-toolbar>
        <div class="header-content">
          <div class="header-title-section">
            <div class="app-logo">
              <ion-icon :icon="locationOutline"></ion-icon>
            </div>
            <div class="header-titles">
              <h1 class="main-title">Signaleo</h1>
              <p class="subtitle">{{ signals.length }} signalements actifs</p>
            </div>
          </div>
          <div class="header-actions">
            <NotificationBell />
            <button class="action-btn stats-btn" @click="showStatsModal = true">
              <ion-icon :icon="statsChartOutline"></ion-icon>
            </button>
          </div>
        </div>
      </ion-toolbar>
    </ion-header>

    <ion-content :fullscreen="true">
      <div id="map" ref="mapContainer"></div>
      
      <!-- Mini stats flottantes -->
      <div class="floating-stats">
        <div class="mini-stat">
          <span class="stat-number">{{ globalStats.totalIssues }}</span>
          <span class="stat-text">Total</span>
        </div>
        <div class="mini-stat resolved">
          <span class="stat-number">{{ globalStats.resolvedIssues }}</span>
          <span class="stat-text">Résolus</span>
        </div>
        <div class="mini-stat progress">
          <span class="stat-number">{{ globalStats.progressPercentage.toFixed(0) }}%</span>
          <span class="stat-text">Avancement</span>
        </div>
      </div>
      
      <!-- Panneau de filtres moderne -->
      <div class="filters-panel" :class="{ collapsed: legendCollapsed }">
        <div class="filters-header" @click="legendCollapsed = !legendCollapsed">
          <div class="filters-title">
            <ion-icon :icon="filterOutline"></ion-icon>
            <span>Filtres</span>
          </div>
          <div class="filters-badge" v-if="selectedFilterTypes.length < issueTypes.length">
            {{ selectedFilterTypes.length }}/{{ issueTypes.length }}
          </div>
          <ion-icon :icon="legendCollapsed ? chevronDownOutline : chevronUpOutline" class="toggle-icon"></ion-icon>
        </div>
        
        <div class="filters-body" v-show="!legendCollapsed">
          <!-- Toggle mes signalements -->
          <div class="my-issues-toggle" :class="{ active: showMyIssuesOnly }" @click="toggleMyIssuesOnly">
            <ion-icon :icon="personOutline"></ion-icon>
            <span>Mes signalements uniquement</span>
            <div class="toggle-switch">
              <div class="toggle-dot"></div>
            </div>
          </div>
          
          <div class="filters-divider"></div>
          
          <!-- Actions rapides -->
          <div class="quick-actions">
            <button class="quick-btn" @click="selectAllTypes">
              <ion-icon :icon="checkmarkDoneOutline"></ion-icon>
              Tout
            </button>
            <button class="quick-btn" @click="deselectAllTypes">
              <ion-icon :icon="closeOutline"></ion-icon>
              Aucun
            </button>
          </div>
          
          <!-- Types de signalement en grille -->
          <div class="filter-types-grid">
            <div 
              v-for="type in issueTypes" 
              :key="type.id"
              class="filter-type-chip"
              :class="{ active: selectedFilterTypes.includes(type.id) }"
              :style="{ '--chip-color': type.color }"
              @click="toggleFilterType(type.id)"
            >
              <div class="chip-icon">
                <span>{{ type.emoji }}</span>
              </div>
              <span class="chip-label">{{ type.label }}</span>
              <span class="chip-count">{{ getCountForType(type.id) }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Bouton flottant d'ajout -->
      <div class="fab-container">
        <button 
          class="fab-button" 
          :class="{ active: isSignalMode }"
          @click="toggleSignalMode"
        >
          <ion-icon :icon="isSignalMode ? closeOutline : addOutline"></ion-icon>
        </button>
        <span class="fab-label" v-if="!isSignalMode">Signaler</span>
      </div>
      
      <!-- Sélecteur de type moderne -->
      <transition name="slide-up">
        <div v-if="isSignalMode && !showModal" class="type-selector-panel">
          <div class="selector-header">
            <h2>Nouveau signalement</h2>
            <p>Quel type de problème souhaitez-vous signaler ?</p>
          </div>
          
          <!-- Bouton GPS -->
          <button 
            class="gps-button" 
            @click="useMyLocation" 
            :disabled="gettingLocation"
          >
            <ion-spinner v-if="gettingLocation" name="crescent"></ion-spinner>
            <ion-icon v-else :icon="navigateOutline"></ion-icon>
            <span>{{ gettingLocation ? 'Localisation...' : 'Ma position actuelle' }}</span>
          </button>
          
          <!-- Grille de types -->
          <div class="type-options-grid">
            <div 
              v-for="type in issueTypes" 
              :key="type.id"
              class="type-option-card"
              :class="{ selected: selectedIssueType?.id === type.id }"
              :style="{ '--card-color': type.color }"
              @click="selectIssueType(type)"
            >
              <div class="type-icon-wrapper">
                <span class="type-emoji">{{ type.emoji }}</span>
              </div>
              <span class="type-name">{{ type.label }}</span>
              <div class="selection-indicator">
                <ion-icon :icon="checkmarkCircleOutline"></ion-icon>
              </div>
            </div>
          </div>
          
          <!-- Instruction -->
          <div v-if="selectedIssueType" class="instruction-banner">
            <ion-icon :icon="fingerPrintOutline"></ion-icon>
            <span>Touchez la carte pour placer votre signalement</span>
          </div>
        </div>
      </transition>
      
      <ion-loading :is-open="loading" message="Chargement des signalements..."></ion-loading>
      
      <ion-toast
        :is-open="!!error"
        :message="error"
        :duration="3000"
        color="danger"
        @didDismiss="error = ''"
      ></ion-toast>
      
      <ion-toast
        :is-open="!!successMessage"
        :message="successMessage"
        :duration="3000"
        color="success"
        @didDismiss="successMessage = ''"
      ></ion-toast>
    </ion-content>
    
    <!-- Modal de statistiques redesigné -->
    <ion-modal :is-open="showStatsModal" @didDismiss="showStatsModal = false; isStatsFullscreen = false" class="stats-modal" :class="{ 'stats-fullscreen': isStatsFullscreen }">
      <ion-header class="stats-header">
        <ion-toolbar>
          <div class="stats-header-content">
            <div class="stats-header-info">
              <div class="stats-icon-badge">
                <ion-icon :icon="analyticsOutline"></ion-icon>
              </div>
              <div>
                <h2>Tableau de bord</h2>
                <p>{{ globalStats.totalIssues }} signalements • {{ formatBudgetShort(globalStats.totalBudget) }}</p>
              </div>
            </div>
            <div class="header-actions-group">
              <button class="expand-modal-btn" @click="isStatsFullscreen = !isStatsFullscreen" :title="isStatsFullscreen ? 'Réduire' : 'Agrandir'">
                <ion-icon :icon="isStatsFullscreen ? contractOutline : expandOutline"></ion-icon>
              </button>
              <button class="close-modal-btn" @click="showStatsModal = false">
                <ion-icon :icon="closeOutline"></ion-icon>
              </button>
            </div>
          </div>
        </ion-toolbar>
      </ion-header>
      
      <ion-content class="stats-content">
        <!-- Hero stats -->
        <div class="hero-stats">
          <div class="hero-stat-card main">
            <div class="hero-stat-icon">
              <ion-icon :icon="documentsOutline"></ion-icon>
            </div>
            <div class="hero-stat-info">
              <span class="hero-stat-value">{{ globalStats.totalIssues }}</span>
              <span class="hero-stat-label">Signalements</span>
            </div>
          </div>
          
          <div class="hero-stat-row">
            <div class="hero-stat-card small">
              <span class="small-stat-value">{{ globalStats.totalSurface.toFixed(0) }}</span>
              <span class="small-stat-label">m² Surface</span>
            </div>
            <div class="hero-stat-card small accent">
              <span class="small-stat-value">{{ formatBudgetShort(globalStats.totalBudget) }}</span>
              <span class="small-stat-label">Budget</span>
            </div>
          </div>
        </div>
        
        <!-- Progression circulaire -->
        <div class="progress-card">
          <div class="circular-progress" :style="{ '--progress': globalStats.progressPercentage }">
            <svg viewBox="0 0 100 100">
              <circle class="progress-bg" cx="50" cy="50" r="45"></circle>
              <circle class="progress-fill" cx="50" cy="50" r="45" 
                :stroke-dasharray="`${globalStats.progressPercentage * 2.827} 282.7`"></circle>
            </svg>
            <div class="progress-text">
              <span class="progress-value">{{ globalStats.progressPercentage.toFixed(0) }}%</span>
              <span class="progress-label">Avancement</span>
            </div>
          </div>
          <div class="progress-details">
            <div class="progress-detail-item">
              <span class="detail-dot resolved"></span>
              <span>{{ globalStats.resolvedIssues }} résolus</span>
            </div>
            <div class="progress-detail-item">
              <span class="detail-dot pending"></span>
              <span>{{ globalStats.totalIssues - globalStats.resolvedIssues }} en attente</span>
            </div>
          </div>
        </div>
        
        <!-- Statistiques par type -->
        <div class="section-header">
          <ion-icon :icon="gridOutline"></ion-icon>
          <h3>Par catégorie</h3>
        </div>
        
        <div class="type-stats-grid">
          <div 
            v-for="stat in statsByType" 
            :key="stat.type.id"
            class="type-stat-card"
            :style="{ '--type-color': stat.type.color }"
          >
            <div class="type-stat-header">
              <span class="type-stat-emoji">{{ stat.type.emoji }}</span>
              <span class="type-stat-name">{{ stat.type.label }}</span>
            </div>
            <div class="type-stat-body">
              <div class="type-stat-main">
                <span class="type-stat-count">{{ stat.count }}</span>
                <span class="type-stat-unit">signalements</span>
              </div>
              <div class="type-stat-details">
                <div class="detail-row">
                  <span>Surface</span>
                  <span>{{ stat.totalSurface.toFixed(0) }} m²</span>
                </div>
                <div class="detail-row">
                  <span>Budget</span>
                  <span>{{ formatBudgetShort(stat.totalBudget) }}</span>
                </div>
              </div>
            </div>
            <div class="type-stat-progress">
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: stat.progressPercentage + '%' }"></div>
              </div>
              <span class="progress-text">{{ stat.progressPercentage.toFixed(0) }}% résolus</span>
            </div>
          </div>
        </div>
        
        <!-- Statuts -->
        <div class="section-header">
          <ion-icon :icon="flagOutline"></ion-icon>
          <h3>Par statut</h3>
        </div>
        
        <div class="status-pills">
          <div 
            v-for="stat in statsByStatus" 
            :key="stat.statusId"
            class="status-pill"
            :class="'status-' + stat.statusId"
          >
            <div class="pill-content">
              <span class="pill-count">{{ stat.count }}</span>
              <span class="pill-label">{{ stat.label }}</span>
            </div>
            <div class="pill-percentage">{{ stat.percentage.toFixed(0) }}%</div>
          </div>
        </div>
      </ion-content>
    </ion-modal>
    
    <!-- Modal de création moderne -->
    <ion-modal :is-open="showModal" @didDismiss="closeModal" class="create-modal">
      <ion-header class="create-header">
        <ion-toolbar>
          <div class="create-header-content">
            <div class="create-header-info" v-if="selectedIssueType">
              <div class="selected-type-badge" :style="{ backgroundColor: selectedIssueType.color }">
                <span>{{ selectedIssueType.emoji }}</span>
              </div>
              <div>
                <h2>{{ selectedIssueType.label }}</h2>
                <p class="location-text">
                  <ion-icon :icon="locationOutline"></ion-icon>
                  {{ selectedLocation?.lat.toFixed(4) }}, {{ selectedLocation?.lng.toFixed(4) }}
                </p>
              </div>
            </div>
            <button class="close-modal-btn" @click="closeModal">
              <ion-icon :icon="closeOutline"></ion-icon>
            </button>
          </div>
        </ion-toolbar>
      </ion-header>
      
      <ion-content class="create-content">
        <!-- Formulaire moderne -->
        <div class="form-section">
          <div class="input-group">
            <label class="input-label">
              <ion-icon :icon="textOutline"></ion-icon>
              Titre du signalement
            </label>
            <input 
              v-model="newIssue.title" 
              type="text" 
              class="modern-input"
              placeholder="Ex: Nid de poule dangereux"
            />
          </div>
          
          <div class="input-group">
            <label class="input-label">
              <ion-icon :icon="documentTextOutline"></ion-icon>
              Description détaillée
            </label>
            <textarea 
              v-model="newIssue.description" 
              class="modern-textarea"
              rows="3"
              placeholder="Décrivez le problème en détail..."
            ></textarea>
          </div>
          
          <div class="input-row">
            <div class="input-group half">
              <label class="input-label">
                <ion-icon :icon="resizeOutline"></ion-icon>
                Surface (m²)
              </label>
              <input 
                v-model.number="newIssue.surface" 
                type="number" 
                class="modern-input"
                placeholder="10"
                @input="calculateBudget"
              />
            </div>
            
            <div class="input-group half">
              <label class="input-label">
                <ion-icon :icon="speedometerOutline"></ion-icon>
                Gravité
              </label>
              <div class="severity-selector">
                <button 
                  v-for="n in 10" 
                  :key="n"
                  class="severity-btn"
                  :class="{ active: newIssue.niveau === n, low: n <= 3, medium: n > 3 && n <= 6, high: n > 6 }"
                  @click="newIssue.niveau = n; calculateBudget()"
                >
                  {{ n }}
                </button>
              </div>
              <span class="severity-label">{{ getNiveauLabel(newIssue.niveau) }}</span>
            </div>
          </div>
        </div>
        
        <!-- Budget estimé moderne -->
        <div class="budget-card" v-if="newIssue.surface > 0 && newIssue.niveau > 0">
          <div class="budget-header">
            <ion-icon :icon="walletOutline"></ion-icon>
            <span>Estimation du budget</span>
          </div>
          <div class="budget-calculation">
            <div class="calc-item">
              <span class="calc-label">Prix/m²</span>
              <span class="calc-value">{{ formatBudget(prixForfaitaire) }}</span>
            </div>
            <span class="calc-operator">×</span>
            <div class="calc-item">
              <span class="calc-label">Niveau</span>
              <span class="calc-value">{{ newIssue.niveau }}</span>
            </div>
            <span class="calc-operator">×</span>
            <div class="calc-item">
              <span class="calc-label">Surface</span>
              <span class="calc-value">{{ newIssue.surface }} m²</span>
            </div>
          </div>
          <div class="budget-total">
            <span class="total-label">Total estimé</span>
            <span class="total-value">{{ formatBudget(calculatedBudget) }}</span>
          </div>
        </div>
        
        <!-- Section Photos -->
        <div class="photos-section">
          <div class="photos-header">
            <ion-label>Photos</ion-label>
            <div class="photo-actions">
              <ion-button fill="clear" size="small" @click="takePhoto">
                <ion-icon :icon="cameraOutline" slot="start"></ion-icon>
                Caméra
              </ion-button>
              <ion-button fill="clear" size="small" @click="pickPhotos">
                <ion-icon :icon="imagesOutline" slot="start"></ion-icon>
                Galerie
              </ion-button>
            </div>
          </div>
          
          <div v-if="capturedPhotos.length > 0" class="photos-preview">
            <div 
              v-for="(photo, index) in capturedPhotos" 
              :key="index" 
              class="photo-item"
            >
              <img :src="photo.webPath" :alt="'Photo ' + (index + 1)" />
              <ion-button 
                fill="clear" 
                size="small" 
                class="remove-photo-btn" 
                @click="removePhoto(index)"
              >
                <ion-icon :icon="closeCircleOutline" color="danger"></ion-icon>
              </ion-button>
            </div>
          </div>
          <p v-else class="no-photos-text">Aucune photo ajoutée</p>
        </div>
        
        <ion-item>
          <ion-label>Statut</ion-label>
          <ion-select v-model="newIssue.status" interface="popover">
            <ion-select-option value="signale">Signalé</ion-select-option>
            <ion-select-option value="en_cours">En cours</ion-select-option>
            <ion-select-option value="resolu">Résolu</ion-select-option>
            <ion-select-option value="rejete">Rejeté</ion-select-option>
          </ion-select>
        </ion-item>
        
        <ion-button 
          expand="block" 
          class="ion-margin-top" 
          @click="submitIssue" 
          :disabled="submitting || !newIssue.title || !newIssue.description || !newIssue.surface || !newIssue.niveau"
        >
          <ion-spinner v-if="submitting" name="crescent"></ion-spinner>
          <span v-else>Créer le signalement</span>
        </ion-button>
      </ion-content>
    </ion-modal>
    
    <!-- Modal de visualisation des photos -->
    <ion-modal :is-open="showPhotosModal" @didDismiss="closePhotosModal">
      <ion-header>
        <ion-toolbar>
          <ion-title>Photos - {{ selectedSignalForPhotos?.title }}</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="closePhotosModal">
              <ion-icon :icon="closeOutline"></ion-icon>
            </ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>
      
      <ion-content class="ion-padding">
        <div v-if="selectedSignalForPhotos?.photos && selectedSignalForPhotos.photos.length > 0" class="photos-gallery">
          <div 
            v-for="(photo, index) in selectedSignalForPhotos.photos" 
            :key="index" 
            class="gallery-item"
            @click="openFullscreenPhoto(photo)"
          >
            <img :src="photo" :alt="'Photo ' + (index + 1)" />
            <div class="photo-overlay">
              <ion-icon :icon="expandOutline"></ion-icon>
            </div>
          </div>
        </div>
        <div v-else class="no-photos-container">
          <ion-icon :icon="imagesOutline" class="no-photos-icon"></ion-icon>
          <p>Aucune photo disponible pour ce signalement</p>
        </div>
      </ion-content>
    </ion-modal>
    
    <!-- Modal plein écran pour une photo -->
    <ion-modal :is-open="showFullscreenPhoto" @didDismiss="closeFullscreenPhoto">
      <ion-header>
        <ion-toolbar color="dark">
          <ion-buttons slot="end">
            <ion-button @click="closeFullscreenPhoto" color="light">
              <ion-icon :icon="closeOutline"></ion-icon>
            </ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>
      
      <ion-content class="fullscreen-photo-content">
        <div class="fullscreen-photo-container">
          <img :src="fullscreenPhotoUrl" alt="Photo en plein écran" />
        </div>
      </ion-content>
    </ion-modal>
  </ion-page>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, reactive, computed } from 'vue';
import { 
  IonPage, IonHeader, IonToolbar, IonTitle, IonContent, IonLoading, IonToast,
  IonButton, IonButtons, IonIcon, IonModal, IonItem, IonLabel, IonInput, 
  IonTextarea, IonSelect, IonSelectOption, IonSpinner
} from '@ionic/vue';
import { 
  addOutline, closeOutline, locationOutline, warningOutline, 
  alertCircleOutline, carOutline, waterOutline, constructOutline,
  checkmarkCircleOutline, flashOutline, trashOutline, leafOutline,
  chevronDownOutline, chevronUpOutline, checkboxOutline, squareOutline,
  statsChartOutline, navigateOutline, cameraOutline, imagesOutline, 
  closeCircleOutline, expandOutline, contractOutline, informationCircleOutline, calculatorOutline,
  filterOutline, personOutline, checkmarkDoneOutline, fingerPrintOutline,
  analyticsOutline, documentsOutline, gridOutline, flagOutline,
  textOutline, documentTextOutline, resizeOutline, speedometerOutline, walletOutline
} from 'ionicons/icons';
import { Camera, CameraResultType, CameraSource, Photo } from '@capacitor/camera';
import { Geolocation } from '@capacitor/geolocation';
import { LocalNotifications } from '@capacitor/local-notifications';
import { Capacitor } from '@capacitor/core';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { collection, getDocs, addDoc, Timestamp, doc, getDoc, setDoc } from 'firebase/firestore';
import { db, auth } from '@/config/firebase';
import { getStorage, ref as storageRef, uploadBytes, getDownloadURL } from 'firebase/storage';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { notificationService } from '@/services/notificationService';
import NotificationBell from '@/components/NotificationBell.vue';

// Types de signalements avec icônes et couleurs
interface IssueType {
  id: number;
  label: string;
  icon: string;
  color: string;
  emoji: string;
}

const issueTypes: IssueType[] = [
  { id: 1, label: 'Danger', icon: warningOutline, color: '#dc3545', emoji: '⚠️' },
  { id: 2, label: 'Accident', icon: carOutline, color: '#9c27b0', emoji: '🚗' },
  { id: 3, label: 'Travaux', icon: constructOutline, color: '#ff9800', emoji: '🚧' },
  { id: 4, label: 'Inondation', icon: waterOutline, color: '#2196f3', emoji: '🌊' },
  { id: 5, label: 'Nid de poule', icon: alertCircleOutline, color: '#c62828', emoji: '🕳️' },
  { id: 6, label: 'Résolu', icon: checkmarkCircleOutline, color: '#4caf50', emoji: '✅' },
  { id: 7, label: 'Électricité', icon: flashOutline, color: '#ffc107', emoji: '⚡' },
  { id: 8, label: 'Déchets', icon: trashOutline, color: '#795548', emoji: '🗑️' },
  { id: 9, label: 'Végétation', icon: leafOutline, color: '#8bc34a', emoji: '🌿' }
];

// Mapping des statuts (string <-> number)
const statusMapping = {
  'signale': { id: 1, label: 'Signalé' },
  'en_cours': { id: 2, label: 'En cours' },
  'resolu': { id: 3, label: 'Résolu' },
  'rejete': { id: 4, label: 'Rejeté' }
};

const statusIdToString: Record<number, string> = {
  1: 'signale',
  2: 'en_cours',
  3: 'resolu',
  4: 'rejete'
};

// Interface unifiée pour les signalements (compatible avec les deux collections)
interface Signal {
  id: string;
  title: string;
  description: string;
  latitude: number;
  longitude: number;
  surface: number;
  budget: number;
  niveau: number; // Ajout du niveau
  status: string;
  statusId: number;
  typeId: number;
  type: string;
  color: string;
  icon: string;
  reportedBy: string;
  createdAt: Timestamp;
  updatedAt?: Timestamp;
  companyId?: number;
  photos?: string[];
  source: 'signals' | 'road_issues';
}

// Référence pour le conteneur de la carte
const mapContainer = ref(null);
let map: L.Map | null = null;
let tempMarker: L.Marker | null = null;
const loading = ref(false);
const error = ref('');
const successMessage = ref('');
const signals = ref<Signal[]>([]);

// État pour le mode signalement
const isSignalMode = ref(false);
const showModal = ref(false);
const showStatsModal = ref(false);
const isStatsFullscreen = ref(false);
const submitting = ref(false);
const selectedLocation = ref<{ lat: number; lng: number } | null>(null);
const selectedIssueType = ref<IssueType | null>(null);
const legendCollapsed = ref(false);

// État pour la visualisation des photos
const showPhotosModal = ref(false);
const selectedSignalForPhotos = ref<Signal | null>(null);
const showFullscreenPhoto = ref(false);
const fullscreenPhotoUrl = ref('');

// Filtres
const selectedFilterTypes = ref<number[]>(issueTypes.map(t => t.id));
const showMyIssuesOnly = ref(false);
let allMarkers: L.Marker[] = [];

// Photos et géolocalisation
const capturedPhotos = ref<Photo[]>([]);
const gettingLocation = ref(false);

// Formulaire pour nouveau signalement
const newIssue = reactive({
  title: '',
  description: '',
  surface: 0,
  niveau: 1, // Niveau par défaut
  status: 'signale'
});

// Budget calculé automatiquement
const calculatedBudget = computed(() => {
  return prixForfaitaire.value * newIssue.niveau * newIssue.surface;
});

// Prix forfaitaire
const prixForfaitaire = ref<number>(50000); // Valeur par défaut
const loadingPrix = ref(false);

// Charger le prix forfaitaire depuis Firebase
const loadPrixForfaitaire = async () => {
  loadingPrix.value = true;
  try {
    // Essayer de charger depuis la collection prix_forfaitaire
    const prixDocRef = doc(db, 'prix_forfaitaire', 'config');
    const prixDoc = await getDoc(prixDocRef);
    
    if (prixDoc.exists()) {
      const data = prixDoc.data();
      prixForfaitaire.value = data.prix_par_m2 || 50000;
      console.log('💰 Prix forfaitaire chargé:', prixForfaitaire.value);
    } else {
      // Créer le document avec la valeur par défaut
      await setDoc(prixDocRef, {
        prix_par_m2: 50000,
        updated_at: Timestamp.now()
      });
      console.log('💰 Prix forfaitaire créé avec valeur par défaut: 50000');
    }
  } catch (error) {
    console.error('❌ Erreur chargement prix forfaitaire:', error);
    // Utiliser la valeur par défaut en cas d'erreur
    prixForfaitaire.value = 50000;
  } finally {
    loadingPrix.value = false;
  }
};

// Obtenir le label du niveau
const getNiveauLabel = (niveau: number): string => {
  const labels: Record<number, string> = {
    1: 'Très faible',
    2: 'Faible',
    3: 'Faible+',
    4: 'Modéré',
    5: 'Moyen',
    6: 'Moyen+',
    7: 'Élevé',
    8: 'Élevé+',
    9: 'Très élevé',
    10: 'Critique'
  };
  return labels[niveau] || 'Inconnu';
};

// Fonction pour calculer le budget (appelée lors des changements)
const calculateBudget = () => {
  // Le budget est calculé via computed, cette fonction peut être utilisée
  // pour des effets secondaires si nécessaire
  console.log(`📊 Calcul budget: ${prixForfaitaire.value} × ${newIssue.niveau} × ${newIssue.surface} = ${calculatedBudget.value}`);
};

// Fonction pour parser le budget (gère string et number)
const parseBudget = (budget: any): number => {
  if (typeof budget === 'number') return budget;
  if (typeof budget === 'string') return parseFloat(budget) || 0;
  return 0;
};

// Fonction pour parser la surface (gère string et number)
const parseSurface = (surface: any): number => {
  if (typeof surface === 'number') return surface;
  if (typeof surface === 'string') return parseFloat(surface) || 0;
  return 0;
};

// Fonction pour normaliser le statut
const normalizeStatus = (status: any, statusId?: number): { status: string; statusId: number } => {
  if (typeof statusId === 'number' && statusId >= 1 && statusId <= 4) {
    return { 
      status: statusIdToString[statusId], 
      statusId 
    };
  }
  
  if (typeof status === 'string') {
    const normalized = status.toLowerCase().replace(/[éè]/g, 'e').replace(/\s+/g, '_');
    const mapping = statusMapping[normalized as keyof typeof statusMapping];
    if (mapping) {
      return { status: normalized, statusId: mapping.id };
    }
  }
  
  return { status: 'signale', statusId: 1 };
};

// Statistiques globales
const globalStats = computed(() => {
  const issues = signals.value;
  const totalIssues = issues.length;
  const totalSurface = issues.reduce((sum, i) => sum + (i.surface || 0), 0);
  const totalBudget = issues.reduce((sum, i) => sum + (i.budget || 0), 0);
  const resolvedIssues = issues.filter(i => i.statusId === 3).length;
  const progressPercentage = totalIssues > 0 ? (resolvedIssues / totalIssues) * 100 : 0;
  
  return {
    totalIssues,
    totalSurface,
    totalBudget,
    resolvedIssues,
    progressPercentage
  };
});

// Statistiques par type
const statsByType = computed(() => {
  return issueTypes.map(type => {
    const issuesOfType = signals.value.filter(i => i.typeId === type.id);
    const count = issuesOfType.length;
    const totalSurface = issuesOfType.reduce((sum, i) => sum + (i.surface || 0), 0);
    const totalBudget = issuesOfType.reduce((sum, i) => sum + (i.budget || 0), 0);
    const resolvedCount = issuesOfType.filter(i => i.statusId === 3).length;
    const progressPercentage = count > 0 ? (resolvedCount / count) * 100 : 0;
    
    return {
      type,
      count,
      totalSurface,
      totalBudget,
      resolvedCount,
      progressPercentage
    };
  }).filter(stat => stat.count > 0);
});

// Statistiques par statut
const statsByStatus = computed(() => {
  const statuses = [
    { statusId: 1, label: 'Signalé', color: '#ff9800' },
    { statusId: 2, label: 'En cours', color: '#2196f3' },
    { statusId: 3, label: 'Résolu', color: '#4caf50' },
    { statusId: 4, label: 'Rejeté', color: '#9e9e9e' }
  ];
  
  const total = signals.value.length;
  
  return statuses.map(status => {
    const count = signals.value.filter(i => i.statusId === status.statusId).length;
    const percentage = total > 0 ? (count / total) * 100 : 0;
    
    return {
      ...status,
      count,
      percentage
    };
  });
});

// Formater le budget en format court
const formatBudgetShort = (budget: number): string => {
  if (budget >= 1000000000) {
    return (budget / 1000000000).toFixed(1) + ' Mrd';
  } else if (budget >= 1000000) {
    return (budget / 1000000).toFixed(1) + ' M';
  } else if (budget >= 1000) {
    return (budget / 1000).toFixed(1) + ' K';
  }
  return budget.toString();
};

// Correction du problème des icônes de Leaflet
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

// Créer une icône personnalisée avec couleur et symbole
const createCustomIcon = (color: string, iconSymbol: string): L.DivIcon => {
  return L.divIcon({
    className: 'custom-div-icon',
    html: `
      <div style="
        background-color: ${color};
        width: 36px;
        height: 36px;
        border-radius: 50% 50% 50% 0;
        transform: rotate(-45deg);
        display: flex;
        align-items: center;
        justify-content: center;
        border: 3px solid white;
        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
      ">
        <ion-icon name="${iconSymbol}" style="
          transform: rotate(45deg);
          color: white;
          font-size: 18px;
        "></ion-icon>
      </div>
    `,
    iconSize: [36, 36],
    iconAnchor: [18, 36],
    popupAnchor: [0, -36]
  });
};

// Obtenir l'icône pour un type de signalement
const getIconForIssueType = (typeId: number): L.DivIcon => {
  const type = issueTypes.find(t => t.id === typeId) || issueTypes[0];
  const iconName = getIonIconName(type.icon);
  return createCustomIcon(type.color, iconName);
};

// Convertir l'icône ionicons en nom
const getIonIconName = (icon: string): string => {
  const iconMap: Record<string, string> = {
    [warningOutline]: 'warning-outline',
    [carOutline]: 'car-outline',
    [constructOutline]: 'construct-outline',
    [waterOutline]: 'water-outline',
    [alertCircleOutline]: 'alert-circle-outline',
    [checkmarkCircleOutline]: 'checkmark-circle-outline',
    [flashOutline]: 'flash-outline',
    [trashOutline]: 'trash-outline',
    [leafOutline]: 'leaf-outline'
  };
  return iconMap[icon] || 'alert-circle-outline';
};

// Fonction pour obtenir le statut en texte
const getStatusText = (statusId: number): string => {
  const statuses: Record<number, string> = {
    1: 'Signalé',
    2: 'En cours',
    3: 'Résolu',
    4: 'Rejeté'
  };
  return statuses[statusId] || 'Inconnu';
};

// Fonction pour formater la date
const formatDate = (timestamp: Timestamp): string => {
  if (!timestamp || !timestamp.toDate) return 'N/A';
  return timestamp.toDate().toLocaleDateString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// Fonction pour formater le budget
const formatBudget = (budget: number): string => {
  return new Intl.NumberFormat('fr-MG', {
    style: 'currency',
    currency: 'MGA',
    maximumFractionDigits: 0
  }).format(budget);
};

// Ouvrir le modal des photos pour un signalement
const openPhotosModal = (signalId: string) => {
  const signal = signals.value.find(s => s.id === signalId);
  if (signal) {
    selectedSignalForPhotos.value = signal;
    showPhotosModal.value = true;
  }
};

// Fermer le modal des photos
const closePhotosModal = () => {
  showPhotosModal.value = false;
  selectedSignalForPhotos.value = null;
};

// Ouvrir une photo en plein écran
const openFullscreenPhoto = (photoUrl: string) => {
  fullscreenPhotoUrl.value = photoUrl;
  showFullscreenPhoto.value = true;
};

// Fermer la photo en plein écran
const closeFullscreenPhoto = () => {
  showFullscreenPhoto.value = false;
  fullscreenPhotoUrl.value = '';
};

// Exposer la fonction globalement pour les boutons dans les popups
const setupGlobalPhotoHandler = () => {
  (window as any).openSignalPhotos = (signalId: string) => {
    openPhotosModal(signalId);
  };
};

// Créer le contenu du popup avec bouton photos
const createPopupContent = (signal: Signal): string => {
  const type = issueTypes.find(t => t.id === signal.typeId) || issueTypes[0];
  const hasPhotos = signal.photos && signal.photos.length > 0;
  const photoCount = signal.photos?.length || 0;
  
  return `
    <div class="issue-popup">
      <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
        <span style="
          background-color: ${type.color};
          color: white;
          padding: 4px 8px;
          border-radius: 12px;
          font-size: 11px;
          font-weight: 600;
        ">${type.label}</span>
        <span style="
          background-color: ${getNiveauColor(signal.niveau)};
          color: white;
          padding: 4px 8px;
          border-radius: 12px;
          font-size: 11px;
          font-weight: 600;
        ">Niveau ${signal.niveau}</span>
        ${hasPhotos ? `
          <span style="
            background-color: #2196f3;
            color: white;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 600;
          ">
            📷 ${photoCount}
          </span>
        ` : ''}
      </div>
      <h3 style="margin: 0 0 8px 0; color: #333;">${signal.title}</h3>
      <p style="margin: 0 0 8px 0; font-size: 13px; color: #666;">${signal.description}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 8px 0;">
      <table style="font-size: 12px; width: 100%;">
        <tr>
          <td><strong>Surface:</strong></td>
          <td>${signal.surface} m²</td>
        </tr>
        <tr>
          <td><strong>Niveau:</strong></td>
          <td>${signal.niveau}/10 (${getNiveauLabel(signal.niveau)})</td>
        </tr>
        <tr>
          <td><strong>Budget:</strong></td>
          <td>${formatBudget(signal.budget)}</td>
        </tr>
        <tr>
          <td><strong>Statut:</strong></td>
          <td><span style="padding: 2px 6px; background: #ffeb3b; border-radius: 4px; font-size: 11px;">${getStatusText(signal.statusId)}</span></td>
        </tr>
        <tr>
          <td><strong>Signalé le:</strong></td>
          <td>${formatDate(signal.createdAt)}</td>
        </tr>
      </table>
      ${hasPhotos ? `
        <button 
          onclick="window.openSignalPhotos('${signal.id}')"
          style="
            margin-top: 12px;
            width: 100%;
            padding: 10px 16px;
            background: linear-gradient(135deg, #2196f3, #1976d2);
            color: white;
            border: none;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
          "
        >
          📷 Voir les photos (${photoCount})
        </button>
      ` : ''}
    </div>
  `;
};

// Créer le contenu du tooltip (info rapide au survol)
const createTooltipContent = (signal: Signal): string => {
  const type = issueTypes.find(t => t.id === signal.typeId) || issueTypes[0];
  return `
    <div style="text-align: center;">
      <strong>${signal.title}</strong><br>
      <span style="color: ${type.color};">${type.label}</span> | Niveau ${signal.niveau}
    </div>
  `;
};

// Fonction pour obtenir la couleur selon le niveau
const getNiveauColor = (niveau: number): string => {
  if (niveau <= 2) return '#4caf50'; // Vert
  if (niveau <= 4) return '#8bc34a'; // Vert clair
  if (niveau <= 6) return '#ff9800'; // Orange
  if (niveau <= 8) return '#f44336'; // Rouge
  return '#9c27b0'; // Violet (critique)
};

// Charger les signalements depuis Firestore (les deux collections)
const loadSignals = async () => {
  loading.value = true;
  try {
    const allSignals: Signal[] = [];
    
    // Charger depuis 'signals'
    const signalsSnapshot = await getDocs(collection(db, 'signals'));
    signalsSnapshot.docs.forEach(doc => {
      const data = doc.data();
      const typeId = data.typeId || 1;
      const type = issueTypes.find(t => t.id === typeId) || issueTypes[0];
      const { status, statusId } = normalizeStatus(data.status, data.statusId);
      
      let photos: string[] = [];
      if (data.photosBase64 && Array.isArray(data.photosBase64)) {
        photos = data.photosBase64.map((base64: string) => `data:image/jpeg;base64,${base64}`);
      } else if (data.photos) {
        photos = data.photos;
      }
      
      allSignals.push({
        id: doc.id,
        title: data.title || '',
        description: data.description || '',
        latitude: data.latitude || 0,
        longitude: data.longitude || 0,
        surface: parseSurface(data.surface),
        budget: parseBudget(data.budget),
        niveau: data.niveau || 1, // Ajout du niveau
        status,
        statusId,
        typeId,
        type: data.type || type.label,
        color: data.color || type.color,
        icon: data.icon || type.emoji,
        reportedBy: data.reportedBy || data.userId || '',
        createdAt: data.createdAt,
        updatedAt: data.updatedAt,
        photos: photos,
        source: 'signals'
      });
    });
    
    // Charger depuis 'road_issues'
    const roadIssuesSnapshot = await getDocs(collection(db, 'road_issues'));
    roadIssuesSnapshot.docs.forEach(doc => {
      const data = doc.data();
      const typeId = data.typeId || data.issueTypeId || 1;
      const type = issueTypes.find(t => t.id === typeId) || issueTypes[0];
      const { status, statusId } = normalizeStatus(data.status, data.statusId);
      
      let photos: string[] = [];
      if (data.photosBase64 && Array.isArray(data.photosBase64)) {
        photos = data.photosBase64.map((base64: string) => `data:image/jpeg;base64,${base64}`);
      } else if (data.photos) {
        photos = data.photos;
      }
      
      allSignals.push({
        id: doc.id,
        title: data.title || '',
        description: data.description || '',
        latitude: data.latitude || 0,
        longitude: data.longitude || 0,
        surface: parseSurface(data.surfaceM2 || data.surface),
        budget: parseBudget(data.budget),
        niveau: data.niveau || 1, // Ajout du niveau
        status,
        statusId,
        typeId,
        type: type.label,
        color: type.color,
        icon: type.emoji,
        reportedBy: data.reportedBy || '',
        createdAt: data.reportedAt || data.createdAt,
        updatedAt: data.updatedAt,
        companyId: data.companyId,
        photos: photos,
        source: 'road_issues'
      });
    });
    
    signals.value = allSignals;
    console.log(`${signals.value.length} signalements chargés (signals: ${signalsSnapshot.size}, road_issues: ${roadIssuesSnapshot.size})`);
  } catch (e: any) {
    console.error('Erreur lors du chargement:', e);
    error.value = e.message || 'Erreur lors du chargement des signalements';
  } finally {
    loading.value = false;
  }
};

// Ajouter les marqueurs sur la carte
const addMarkersToMap = () => {
  if (!map) return;
  
  // Nettoyer les anciens marqueurs
  allMarkers.forEach(marker => map!.removeLayer(marker));
  allMarkers = [];
  
  const currentUserId = auth.currentUser?.uid;
  
  signals.value.forEach(signal => {
    if (signal.latitude && signal.longitude) {
      const typeId = signal.typeId || 1;
      
      if (!selectedFilterTypes.value.includes(typeId)) return;
      if (showMyIssuesOnly.value && signal.reportedBy !== currentUserId) return;
      
      const icon = getIconForIssueType(typeId);
      
      const marker = L.marker(
        [signal.latitude, signal.longitude],
        { icon }
      ).addTo(map!);
      
      marker.bindPopup(createPopupContent(signal), {
        maxWidth: 320,
        className: 'custom-popup'
      });
      
      marker.bindTooltip(createTooltipContent(signal), {
        permanent: false,
        direction: 'top',
        offset: [0, -40],
        className: 'custom-tooltip'
      });
      
      allMarkers.push(marker);
    }
  });
};

// Sélectionner un type de signalement
const selectIssueType = (type: IssueType) => {
  selectedIssueType.value = type;
  
  const mapElement = document.getElementById('map');
  if (mapElement) {
    mapElement.style.cursor = 'crosshair';
  }
};

// Fonctions de filtrage
const toggleFilterType = (typeId: number) => {
  const index = selectedFilterTypes.value.indexOf(typeId);
  if (index > -1) {
    selectedFilterTypes.value.splice(index, 1);
  } else {
    selectedFilterTypes.value.push(typeId);
  }
  addMarkersToMap();
};

const selectAllTypes = () => {
  selectedFilterTypes.value = issueTypes.map(t => t.id);
  addMarkersToMap();
};

const deselectAllTypes = () => {
  selectedFilterTypes.value = [];
  addMarkersToMap();
};

const toggleMyIssuesOnly = () => {
  showMyIssuesOnly.value = !showMyIssuesOnly.value;
  addMarkersToMap();
};

const getCountForType = (typeId: number): number => {
  const currentUserId = auth.currentUser?.uid;
  return signals.value.filter(signal => {
    const matchType = signal.typeId === typeId;
    const matchUser = !showMyIssuesOnly.value || signal.reportedBy === currentUserId;
    return matchType && matchUser;
  }).length;
};

// Activer/désactiver le mode signalement
const toggleSignalMode = () => {
  isSignalMode.value = !isSignalMode.value;
  
  if (!isSignalMode.value) {
    if (tempMarker && map) {
      map.removeLayer(tempMarker);
      tempMarker = null;
    }
    selectedLocation.value = null;
    selectedIssueType.value = null;
    
    const mapElement = document.getElementById('map');
    if (mapElement) {
      mapElement.style.cursor = '';
    }
  }
};

// Gérer le clic sur la carte
const handleMapClick = (e: L.LeafletMouseEvent) => {
  if (!isSignalMode.value || !selectedIssueType.value) return;
  
  const { lat, lng } = e.latlng;
  selectedLocation.value = { lat, lng };
  
  if (tempMarker && map) {
    map.removeLayer(tempMarker);
  }
  
  const icon = getIconForIssueType(selectedIssueType.value.id);
  tempMarker = L.marker([lat, lng], { icon }).addTo(map!);
  tempMarker.bindPopup('Nouvel emplacement sélectionné').openPopup();
  
  showModal.value = true;
};

// Fermer le modal
const closeModal = () => {
  showModal.value = false;
  newIssue.title = '';
  newIssue.description = '';
  newIssue.surface = 0;
  newIssue.niveau = 1; // Réinitialiser le niveau
  newIssue.status = 'signale';
  capturedPhotos.value = [];
};

// Prendre une photo avec la caméra
const takePhoto = async () => {
  try {
    const photo = await Camera.getPhoto({
      quality: 80,
      allowEditing: false,
      resultType: CameraResultType.Uri,
      source: CameraSource.Camera
    });
    capturedPhotos.value.push(photo);
    successMessage.value = 'Photo ajoutée !';
  } catch (e: any) {
    if (e.message !== 'User cancelled photos app') {
      console.error('Erreur caméra:', e);
      error.value = 'Erreur lors de la prise de photo';
    }
  }
};

// Sélectionner des photos depuis la galerie
const pickPhotos = async () => {
  try {
    const photos = await Camera.pickImages({
      quality: 80,
      limit: 5
    });
    photos.photos.forEach(photo => {
      capturedPhotos.value.push({
        webPath: photo.webPath,
        format: photo.format
      } as Photo);
    });
    successMessage.value = `${photos.photos.length} photo(s) ajoutée(s) !`;
  } catch (e: any) {
    if (e.message !== 'User cancelled photos app') {
      console.error('Erreur galerie:', e);
      error.value = 'Erreur lors de la sélection des photos';
    }
  }
};

// Supprimer une photo
const removePhoto = (index: number) => {
  capturedPhotos.value.splice(index, 1);
};

// Utiliser ma position GPS
const useMyLocation = async () => {
  if (!selectedIssueType.value) {
    error.value = 'Veuillez d\'abord sélectionner un type de signalement';
    return;
  }
  
  gettingLocation.value = true;
  
  try {
    const position = await Geolocation.getCurrentPosition({
      enableHighAccuracy: true,
      timeout: 10000
    });
    
    const { latitude, longitude } = position.coords;
    selectedLocation.value = { lat: latitude, lng: longitude };
    
    if (map) {
      map.setView([latitude, longitude], 17);
      
      if (tempMarker) {
        map.removeLayer(tempMarker);
      }
      
      const icon = getIconForIssueType(selectedIssueType.value.id);
      tempMarker = L.marker([latitude, longitude], { icon }).addTo(map);
      tempMarker.bindPopup('Ma position actuelle').openPopup();
    }
    
    successMessage.value = 'Position récupérée !';
    showModal.value = true;
    
  } catch (e: any) {
    console.error('Erreur géolocalisation:', e);
    error.value = 'Impossible de récupérer votre position. Vérifiez les permissions.';
  } finally {
    gettingLocation.value = false;
  }
};

// Générer un UUID v4
const generateUUID = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
};

// Fonction améliorée pour convertir une Photo en Blob
const photoToBlob = async (photo: Photo): Promise<Blob> => {
  if (!photo.webPath) {
    throw new Error('Photo sans webPath');
  }

  try {
    // Cas 1: URL blob locale (navigateur web)
    if (photo.webPath.startsWith('blob:')) {
      console.log('📸 Conversion blob URL:', photo.webPath);
      const response = await fetch(photo.webPath);
      const blob = await response.blob();
      console.log('✅ Blob créé:', blob.size, 'bytes', blob.type);
      return blob;
    }

    // Cas 2: Chemin capacitor:// (mobile)
    if (photo.webPath.startsWith('capacitor://')) {
      console.log('📸 Lecture depuis Capacitor:', photo.webPath);
      const base64Data = await Filesystem.readFile({
        path: photo.webPath
      });
      
      // Convertir base64 en blob
      const base64String = typeof base64Data.data === 'string' 
        ? base64Data.data 
        : base64Data.data.toString();
      
      const mimeType = `image/${photo.format || 'jpeg'}`;
      const base64Response = await fetch(`data:${mimeType};base64,${base64String}`);
      const blob = await base64Response.blob();
      console.log('✅ Blob créé depuis Capacitor:', blob.size, 'bytes', blob.type);
      return blob;
    }

    // Cas 3: Chemin file:// (mobile)
    if (photo.webPath.startsWith('file://')) {
      console.log('📸 Lecture depuis file://', photo.webPath);
      
      // Extraire le chemin sans file://
      const filePath = photo.webPath.replace('file://', '');
      
      const base64Data = await Filesystem.readFile({
        path: filePath
      });
      
      const base64String = typeof base64Data.data === 'string' 
        ? base64Data.data 
        : base64Data.data.toString();
      
      const mimeType = `image/${photo.format || 'jpeg'}`;
      const base64Response = await fetch(`data:${mimeType};base64,${base64String}`);
      const blob = await base64Response.blob();
      console.log('✅ Blob créé depuis file://', blob.size, 'bytes', blob.type);
      return blob;
    }

    // Cas 4: HTTP/HTTPS URL
    if (photo.webPath.startsWith('http://') || photo.webPath.startsWith('https://')) {
      console.log('📸 Téléchargement depuis URL:', photo.webPath);
      const response = await fetch(photo.webPath);
      const blob = await response.blob();
      console.log('✅ Blob téléchargé:', blob.size, 'bytes', blob.type);
      return blob;
    }

    // Fallback: essayer de fetch directement
    console.log('📸 Tentative fetch direct:', photo.webPath);
    const response = await fetch(photo.webPath);
    const blob = await response.blob();
    console.log('✅ Blob créé (fallback):', blob.size, 'bytes', blob.type);
    return blob;
    
  } catch (error) {
    console.error('❌ Erreur conversion photo en blob:', error);
    console.error('Photo webPath:', photo.webPath);
    console.error('Photo format:', photo.format);
    throw new Error(`Impossible de convertir la photo en blob: ${error}`);
  }
};

// Fonction améliorée pour uploader une photo vers Firebase Storage
const uploadPhotoToStorage = async (photo: Photo, issueId: string, index: number): Promise<string> => {
  try {
    console.log(`📤 Upload photo ${index + 1}...`);
    const storage = getStorage();
    const blob = await photoToBlob(photo);
    
    // Vérifier la taille du blob
    if (blob.size === 0) {
      throw new Error('Fichier vide (0 bytes)');
    }
    
    if (blob.size > 10 * 1024 * 1024) { // 10 MB max
      throw new Error(`Fichier trop volumineux: ${(blob.size / 1024 / 1024).toFixed(2)} MB`);
    }
    
    // Déterminer l'extension et le type MIME
    let extension = photo.format || 'jpg';
    let mimeType = blob.type || 'image/jpeg';
    
    // Normaliser l'extension
    if (extension === 'jpeg') extension = 'jpg';
    
    // S'assurer que le type MIME correspond à l'extension
    if (!mimeType.startsWith('image/')) {
      mimeType = `image/${extension}`;
    }
    
    const fileName = `image_${index}_${Date.now()}.${extension}`;
    const storagePath = `road_issues/${issueId}/${fileName}`;
    
    console.log(`📁 Chemin: ${storagePath}`);
    console.log(`📊 Taille: ${(blob.size / 1024).toFixed(2)} KB`);
    console.log(`🎨 Type: ${mimeType}`);
    
    // Créer la référence Firebase Storage
    const imageRef = storageRef(storage, storagePath);
    
    // Uploader l'image avec metadata
    const metadata = {
      contentType: mimeType,
      customMetadata: {
        uploadedFrom: 'mobile-app',
        issueId: issueId,
        originalFormat: photo.format || 'unknown'
      }
    };
    
    await uploadBytes(imageRef, blob, metadata);
    
    // Récupérer l'URL de téléchargement
    const downloadUrl = await getDownloadURL(imageRef);
    
    console.log(`✅ Image ${index + 1} uploadée avec succès!`);
    console.log(`🔗 URL: ${downloadUrl.substring(0, 50)}...`);
    
    return downloadUrl;
    
  } catch (error) {
    console.error(`❌ Erreur upload photo ${index + 1}:`, error);
    throw error;
  }
};

// Fonction pour compresser les images
const compressImage = async (blob: Blob, maxWidth = 400, quality = 0.7): Promise<Blob> => {
  return new Promise((resolve, reject) => {
    const img = new Image();
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    
    img.onload = () => {
      // Calculer les nouvelles dimensions
      let width = img.width;
      let height = img.height;
      
      if (width > maxWidth) {
        height = (height * maxWidth) / width;
        width = maxWidth;
      }
      
      canvas.width = width;
      canvas.height = height;
      
      // Dessiner l'image redimensionnée
      ctx?.drawImage(img, 0, 0, width, height);
      
      // Convertir en blob compressé (JPEG pour meilleure compression)
      canvas.toBlob(
        (compressedBlob) => {
          if (compressedBlob) {
            const originalSize = (blob.size / 1024).toFixed(2);
            const compressedSize = (compressedBlob.size / 1024).toFixed(2);
            console.log(`📦 Compression: ${originalSize}KB → ${compressedSize}KB (${((1 - compressedBlob.size / blob.size) * 100).toFixed(1)}% réduit)`);
            resolve(compressedBlob);
          } else {
            reject(new Error('Erreur de compression'));
          }
        },
        'image/jpeg',
        quality
      );
    };
    
    img.onerror = reject;
    img.src = URL.createObjectURL(blob);
  });
};

// Fonction pour convertir un Blob en base64
const blobToBase64 = (blob: Blob): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const base64String = reader.result as string;
      // Retirer le préfixe "data:image/jpeg;base64,"
      const base64Data = base64String.split(',')[1];
      resolve(base64Data);
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
};

// Fonction pour convertir et compresser une photo en base64
const photoToBase64 = async (photo: Photo): Promise<string> => {
  try {
    console.log('📸 Conversion photo en base64...');
    
    // Étape 1: Convertir en Blob
    const blob = await photoToBlob(photo);
    console.log(`📊 Taille originale: ${(blob.size / 1024).toFixed(2)} KB`);
    
    // Étape 2: Compresser l'image (400px max, qualité 70%)
    const compressedBlob = await compressImage(blob, 400, 0.7);
    
    // Vérifier que la taille compressée ne dépasse pas ~200 KB
    if (compressedBlob.size > 200 * 1024) {
      console.warn('⚠️ Image encore trop volumineuse, compression supplémentaire...');
      // Recompresser avec qualité réduite
      const recompressedBlob = await compressImage(blob, 350, 0.6);
      const base64 = await blobToBase64(recompressedBlob);
      console.log(`✅ Taille finale: ${(recompressedBlob.size / 1024).toFixed(2)} KB`);
      return base64;
    }
    
    // Étape 3: Convertir en base64
    const base64 = await blobToBase64(compressedBlob);
    console.log(`✅ Conversion réussie: ${(compressedBlob.size / 1024).toFixed(2)} KB`);
    
    return base64;
    
  } catch (error) {
    console.error('❌ Erreur conversion base64:', error);
    throw new Error(`Impossible de convertir la photo: ${error}`);
  }
};

// REMPLACER la fonction submitIssue par celle-ci
const submitIssue = async () => {
  if (!selectedLocation.value || !newIssue.title || !newIssue.description || !selectedIssueType.value) {
    error.value = 'Veuillez remplir tous les champs obligatoires';
    return;
  }
  
  if (!newIssue.surface || newIssue.surface <= 0) {
    error.value = 'Veuillez entrer une surface valide';
    return;
  }
  
  if (!newIssue.niveau || newIssue.niveau < 1 || newIssue.niveau > 10) {
    error.value = 'Veuillez sélectionner un niveau de gravité (1-10)';
    return;
  }

  

  if (capturedPhotos.value.length > 3) {
    error.value = 'Maximum 3 photos par signalement';
    return;
  }
  
  submitting.value = true;
  
  try {
    const currentUser = auth.currentUser;
    const reportedBy = currentUser?.uid || 'anonymous';
    const type = selectedIssueType.value;
    const statusId = statusMapping[newIssue.status as keyof typeof statusMapping]?.id || 1;
    const issueId = generateUUID();
    
    // Calculer le budget automatiquement
    const budget = prixForfaitaire.value * newIssue.niveau * newIssue.surface;
    
    console.log(`🆕 Création du signalement ${issueId}`);
    console.log(`📊 Budget calculé: ${prixForfaitaire.value} × ${newIssue.niveau} × ${newIssue.surface} = ${budget}`);
    console.log(`📸 Nombre de photos: ${capturedPhotos.value.length}`);
    
    // Convertir les photos en base64
    const photosBase64: string[] = [];
    const failedPhotos: number[] = [];
    
    if (capturedPhotos.value.length > 0) {
      successMessage.value = `Compression des photos (0/${capturedPhotos.value.length})...`;
      
      for (let i = 0; i < capturedPhotos.value.length; i++) {
        const photo = capturedPhotos.value[i];
        
        try {
          successMessage.value = `Compression des photos (${i + 1}/${capturedPhotos.value.length})...`;
          const base64 = await photoToBase64(photo);
          photosBase64.push(base64);
          
        } catch (conversionError: any) {
          console.error(`❌ Échec conversion photo ${i + 1}:`, conversionError);
          failedPhotos.push(i + 1);
        }
      }
      
      console.log(`✅ ${photosBase64.length}/${capturedPhotos.value.length} photos converties`);
      
      if (failedPhotos.length > 0) {
        console.warn(`⚠️ Photos échouées: ${failedPhotos.join(', ')}`);
      }
    }
    
    const now = Timestamp.now();
    
    // Créer le signalement avec le niveau et le budget calculé
    const issueData = {
      id: issueId,
      title: newIssue.title,
      description: newIssue.description,
      latitude: selectedLocation.value.lat,
      longitude: selectedLocation.value.lng,
      surfaceM2: parseFloat(newIssue.surface.toFixed(2)),
      niveau: newIssue.niveau, // Ajout du niveau
      budget: parseFloat(budget.toFixed(2)), // Budget calculé
      prixForfaitaireUtilise: prixForfaitaire.value, // Stocker le prix utilisé pour référence
      statusId: statusId,
      typeId: type.id,
      companyId: null,
      photosBase64: photosBase64,
      reportedBy: reportedBy,
      reportedAt: now,
      updatedAt: now
    };
    
    // Vérifier la taille du document
    const estimatedSize = JSON.stringify(issueData).length;
    console.log(`📦 Taille estimée: ${(estimatedSize / 1024).toFixed(2)} KB`);
    
    if (estimatedSize > 900 * 1024) {
      throw new Error('Document trop volumineux. Réduisez le nombre de photos.');
    }
    
    console.log('💾 Sauvegarde dans Firestore...');
    
    const docRef = await addDoc(collection(db, 'road_issues'), issueData);
    

    console.log('✅ Signalement créé avec ID:', docRef.id);
    
    let message = `Signalement créé ! Budget estimé: ${formatBudget(budget)}`;
    if (failedPhotos.length > 0) {
      message += ` (${failedPhotos.length} photo(s) échouée(s))`;
    }
    
    successMessage.value = message;
    
    closeModal();
    isSignalMode.value = false;
    selectedIssueType.value = null;
    
    if (tempMarker && map) {
      map.removeLayer(tempMarker);
      tempMarker = null;
    }
    
    capturedPhotos.value = [];
    
    const mapElement = document.getElementById('map');
    if (mapElement) {
      mapElement.style.cursor = '';
    }
    
    await loadSignals();
    addMarkersToMap();
    
  } catch (e: any) {
    console.error('❌ Erreur:', e);
    error.value = e.message || 'Erreur lors de la création du signalement';
  } finally {
    submitting.value = false;
  }
};

// Fonction de test (à supprimer après)
const testNotification = async () => {
  try {
    const permStatus = await LocalNotifications.requestPermissions();
    console.log('📱 Permission:', permStatus.display);
    
    if (permStatus.display === 'granted') {
      // Créer le channel sur Android
      if (Capacitor.getPlatform() === 'android') {
        await LocalNotifications.createChannel({
          id: 'test_channel',
          name: 'Test Notifications',
          description: 'Channel pour tester les notifications',
          importance: 5,
          vibration: true,
          sound: 'default'
        });
      }
      
      const notifId = Math.floor(Math.random() * 2147483647);
      
      await LocalNotifications.schedule({
        notifications: [
          {
            id: notifId,
            title: '🔔 Test Notification',
            body: 'Les notifications fonctionnent correctement !',
            largeBody: 'Ceci est un test de notification.\n\nSi vous voyez cette notification, le système fonctionne parfaitement.',
            channelId: 'test_channel',
            schedule: { at: new Date(Date.now() + 1000) },
            sound: 'default',
            smallIcon: 'ic_launcher_foreground',
            autoCancel: true
          }
        ]
      });
      successMessage.value = 'Notification de test envoyée ! Regardez votre barre de notifications.';
    } else {
      error.value = 'Permission de notification refusée. Activez-la dans les paramètres.';
    }
  } catch (e: any) {
    console.error('Erreur test notification:', e);
    error.value = e.message;
  }
};

onMounted(async () => {
  setupGlobalPhotoHandler();
  
  // Charger le prix forfaitaire
  await loadPrixForfaitaire();
  
  // Initialiser les notifications
  await notificationService.initialize();
  await notificationService.startListeningToMyIssues();
  
  await loadSignals();
  
  setTimeout(() => {
    map = L.map('map', {
      zoomControl: true,
      attributionControl: true
    }).setView([-18.8792, 47.5079], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    map.on('click', handleMapClick);

    addMarkersToMap();

    setTimeout(() => {
      map?.invalidateSize();
    }, 100);
  }, 100);
});

onBeforeUnmount(() => {
  // Cleanup global handler
  delete (window as any).openSignalPhotos;
  
  // Arrêter les listeners de notifications
  notificationService.stopListening();
  
  if (map) {
    map.off('click', handleMapClick);
    map.remove();
    map = null;
  }
});
</script>

<style scoped>
/* ========================================
   DESIGN MODERNE - Signaleo
   ======================================== */

/* Variables CSS */
:root {
  --primary: #0f3460;
  --primary-dark: #1a1a2e;
  --secondary: #764ba2;
  --accent: #f093fb;
  --success: #48bb78;
  --warning: #ed8936;
  --danger: #f56565;
  --dark: #2d3748;
  --light: #f7fafc;
  --gray: #718096;
}

/* Map de base */
#map {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  height: 100%;
  width: 100%;
  z-index: 0;
}

ion-content {
  --padding-start: 0;
  --padding-end: 0;
  --padding-top: 0;
  --padding-bottom: 0;
}

/* ========================================
   HEADER MODERNE
   ======================================== */
.modern-header ion-toolbar {
  --background: linear-gradient(90deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  --color: white;
  padding: 8px 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.header-title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-logo {
  width: 42px;
  height: 42px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.app-logo ion-icon {
  font-size: 24px;
  color: white;
}

.header-titles .main-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.5px;
}

.header-titles .subtitle {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.action-btn ion-icon {
  font-size: 20px;
  color: white;
}

/* ========================================
   MINI STATS FLOTTANTES
   ======================================== */
.floating-stats {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
  z-index: 1000;
}

.mini-stat {
  background: white;
  border-radius: 12px;
  padding: 8px 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 60px;
}

.mini-stat .stat-number {
  font-size: 16px;
  font-weight: 700;
  color: #2d3748;
}

.mini-stat .stat-text {
  font-size: 10px;
  color: #718096;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.mini-stat.resolved .stat-number {
  color: #48bb78;
}

.mini-stat.progress .stat-number {
  color: #0f3460;
}

/* ========================================
   PANNEAU DE FILTRES MODERNE
   ======================================== */
.filters-panel {
  position: absolute;
  top: 12px;
  left: 12px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  min-width: 180px;
  max-width: 220px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.filters-panel.collapsed {
  max-width: 140px;
}

.filters-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  background: linear-gradient(90deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  cursor: pointer;
  color: white;
}

.filters-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.filters-title ion-icon {
  font-size: 18px;
}

.filters-title span {
  font-weight: 600;
  font-size: 14px;
}

.filters-badge {
  background: rgba(255, 255, 255, 0.3);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}

.toggle-icon {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.filters-body {
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.my-issues-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f7fafc;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 10px;
}

.my-issues-toggle:hover {
  background: #edf2f7;
}

.my-issues-toggle.active {
  background: linear-gradient(135deg, #1a1a2e20 0%, #0f346020 100%);
  border: 1px solid #0f346040;
}

.my-issues-toggle ion-icon {
  font-size: 18px;
  color: #0f3460;
}

.my-issues-toggle span {
  flex: 1;
  font-size: 12px;
  font-weight: 500;
  color: #4a5568;
}

.toggle-switch {
  width: 36px;
  height: 20px;
  background: #cbd5e0;
  border-radius: 10px;
  position: relative;
  transition: background 0.3s ease;
}

.my-issues-toggle.active .toggle-switch {
  background: #0f3460;
}

.toggle-dot {
  width: 16px;
  height: 16px;
  background: white;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: transform 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.my-issues-toggle.active .toggle-dot {
  transform: translateX(16px);
}

.filters-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #e2e8f0, transparent);
  margin: 12px 0;
}

.quick-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.quick-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px;
  border: none;
  background: #f7fafc;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 500;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-btn:hover {
  background: #edf2f7;
}

.quick-btn ion-icon {
  font-size: 14px;
}

.filter-types-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-type-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #f7fafc;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.filter-type-chip:hover {
  background: #edf2f7;
}

.filter-type-chip.active {
  background: white;
  border-color: var(--chip-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chip-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  background: var(--chip-color);
  opacity: 0.9;
}

.filter-type-chip:not(.active) .chip-icon {
  filter: grayscale(50%);
  opacity: 0.6;
}

.chip-label {
  flex: 1;
  font-size: 12px;
  font-weight: 500;
  color: #4a5568;
}

.filter-type-chip:not(.active) .chip-label {
  color: #a0aec0;
}

.chip-count {
  font-size: 11px;
  font-weight: 600;
  color: #718096;
  background: #e2e8f0;
  padding: 2px 6px;
  border-radius: 6px;
}

/* ========================================
   BOUTON FLOTTANT (FAB)
   ======================================== */
.fab-container {
  position: absolute;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.fab-button {
  width: 60px;
  height: 60px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(135deg, #1a1a2e 0%, #0f3460 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 8px 25px rgba(15, 52, 96, 0.5);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fab-button:hover {
  transform: scale(1.05);
  box-shadow: 0 12px 35px rgba(15, 52, 96, 0.6);
}

.fab-button.active {
  background: linear-gradient(135deg, #f56565 0%, #c53030 100%);
  box-shadow: 0 8px 25px rgba(245, 101, 101, 0.4);
  transform: rotate(45deg);
}

.fab-button ion-icon {
  font-size: 28px;
  transition: transform 0.3s ease;
}

.fab-button.active ion-icon {
  transform: rotate(-45deg);
}

.fab-label {
  font-size: 11px;
  font-weight: 600;
  color: #4a5568;
  background: white;
  padding: 4px 10px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* ========================================
   SÉLECTEUR DE TYPE MODERNE
   ======================================== */
.type-selector-panel {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-radius: 24px 24px 0 0;
  padding: 24px;
  box-shadow: 0 -8px 30px rgba(0, 0, 0, 0.15);
  z-index: 1001;
  max-height: 70vh;
  overflow-y: auto;
}

.selector-header {
  margin-bottom: 20px;
  text-align: center;
}

.selector-header h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 700;
  color: #2d3748;
}

.selector-header p {
  margin: 0;
  font-size: 14px;
  color: #718096;
}

.gps-button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px;
  border: 2px dashed #0f3460;
  background: linear-gradient(135deg, #1a1a2e10 0%, #0f346010 100%);
  border-radius: 14px;
  color: #0f3460;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.gps-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #1a1a2e20 0%, #0f346020 100%);
  transform: translateY(-2px);
}

.gps-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.gps-button ion-icon {
  font-size: 20px;
}

.type-options-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.type-option-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  background: #f7fafc;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
}

.type-option-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.type-option-card.selected {
  background: white;
  border-color: var(--card-color);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
}

.type-icon-wrapper {
  width: 50px;
  height: 50px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--card-color);
  transition: all 0.3s ease;
}

.type-emoji {
  font-size: 26px;
}

.type-name {
  font-size: 11px;
  font-weight: 600;
  color: #4a5568;
  text-align: center;
}

.selection-indicator {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--card-color);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transform: scale(0);
  transition: all 0.3s ease;
}

.type-option-card.selected .selection-indicator {
  opacity: 1;
  transform: scale(1);
}

.selection-indicator ion-icon {
  font-size: 14px;
  color: white;
}

.instruction-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px;
  background: linear-gradient(135deg, #48bb7820 0%, #38a16920 100%);
  border: 1px solid #48bb7840;
  border-radius: 12px;
  color: #276749;
  font-size: 13px;
  font-weight: 500;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.instruction-banner ion-icon {
  font-size: 20px;
}

/* Animation slide-up */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

/* ========================================
   MODAL DE STATISTIQUES
   ======================================== */

/* Fullscreen mode */
.stats-modal {
  --height: 85%;
  --width: 100%;
  --max-width: 500px;
  --border-radius: 24px 24px 0 0;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.stats-modal.stats-fullscreen {
  --width: 100% !important;
  --height: 100% !important;
  --max-width: 100% !important;
  --max-height: 100% !important;
  --border-radius: 0 !important;
}

.stats-modal.stats-fullscreen::part(content) {
  width: 100vw;
  max-width: 100vw;
  height: 100vh;
  max-height: 100vh;
}

.stats-header ion-toolbar {
  --background: linear-gradient(90deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  --color: white;
}

.stats-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
}

.stats-header-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stats-icon-badge {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.stats-icon-badge ion-icon {
  font-size: 24px;
  color: white;
}

.stats-header-info h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.3px;
}

.stats-header-info p {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.header-actions-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expand-modal-btn,
.close-modal-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(10px);
}

.expand-modal-btn:hover,
.close-modal-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.05);
}

.expand-modal-btn ion-icon,
.close-modal-btn ion-icon {
  font-size: 20px;
  color: white;
}

.stats-content {
  --background: #f7fafc;
  padding: 20px;
}

.stats-fullscreen .stats-content {
  padding: 32px 48px;
  max-width: 1400px;
  margin: 0 auto;
}

/* Hero Stats */
.hero-stats {
  margin-bottom: 24px;
}

.stats-fullscreen .hero-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: stretch;
}

.hero-stat-card {
  background: white;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.hero-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.hero-stat-card.main {
  display: flex;
  align-items: center;
  gap: 16px;
  background: linear-gradient(135deg, #1a1a2e 0%, #0f3460 100%);
  color: white;
  margin-bottom: 12px;
}

.stats-fullscreen .hero-stat-card.main {
  margin-bottom: 0;
  grid-column: 1;
  grid-row: span 2;
  flex-direction: column;
  justify-content: center;
  text-align: center;
}

.hero-stat-icon {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.stats-fullscreen .hero-stat-icon {
  width: 80px;
  height: 80px;
  border-radius: 24px;
}

.hero-stat-icon ion-icon {
  font-size: 32px;
}

.stats-fullscreen .hero-stat-icon ion-icon {
  font-size: 40px;
}

.hero-stat-value {
  font-size: 40px;
  font-weight: 800;
  display: block;
  letter-spacing: -1px;
}

.stats-fullscreen .hero-stat-value {
  font-size: 52px;
}

.hero-stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.stats-fullscreen .hero-stat-label {
  font-size: 16px;
}

.hero-stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stats-fullscreen .hero-stat-row {
  display: contents;
}

.hero-stat-card.small {
  text-align: center;
  padding: 18px 16px;
}

.stats-fullscreen .hero-stat-card.small {
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.small-stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #2d3748;
  display: block;
}

.stats-fullscreen .small-stat-value {
  font-size: 32px;
}

.hero-stat-card.small.accent .small-stat-value {
  color: #0f3460;
}

.small-stat-label {
  font-size: 12px;
  color: #718096;
  margin-top: 4px;
}

.stats-fullscreen .small-stat-label {
  font-size: 14px;
}

/* Progress Card */
.progress-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
  transition: all 0.3s ease;
}

.progress-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.stats-fullscreen .progress-card {
  padding: 32px;
  gap: 32px;
}

.circular-progress {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}

.stats-fullscreen .circular-progress {
  width: 130px;
  height: 130px;
}

.circular-progress svg {
  transform: rotate(-90deg);
  width: 100%;
  height: 100%;
}

.progress-bg {
  fill: none;
  stroke: #e2e8f0;
  stroke-width: 8;
}

.progress-fill {
  fill: none;
  stroke: url(#progressGradient);
  stroke: #0f3460;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dasharray 0.5s ease;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.progress-value {
  font-size: 22px;
  font-weight: 700;
  color: #2d3748;
  display: block;
}

.stats-fullscreen .progress-value {
  font-size: 28px;
}

.progress-label {
  font-size: 10px;
  color: #718096;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stats-fullscreen .progress-label {
  font-size: 12px;
}

.progress-details {
  flex: 1;
}

.progress-detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #4a5568;
}

.stats-fullscreen .progress-detail-item {
  font-size: 16px;
  margin-bottom: 14px;
}

.detail-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.stats-fullscreen .detail-dot {
  width: 14px;
  height: 14px;
}

.detail-dot.resolved {
  background: linear-gradient(135deg, #48bb78, #38a169);
}

.detail-dot.pending {
  background: linear-gradient(135deg, #ed8936, #dd6b20);
}

/* Section Headers */
.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.section-header ion-icon {
  font-size: 22px;
  color: #0f3460;
}

.section-header h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #2d3748;
}

.stats-fullscreen .section-header h3 {
  font-size: 20px;
}

/* Type Stats Grid */
.type-stats-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  margin-bottom: 24px;
}

.stats-fullscreen .type-stats-grid {
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.type-stat-card {
  background: white;
  border-radius: 18px;
  padding: 18px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
  border-left: 5px solid var(--type-color);
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-left: 5px solid var(--type-color);
}

.type-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.stats-fullscreen .type-stat-card {
  padding: 22px;
}

.type-stat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.type-stat-emoji {
  font-size: 28px;
}

.stats-fullscreen .type-stat-emoji {
  font-size: 32px;
}

.type-stat-name {
  font-size: 15px;
  font-weight: 600;
  color: #2d3748;
}

.stats-fullscreen .type-stat-name {
  font-size: 17px;
}

.type-stat-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 14px;
}

.type-stat-count {
  font-size: 30px;
  font-weight: 700;
  color: var(--type-color);
}

.stats-fullscreen .type-stat-count {
  font-size: 36px;
}

.type-stat-unit {
  font-size: 12px;
  color: #718096;
  margin-left: 4px;
}

.type-stat-details {
  text-align: right;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: #718096;
  margin-bottom: 4px;
}

.detail-row span:last-child {
  font-weight: 600;
  color: #4a5568;
}

.type-stat-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-track {
  flex: 1;
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-track .progress-fill {
  height: 100%;
  background: var(--type-color);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.type-stat-progress .progress-text {
  font-size: 12px;
  font-weight: 600;
  color: #718096;
  min-width: 70px;
  text-align: right;
}

/* Status Pills */
.status-pills {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stats-fullscreen .status-pills {
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.status-pill {
  background: white;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s ease;
}

.status-pill:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.stats-fullscreen .status-pill {
  padding: 20px 24px;
  border-radius: 16px;
}

.pill-content {
  display: flex;
  flex-direction: column;
}

.pill-count {
  font-size: 24px;
  font-weight: 700;
}

.stats-fullscreen .pill-count {
  font-size: 32px;
}

.pill-label {
  font-size: 12px;
  color: #718096;
}

.stats-fullscreen .pill-label {
  font-size: 14px;
}

.pill-percentage {
  font-size: 14px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 8px;
}

.stats-fullscreen .pill-percentage {
  font-size: 16px;
  padding: 6px 14px;
  border-radius: 10px;
}

.status-pill.status-1 .pill-count { color: #ed8936; }
.status-pill.status-1 .pill-percentage { background: #feebc8; color: #c05621; }

.status-pill.status-2 .pill-count { color: #4299e1; }
.status-pill.status-2 .pill-percentage { background: #bee3f8; color: #2b6cb0; }

.status-pill.status-3 .pill-count { color: #48bb78; }
.status-pill.status-3 .pill-percentage { background: #c6f6d5; color: #276749; }

.status-pill.status-4 .pill-count { color: #a0aec0; }
.status-pill.status-4 .pill-percentage { background: #e2e8f0; color: #4a5568; }

/* ========================================
   MODAL DE CRÉATION
   ======================================== */
.create-header ion-toolbar {
  --background: white;
  border-bottom: 1px solid #e2e8f0;
}

.create-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
}

.create-header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selected-type-badge {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.create-header-info h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #2d3748;
}

.location-text {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #718096;
}

.location-text ion-icon {
  font-size: 14px;
}

.create-content {
  --background: #f7fafc;
  padding: 20px;
}

/* Form Styles */
.form-section {
  background: white;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
}

.input-group {
  margin-bottom: 16px;
}

.input-group.half {
  flex: 1;
}

.input-row {
  display: flex;
  gap: 16px;
}

.input-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #4a5568;
  margin-bottom: 8px;
}

.input-label ion-icon {
  font-size: 16px;
  color: #0f3460;
}

.modern-input {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 15px;
  color: #2d3748;
  background: #f7fafc;
  transition: all 0.3s ease;
  outline: none;
}

.modern-input:focus {
  border-color: #0f3460;
  background: white;
  box-shadow: 0 0 0 4px rgba(15, 52, 96, 0.1);
}

.modern-input::placeholder {
  color: #a0aec0;
}

.modern-textarea {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 15px;
  color: #2d3748;
  background: #f7fafc;
  transition: all 0.3s ease;
  outline: none;
  resize: none;
  font-family: inherit;
}

.modern-textarea:focus {
  border-color: #0f3460;
  background: white;
  box-shadow: 0 0 0 4px rgba(15, 52, 96, 0.1);
}

/* Severity Selector */
.severity-selector {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
}

.severity-btn {
  flex: 1;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: #e2e8f0;
  color: #718096;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.severity-btn.active {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.severity-btn.low { background: #c6f6d5; color: #276749; }
.severity-btn.low.active { background: #48bb78; color: white; }

.severity-btn.medium { background: #feebc8; color: #c05621; }
.severity-btn.medium.active { background: #ed8936; color: white; }

.severity-btn.high { background: #fed7d7; color: #c53030; }
.severity-btn.high.active { background: #f56565; color: white; }

.severity-label {
  display: block;
  text-align: center;
  font-size: 11px;
  color: #718096;
  font-weight: 500;
}

/* Budget Card */
.budget-card {
  background: linear-gradient(135deg, #1a1a2e 0%, #0f3460 100%);
  border-radius: 20px;
  padding: 20px;
  color: white;
  margin-bottom: 16px;
}

.budget-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
  opacity: 0.9;
}

.budget-header ion-icon {
  font-size: 20px;
}

.budget-calculation {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.calc-item {
  text-align: center;
  background: rgba(255, 255, 255, 0.15);
  padding: 8px 12px;
  border-radius: 10px;
}

.calc-label {
  display: block;
  font-size: 10px;
  opacity: 0.8;
  margin-bottom: 2px;
}

.calc-value {
  font-size: 14px;
  font-weight: 600;
}

.calc-operator {
  font-size: 18px;
  font-weight: 300;
  opacity: 0.6;
}

.budget-total {
  text-align: center;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.total-label {
  display: block;
  font-size: 12px;
  opacity: 0.8;
  margin-bottom: 4px;
}

.total-value {
  font-size: 28px;
  font-weight: 800;
}

/* Photos Section */
.photos-section {
  background: white;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
}

.photos-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.photos-header ion-label {
  font-weight: 600;
  font-size: 14px;
  color: #2d3748;
}

.photo-actions {
  display: flex;
  gap: 8px;
}

.photo-actions ion-button {
  --padding-start: 12px;
  --padding-end: 12px;
  --border-radius: 10px;
  font-size: 12px;
}

.photos-preview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.photo-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-photo-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  --padding-start: 4px;
  --padding-end: 4px;
  margin: 0;
  background: white;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.no-photos-text {
  text-align: center;
  color: #a0aec0;
  font-size: 13px;
  padding: 30px;
  margin: 0;
  background: #f7fafc;
  border-radius: 12px;
  border: 2px dashed #e2e8f0;
}

/* Gallery Styles */
.photos-gallery {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.gallery-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  cursor: pointer;
  transition: transform 0.3s ease;
}

.gallery-item:hover {
  transform: scale(1.02);
}

.gallery-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.gallery-item:hover .photo-overlay {
  opacity: 1;
}

.photo-overlay ion-icon {
  font-size: 36px;
  color: white;
}

.no-photos-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #a0aec0;
}

.no-photos-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

/* Fullscreen Photo */
.fullscreen-photo-content {
  --background: #000;
}

.fullscreen-photo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 16px;
}

.fullscreen-photo-container img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
}
</style>

<style>
/* Styles globaux pour les popups Leaflet */
.custom-popup .leaflet-popup-content {
  margin: 14px;
  min-width: 280px;
}

.custom-popup .leaflet-popup-content-wrapper {
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
}

.custom-popup .leaflet-popup-tip {
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.custom-tooltip {
  background: white;
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
  padding: 12px 16px;
  font-size: 13px;
}

.custom-tooltip::before {
  border-top-color: white;
}

.issue-popup h3 {
  font-size: 16px;
  font-weight: 700;
}

.issue-popup p {
  color: #718096;
}

.custom-div-icon {
  background: transparent;
  border: none;
}
</style>