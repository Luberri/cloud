<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>Carte des signalements</ion-title>
        <ion-buttons slot="end">
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
            :key="stat.status_id"
            class="status-card"
            :class="'status-' + stat.status_id"
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
          <ion-label position="floating">Surface (m²)</ion-label>
          <ion-input v-model.number="newIssue.surface_m2" type="number" placeholder="Ex: 10"></ion-input>
        </ion-item>
        
        <ion-item>
          <ion-label position="floating">Budget estimé (MGA)</ion-label>
          <ion-input v-model.number="newIssue.budget" type="number" placeholder="Ex: 500000"></ion-input>
        </ion-item>
        
        <ion-item>
          <ion-label>Statut</ion-label>
          <ion-select v-model="newIssue.status_id" interface="popover">
            <ion-select-option :value="1">Signalé</ion-select-option>
            <ion-select-option :value="2">En cours</ion-select-option>
            <ion-select-option :value="3">Résolu</ion-select-option>
            <ion-select-option :value="4">Rejeté</ion-select-option>
          </ion-select>
        </ion-item>
        
        <ion-button 
          expand="block" 
          class="ion-margin-top" 
          @click="submitIssue" 
          :disabled="submitting || !newIssue.title || !newIssue.description"
        >
          <ion-spinner v-if="submitting" name="crescent"></ion-spinner>
          <span v-else>Créer le signalement</span>
        </ion-button>
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
  statsChartOutline
} from 'ionicons/icons';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { collection, getDocs, addDoc, GeoPoint, Timestamp } from 'firebase/firestore';
import { db, auth } from '@/config/firebase';

// Types de signalements avec icônes et couleurs
interface IssueType {
  id: number;
  label: string;
  icon: string;
  color: string;
}

const issueTypes: IssueType[] = [
  { id: 1, label: 'Danger', icon: warningOutline, color: '#dc3545' },
  { id: 2, label: 'Accident', icon: carOutline, color: '#9c27b0' },
  { id: 3, label: 'Travaux', icon: constructOutline, color: '#ff9800' },
  { id: 4, label: 'Inondation', icon: waterOutline, color: '#2196f3' },
  { id: 5, label: 'Nid de poule', icon: alertCircleOutline, color: '#c62828' },
  { id: 6, label: 'Résolu', icon: checkmarkCircleOutline, color: '#4caf50' },
  { id: 7, label: 'Électricité', icon: flashOutline, color: '#ffc107' },
  { id: 8, label: 'Déchets', icon: trashOutline, color: '#795548' },
  { id: 9, label: 'Végétation', icon: leafOutline, color: '#8bc34a' }
];

// Interface pour les road issues
interface RoadIssue {
  id: string;
  title: string;
  description: string;
  location: GeoPoint;
  surface_m2: number;
  budget: number;
  status_id: number;
  issue_type_id: number;
  company_id: number;
  reported_by: string;
  reported_at: Timestamp;
  updated_at: Timestamp;
  is_synced: boolean;
  firebase_id: string;
}

// Référence pour le conteneur de la carte
const mapContainer = ref(null);
let map: L.Map | null = null;
let tempMarker: L.Marker | null = null;
const loading = ref(false);
const error = ref('');
const successMessage = ref('');
const roadIssues = ref<RoadIssue[]>([]);

// État pour le mode signalement
const isSignalMode = ref(false);
const showModal = ref(false);
const showStatsModal = ref(false);
const submitting = ref(false);
const selectedLocation = ref<{ lat: number; lng: number } | null>(null);
const selectedIssueType = ref<IssueType | null>(null);
const legendCollapsed = ref(false);

// Filtres
const selectedFilterTypes = ref<number[]>(issueTypes.map(t => t.id));
const showMyIssuesOnly = ref(false);
let allMarkers: L.Marker[] = [];

// Formulaire pour nouveau signalement
const newIssue = reactive({
  title: '',
  description: '',
  surface_m2: 0,
  budget: 0,
  status_id: 1
});

// Statistiques globales
const globalStats = computed(() => {
  const issues = roadIssues.value;
  const totalIssues = issues.length;
  const totalSurface = issues.reduce((sum, i) => sum + (i.surface_m2 || 0), 0);
  const totalBudget = issues.reduce((sum, i) => sum + (i.budget || 0), 0);
  const resolvedIssues = issues.filter(i => i.status_id === 3).length;
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
    const issuesOfType = roadIssues.value.filter(i => (i.issue_type_id || 1) === type.id);
    const count = issuesOfType.length;
    const totalSurface = issuesOfType.reduce((sum, i) => sum + (i.surface_m2 || 0), 0);
    const totalBudget = issuesOfType.reduce((sum, i) => sum + (i.budget || 0), 0);
    const resolvedCount = issuesOfType.filter(i => i.status_id === 3).length;
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
    { status_id: 1, label: 'Signalé', color: '#ff9800' },
    { status_id: 2, label: 'En cours', color: '#2196f3' },
    { status_id: 3, label: 'Résolu', color: '#4caf50' },
    { status_id: 4, label: 'Rejeté', color: '#9e9e9e' }
  ];
  
  const total = roadIssues.value.length;
  
  return statuses.map(status => {
    const count = roadIssues.value.filter(i => i.status_id === status.status_id).length;
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

// Créer le contenu du popup
const createPopupContent = (issue: RoadIssue): string => {
  const type = issueTypes.find(t => t.id === issue.issue_type_id) || issueTypes[0];
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
      </div>
      <h3 style="margin: 0 0 8px 0; color: #333;">${issue.title}</h3>
      <p style="margin: 0 0 8px 0; font-size: 13px; color: #666;">${issue.description}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 8px 0;">
      <table style="font-size: 12px; width: 100%;">
        <tr>
          <td><strong>Surface:</strong></td>
          <td>${issue.surface_m2} m²</td>
        </tr>
        <tr>
          <td><strong>Budget:</strong></td>
          <td>${formatBudget(issue.budget)}</td>
        </tr>
        <tr>
          <td><strong>Statut:</strong></td>
          <td><span style="padding: 2px 6px; background: #ffeb3b; border-radius: 4px; font-size: 11px;">${getStatusText(issue.status_id)}</span></td>
        </tr>
        <tr>
          <td><strong>Signalé le:</strong></td>
          <td>${formatDate(issue.reported_at)}</td>
        </tr>
      </table>
    </div>
  `;
};

// Créer le contenu du tooltip (survol)
const createTooltipContent = (issue: RoadIssue): string => {
  const type = issueTypes.find(t => t.id === issue.issue_type_id) || issueTypes[0];
  return `
    <strong>${issue.title}</strong><br>
    <small style="color: ${type.color};">${type.label}</small><br>
    <small>Statut: ${getStatusText(issue.status_id)}</small>
  `;
};

// Charger les road issues depuis Firestore
const loadRoadIssues = async () => {
  loading.value = true;
  try {
    const querySnapshot = await getDocs(collection(db, 'road_issues'));
    roadIssues.value = querySnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    })) as RoadIssue[];
    
    console.log(`${roadIssues.value.length} road issues chargés`);
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
  
  roadIssues.value.forEach(issue => {
    if (issue.location && issue.location.latitude && issue.location.longitude) {
      // Appliquer les filtres
      const typeId = issue.issue_type_id || 1;
      
      // Filtre par type
      if (!selectedFilterTypes.value.includes(typeId)) return;
      
      // Filtre mes signalements
      if (showMyIssuesOnly.value && issue.reported_by !== currentUserId) return;
      
      const icon = getIconForIssueType(typeId);
      
      const marker = L.marker(
        [issue.location.latitude, issue.location.longitude],
        { icon }
      ).addTo(map!);
      
      marker.bindPopup(createPopupContent(issue), {
        maxWidth: 300,
        className: 'custom-popup'
      });
      
      marker.bindTooltip(createTooltipContent(issue), {
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
  
  // Changer le curseur de la carte
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
  return roadIssues.value.filter(issue => {
    const matchType = (issue.issue_type_id || 1) === typeId;
    const matchUser = !showMyIssuesOnly.value || issue.reported_by === currentUserId;
    return matchType && matchUser;
  }).length;
};

// Activer/désactiver le mode signalement
const toggleSignalMode = () => {
  isSignalMode.value = !isSignalMode.value;
  
  if (!isSignalMode.value) {
    // Retirer le marqueur temporaire si on annule
    if (tempMarker && map) {
      map.removeLayer(tempMarker);
      tempMarker = null;
    }
    selectedLocation.value = null;
    selectedIssueType.value = null;
    
    // Réinitialiser le curseur
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
  
  // Retirer l'ancien marqueur temporaire
  if (tempMarker && map) {
    map.removeLayer(tempMarker);
  }
  
  // Ajouter un nouveau marqueur temporaire avec l'icône sélectionnée
  const icon = getIconForIssueType(selectedIssueType.value.id);
  tempMarker = L.marker([lat, lng], { icon }).addTo(map!);
  tempMarker.bindPopup('Nouvel emplacement sélectionné').openPopup();
  
  // Ouvrir le modal de création
  showModal.value = true;
};

// Fermer le modal
const closeModal = () => {
  showModal.value = false;
  // Réinitialiser le formulaire
  newIssue.title = '';
  newIssue.description = '';
  newIssue.surface_m2 = 0;
  newIssue.budget = 0;
  newIssue.status_id = 1;
};

// Soumettre le signalement
const submitIssue = async () => {
  if (!selectedLocation.value || !newIssue.title || !newIssue.description || !selectedIssueType.value) {
    error.value = 'Veuillez remplir tous les champs obligatoires';
    return;
  }
  
  submitting.value = true;
  
  try {
    const currentUser = auth.currentUser;
    const reportedBy = currentUser?.uid || 'anonymous';
    
    const docRef = await addDoc(collection(db, 'road_issues'), {
      title: newIssue.title,
      description: newIssue.description,
      location: new GeoPoint(selectedLocation.value.lat, selectedLocation.value.lng),
      surface_m2: newIssue.surface_m2 || 0,
      budget: newIssue.budget || 0,
      status_id: newIssue.status_id,
      issue_type_id: selectedIssueType.value.id,
      company_id: 1,
      reported_by: reportedBy,
      reported_at: Timestamp.now(),
      updated_at: Timestamp.now(),
      is_synced: false,
      firebase_id: ''
    });
    
    console.log('Signalement créé avec ID:', docRef.id);
    successMessage.value = 'Signalement créé avec succès !';
    
    // Fermer le modal et réinitialiser
    closeModal();
    isSignalMode.value = false;
    selectedIssueType.value = null;
    
    // Retirer le marqueur temporaire
    if (tempMarker && map) {
      map.removeLayer(tempMarker);
      tempMarker = null;
    }
    
    // Changer le curseur
    const mapElement = document.getElementById('map');
    if (mapElement) {
      mapElement.style.cursor = '';
    }
    
    // Recharger les signalements
    await loadRoadIssues();
    
    // Réajouter les marqueurs avec les filtres
    addMarkersToMap();
    
  } catch (e: any) {
    console.error('Erreur lors de la création:', e);
    error.value = e.message || 'Erreur lors de la création du signalement';
  } finally {
    submitting.value = false;
  }
};

onMounted(async () => {
  await loadRoadIssues();
  
  setTimeout(() => {
    map = L.map('map', {
      zoomControl: true,
      attributionControl: true
    }).setView([-18.8792, 47.5079], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    // Ajouter l'écouteur de clic sur la carte
    map.on('click', handleMapClick);

    addMarkersToMap();

    setTimeout(() => {
      map?.invalidateSize();
    }, 100);
  }, 100);
});

onBeforeUnmount(() => {
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

/* Styles pour le modal de statistiques */
.stats-summary {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  color: white;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  opacity: 0.9;
}

.progress-section {
  background: #f5f5f5;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.progress-section h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #333;
}

.progress-bar-container {
  background: #e0e0e0;
  border-radius: 10px;
  height: 20px;
  overflow: hidden;
}

.progress-bar {
  background: linear-gradient(90deg, #4caf50, #8bc34a);
  height: 100%;
  border-radius: 10px;
  transition: width 0.5s ease;
}

.progress-details {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  text-align: center;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 20px 0 12px 0;
}

.stats-table-container {
  overflow-x: auto;
  margin-bottom: 20px;
}

.stats-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.stats-table th {
  background: #f5f5f5;
  padding: 10px 8px;
  text-align: left;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #e0e0e0;
}

.stats-table td {
  padding: 10px 8px;
  border-bottom: 1px solid #eee;
}

.stats-table .center {
  text-align: center;
}

.stats-table .right {
  text-align: right;
}

.stats-table .total-row {
  background: #f5f5f5;
}

.stats-table .total-row td {
  border-bottom: none;
  border-top: 2px solid #e0e0e0;
}

.type-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-icon {
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

.mini-progress {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mini-progress-bar {
  flex: 1;
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  position: relative;
  overflow: hidden;
}

.mini-progress-bar::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 100%;
  background: #4caf50;
  border-radius: 3px;
}

.mini-progress span {
  font-size: 11px;
  color: #666;
  min-width: 35px;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.status-card {
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  color: white;
}

.status-card.status-1 {
  background: linear-gradient(135deg, #ff9800, #f57c00);
}

.status-card.status-2 {
  background: linear-gradient(135deg, #2196f3, #1976d2);
}

.status-card.status-3 {
  background: linear-gradient(135deg, #4caf50, #388e3c);
}

.status-card.status-4 {
  background: linear-gradient(135deg, #9e9e9e, #757575);
}

.status-count {
  font-size: 28px;
  font-weight: 700;
}

.status-label {
  font-size: 13px;
  margin: 4px 0;
}

.status-percentage {
  font-size: 12px;
  opacity: 0.9;
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
</style>

<style>
.custom-popup .leaflet-popup-content {
  margin: 12px;
}

.custom-popup .leaflet-popup-content-wrapper {
  border-radius: 8px;
  box-shadow: 0 3px 14px rgba(0, 0, 0, 0.2);
}

.custom-tooltip {
  background: white;
  border: none;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  padding: 8px 12px;
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