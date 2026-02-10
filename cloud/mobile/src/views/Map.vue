<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>Carte des signalements</ion-title>
        <ion-buttons slot="end">
          <!-- Bouton de test (à supprimer après) -->
          <ion-button @click="testNotification" color="warning">
            Test Notif
          </ion-button>
          <ion-button @click="showStatsModal = true" color="secondary">
            <ion-icon :icon="statsChartOutline" slot="start"></ion-icon>
            Stats
          </ion-button>
          <ion-button @click="toggleSignalMode" :color="isSignalMode ? 'danger' : 'primary'">
            <ion-icon :icon="isSignalMode ? closeOutline : addOutline" slot="start"></ion-icon>
            {{ isSignalMode ? 'Annuler' : 'Signaler' }}
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content :fullscreen="true">
      <div id="map" ref="mapContainer"></div>
      
      <!-- Légende des types de signalement -->
      <div class="map-legend" :class="{ collapsed: legendCollapsed }">
        <div class="legend-header" @click="legendCollapsed = !legendCollapsed">
          <span>Filtres</span>
          <ion-icon :icon="legendCollapsed ? chevronDownOutline : chevronUpOutline"></ion-icon>
        </div>
        <div class="legend-content" v-show="!legendCollapsed">
          <!-- Filtre mes signalements -->
          <div class="filter-section">
            <div class="filter-toggle" @click="toggleMyIssuesOnly">
              <ion-icon :icon="showMyIssuesOnly ? checkboxOutline : squareOutline"></ion-icon>
              <span>Mes signalements</span>
            </div>
          </div>
          
          <div class="filter-divider"></div>
          
          <!-- Boutons tout sélectionner / désélectionner -->
          <div class="filter-actions">
            <ion-button size="small" fill="clear" @click="selectAllTypes">Tout</ion-button>
            <ion-button size="small" fill="clear" @click="deselectAllTypes">Aucun</ion-button>
          </div>
          
          <!-- Types de signalement -->
          <div 
            v-for="type in issueTypes" 
            :key="type.id"
            class="legend-item"
            :class="{ inactive: !selectedFilterTypes.includes(type.id) }"
            @click="toggleFilterType(type.id)"
          >
            <ion-icon 
              :icon="selectedFilterTypes.includes(type.id) ? checkboxOutline : squareOutline" 
              class="filter-checkbox"
            ></ion-icon>
            <div class="legend-icon" :style="{ backgroundColor: type.color }">
              <ion-icon :icon="type.icon"></ion-icon>
            </div>
            <span class="legend-label">{{ type.label }}</span>
            <span class="legend-count">({{ getCountForType(type.id) }})</span>
          </div>
        </div>
      </div>
      
      <!-- Sélecteur de type de signalement -->
      <div v-if="isSignalMode && !showModal" class="marker-selector">
        <p class="selector-title">Choisissez le type de problème :</p>
        
        <!-- Bouton utiliser ma position -->
        <div class="location-section">
          <ion-button 
            expand="block" 
            fill="outline" 
            size="small" 
            @click="useMyLocation" 
            :disabled="gettingLocation"
          >
            <ion-spinner v-if="gettingLocation" name="crescent" slot="start"></ion-spinner>
            <ion-icon v-else :icon="navigateOutline" slot="start"></ion-icon>
            {{ gettingLocation ? 'Localisation...' : 'Utiliser ma position' }}
          </ion-button>
        </div>
        
        <div class="marker-options">
          <div 
            v-for="type in issueTypes" 
            :key="type.id"
            class="marker-option"
            :class="{ selected: selectedIssueType?.id === type.id }"
            @click="selectIssueType(type)"
          >
            <div class="marker-icon" :style="{ backgroundColor: type.color }">
              <ion-icon :icon="type.icon"></ion-icon>
            </div>
            <span class="marker-label">{{ type.label }}</span>
          </div>
        </div>
        <p v-if="selectedIssueType" class="instruction-text">
          <ion-icon :icon="locationOutline"></ion-icon>
          Cliquez sur la carte pour placer le marqueur
        </p>
      </div>
      
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
    
    <!-- Modal de statistiques -->
    <ion-modal :is-open="showStatsModal" @didDismiss="showStatsModal = false">
      <ion-header>
        <ion-toolbar>
          <ion-title>Tableau récapitulatif</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="showStatsModal = false">
              <ion-icon :icon="closeOutline"></ion-icon>
            </ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>
      
      <ion-content class="ion-padding">
        <!-- Résumé global -->
        <div class="stats-summary">
          <div class="stat-card">
            <div class="stat-value">{{ globalStats.totalIssues }}</div>
            <div class="stat-label">Signalements</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ globalStats.totalSurface.toFixed(1) }} m²</div>
            <div class="stat-label">Surface totale</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ globalStats.progressPercentage.toFixed(1) }}%</div>
            <div class="stat-label">Avancement</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ formatBudgetShort(globalStats.totalBudget) }}</div>
            <div class="stat-label">Budget total</div>
          </div>
        </div>
        
        <!-- Barre de progression -->
        <div class="progress-section">
          <h3>Avancement global</h3>
          <div class="progress-bar-container">
            <div class="progress-bar" :style="{ width: globalStats.progressPercentage + '%' }"></div>
          </div>
          <div class="progress-details">
            <span>{{ globalStats.resolvedIssues }} résolus sur {{ globalStats.totalIssues }}</span>
          </div>
        </div>
        
        <!-- Tableau par type -->
        <h3 class="section-title">Détails par type</h3>
        <div class="stats-table-container">
          <table class="stats-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Nb</th>
                <th>Surface (m²)</th>
                <th>Avancement</th>
                <th>Budget (MGA)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="stat in statsByType" :key="stat.type.id">
                <td>
                  <div class="type-cell">
                    <div class="type-icon" :style="{ backgroundColor: stat.type.color }">
                      <ion-icon :icon="stat.type.icon"></ion-icon>
                    </div>
                    <span>{{ stat.type.label }}</span>
                  </div>
                </td>
                <td class="center">{{ stat.count }}</td>
                <td class="center">{{ stat.totalSurface.toFixed(1) }}</td>
                <td class="center">
                  <div class="mini-progress">
                    <div class="mini-progress-bar" :style="{ width: stat.progressPercentage + '%' }"></div>
                    <span>{{ stat.progressPercentage.toFixed(0) }}%</span>
                  </div>
                </td>
                <td class="right">{{ formatBudgetShort(stat.totalBudget) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr class="total-row">
                <td><strong>TOTAL</strong></td>
                <td class="center"><strong>{{ globalStats.totalIssues }}</strong></td>
                <td class="center"><strong>{{ globalStats.totalSurface.toFixed(1) }}</strong></td>
                <td class="center"><strong>{{ globalStats.progressPercentage.toFixed(1) }}%</strong></td>
                <td class="right"><strong>{{ formatBudgetShort(globalStats.totalBudget) }}</strong></td>
              </tr>
            </tfoot>
          </table>
        </div>
        
        <!-- Tableau par statut -->
        <h3 class="section-title">Répartition par statut</h3>
        <div class="status-cards">
          <div 
            v-for="stat in statsByStatus" 
            :key="stat.statusId"
            class="status-card"
            :class="'status-' + stat.statusId"
          >
            <div class="status-count">{{ stat.count }}</div>
            <div class="status-label">{{ stat.label }}</div>
            <div class="status-percentage">{{ stat.percentage.toFixed(1) }}%</div>
          </div>
        </div>
      </ion-content>
    </ion-modal>
    
    <!-- Modal de création de signalement -->
    <ion-modal :is-open="showModal" @didDismiss="closeModal">
      <ion-header>
        <ion-toolbar>
          <ion-title>Nouveau signalement</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="closeModal">
              <ion-icon :icon="closeOutline"></ion-icon>
            </ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>
      
      <ion-content class="ion-padding">
        <!-- Type sélectionné -->
        <div class="selected-type" v-if="selectedIssueType">
          <div class="type-badge" :style="{ backgroundColor: selectedIssueType.color }">
            <ion-icon :icon="selectedIssueType.icon"></ion-icon>
          </div>
          <span>{{ selectedIssueType.label }}</span>
        </div>
        
        <div class="selected-location">
          <ion-icon :icon="locationOutline"></ion-icon>
          <span>Position: {{ selectedLocation?.lat.toFixed(6) }}, {{ selectedLocation?.lng.toFixed(6) }}</span>
        </div>
        
        <ion-item>
          <ion-label position="floating">Titre *</ion-label>
          <ion-input v-model="newIssue.title" type="text" placeholder="Ex: Nid de poule"></ion-input>
        </ion-item>
        
        <ion-item>
          <ion-label position="floating">Description *</ion-label>
          <ion-textarea 
            v-model="newIssue.description" 
            rows="4" 
            placeholder="Décrivez le problème..."
          ></ion-textarea>
        </ion-item>
        
        <ion-item>
          <ion-label position="floating">Surface (m²) *</ion-label>
          <ion-input v-model.number="newIssue.surface" type="number" placeholder="Ex: 10" @ionInput="calculateBudget"></ion-input>
        </ion-item>
        
        <!-- Nouveau champ: Niveau de gravité -->
        <ion-item>
          <ion-label>Niveau de gravité *</ion-label>
          <ion-select v-model="newIssue.niveau" interface="popover" @ionChange="calculateBudget">
            <ion-select-option v-for="n in 10" :key="n" :value="n">
              {{ n }} - {{ getNiveauLabel(n) }}
            </ion-select-option>
          </ion-select>
        </ion-item>
        
        <!-- Affichage du prix forfaitaire -->
        <div class="prix-info" v-if="prixForfaitaire">
          <ion-icon :icon="informationCircleOutline"></ion-icon>
          <span>Prix forfaitaire: {{ formatBudget(prixForfaitaire) }} / m²</span>
        </div>
        
        <!-- Affichage du budget estimé -->
        <div class="budget-estimate" v-if="newIssue.surface > 0 && newIssue.niveau > 0">
          <div class="budget-label">
            <ion-icon :icon="calculatorOutline"></ion-icon>
            <span>Budget estimé</span>
          </div>
          <div class="budget-formula">
            {{ formatBudget(prixForfaitaire) }} × {{ newIssue.niveau }} × {{ newIssue.surface }} m²
          </div>
          <div class="budget-value">
            {{ formatBudget(calculatedBudget) }}
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
  closeCircleOutline, expandOutline, informationCircleOutline, calculatorOutline
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

/* ...existing code... */

/* Styles pour le prix et le budget estimé */
.prix-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #e3f2fd;
  border-radius: 8px;
  margin: 12px 0;
  font-size: 13px;
  color: #1565c0;
}

.prix-info ion-icon {
  font-size: 18px;
}

.budget-estimate {
  background: linear-gradient(135deg, #4caf50, #2e7d32);
  border-radius: 12px;
  padding: 16px;
  margin: 16px 0;
  color: white;
  text-align: center;
}

.budget-label {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.budget-label ion-icon {
  font-size: 18px;
}

.budget-formula {
  font-size: 12px;
  opacity: 0.8;
  margin-bottom: 8px;
  font-family: monospace;
}

.budget-value {
  font-size: 24px;
  font-weight: 700;
}

/* Styles existants pour la légende et le sélecteur */
.map-legend {
  position: absolute;
  top: 10px;
  left: 10px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 140px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.legend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f5f5f5;
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
  color: #333;
}

.legend-header ion-icon {
  font-size: 16px;
  color: #666;
}

.legend-content {
  padding: 8px;
  max-height: 350px;
  overflow-y: auto;
}

.filter-section {
  padding: 4px 8px;
  margin-bottom: 4px;
}

.filter-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #1976d2;
  background: #e3f2fd;
  transition: background 0.2s;
}

.filter-toggle:hover {
  background: #bbdefb;
}

.filter-toggle ion-icon {
  font-size: 18px;
}

.filter-divider {
  height: 1px;
  background: #e0e0e0;
  margin: 8px;
}

.filter-actions {
  display: flex;
  justify-content: space-between;
  padding: 0 4px;
  margin-bottom: 4px;
}

.filter-actions ion-button {
  font-size: 11px;
  --padding-start: 8px;
  --padding-end: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  transition: all 0.2s;
  cursor: pointer;
}

.legend-item:hover {
  background: #f5f5f5;
}

.legend-item.inactive {
  opacity: 0.5;
}

.legend-item.inactive .legend-icon {
  filter: grayscale(100%);
}

.filter-checkbox {
  font-size: 16px;
  color: #1976d2;
  flex-shrink: 0;
}

.legend-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  flex-shrink: 0;
}

.legend-label {
  font-size: 12px;
  color: #333;
  flex: 1;
}

.legend-count {
  font-size: 11px;
  color: #999;
}

.marker-selector {
  position: absolute;
  bottom: 20px;
  left: 10px;
  right: 10px;
  background: white;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
}

.selector-title {
  margin: 0 0 12px 0;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.marker-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.marker-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.marker-option:hover {
  background: #f5f5f5;
}

.marker-option.selected {
  background: #e3f2fd;
  border-color: #2196f3;
}

.marker-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.marker-label {
  font-size: 11px;
  color: #666;
  text-align: center;
  font-weight: 500;
}

.instruction-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 12px 0 0 0;
  padding: 10px;
  background: #e8f5e9;
  border-radius: 8px;
  color: #2e7d32;
  font-size: 13px;
}

.selected-type {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
  margin-bottom: 12px;
}

.type-badge {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.selected-location {
  background: #e3f2fd;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  
  color: #1976d2;
}

.selected-location ion-icon {
  font-size: 20px;
}

/* Section localisation */
.location-section {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e0e0e0;
}

.location-section ion-button {
  --background: #e8f5e9;
  --color: #2e7d32;
  --border-color: #4caf50;
}

/* Section Photos */
.photos-section {
  margin: 16px 0;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 12px;
}

.photos-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.photos-header ion-label {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.photo-actions {
  display: flex;
  gap: 4px;
}

.photo-actions ion-button {
  --padding-start: 8px;
  --padding-end: 8px;
  font-size: 12px;
}

.photos-preview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.photo-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-photo-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  --padding-start: 4px;
  --padding-end: 4px;
  --padding-top: 4px;
  --padding-bottom: 4px;
  margin: 0;
  background: white;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.remove-photo-btn ion-icon {
  font-size: 20px;
}

.no-photos-text {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 20px;
  margin: 0;
}

/* Styles pour la galerie de photos */
.photos-gallery {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.gallery-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: transform 0.2s ease;
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
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.gallery-item:hover .photo-overlay {
  opacity: 1;
}

.photo-overlay ion-icon {
  font-size: 32px;
  color: white;
}

.no-photos-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.no-photos-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.no-photos-container p {
  font-size: 14px;
  margin: 0;
}

/* Photo plein écran */
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
.custom-popup .leaflet-popup-content {
  margin: 12px;
  min-width: 250px;
}

.custom-popup .leaflet-popup-content-wrapper {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.custom-tooltip {
  background: white;
  border: none;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 10px 14px;
  font-size: 12px;
}

.custom-tooltip::before {
  border-top-color: white;
}

.issue-popup h3 {
  font-size: 16px;
  font-weight: 600;
}

.issue-popup p {
  color: #666;
}

.custom-div-icon {
  background: transparent;
  border: none;
}

</style>