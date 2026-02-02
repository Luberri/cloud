<template>
  <div class="map-container">
    <!-- Top Bar -->
    <div class="top-bar">
      <h2>Carte des signalements</h2>
      <button class="btn-signaler" @click="showSignalTypeSelector">
        + SIGNALER
      </button>
    </div>

    <!-- Floating Sidebar -->
    <div class="sidebar">
      <div class="sidebar-content">
        <div class="tab-header">
          <button 
            :class="['tab', { active: activeTab === 'mes' }]"
            @click="activeTab = 'mes'"
          >
            Mes signalements
          </button>
          <button 
            :class="['tab', { active: activeTab === 'tous' }]"
            @click="activeTab = 'tous'"
          >
            TOUT
          </button>
          <button 
            :class="['tab', { active: activeTab === 'aucun' }]"
            @click="activeTab = 'aucun'"
          >
            AUCUN
          </button>
        </div>

        <div class="signals-list">
          <div
            v-for="signal in signals"
            :key="signal.id"
            class="signal-item"
            @click="toggleSignal(signal.id)"
          >
            <input
              type="checkbox"
              :checked="signal.visible"
              @change.stop="toggleSignal(signal.id)"
            >
            <div class="signal-icon" :style="{ backgroundColor: signal.color }">
              <!-- FIX: icon est un emoji, pas une class -->
              {{ signal.icon }}
            </div>
            <span class="signal-label">{{ signal.label }}</span>
            <span class="signal-count">({{ signal.count }})</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Map -->
    <div id="map" ref="mapContainer"></div>

    <!-- Signal Type Selector Bottom Sheet -->
    <transition name="slide-up">
      <div v-if="showTypeSelector" class="bottom-sheet-overlay" @click="closeTypeSelector">
        <div class="bottom-sheet" @click.stop>
          <div class="bottom-sheet-header">
            <h3>Choisissez le type de problème :</h3>
            <button class="btn-close" @click="closeTypeSelector">✕ ANNULER</button>
          </div>
          <div class="type-grid">
            <div 
              v-for="signal in signals.filter(s => s.id !== 6)" 
              :key="signal.id"
              class="type-card"
              @click="selectSignalType(signal)"
            >
              <div class="type-icon" :style="{ backgroundColor: signal.color }">
                {{ signal.icon }}
              </div>
              <span class="type-label">{{ signal.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- Bottom Navigation -->
    <div class="bottom-nav">
      <button class="nav-item active">
        <i class="icon-map"></i>
        <span>Carte</span>
      </button>
      <button class="nav-item">
        <i class="icon-test"></i>
        <span>Test</span>
      </button>
      <button class="nav-item">
        <i class="icon-disconnect"></i>
        <span>Déconnexion</span>
      </button>
    </div>
  </div>
</template>

<script>
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

import { db } from '../firebase'
import { collection, onSnapshot, query, orderBy } from 'firebase/firestore'

export default {
  name: 'Map',
  data() {
    return {
      map: null,
      markersLayer: null,

      activeTab: 'tous',
      showTypeSelector: false,

      // Firestore
      unsubscribeSignals: null,
      firestoreSignals: [],

      // UI types
      signals: [
        { id: 1, label: 'Danger', count: 0, color: '#E53935', icon: '⚠️', visible: true },
        { id: 2, label: 'Accident', count: 0, color: '#8E24AA', icon: '🚗', visible: true },
        { id: 3, label: 'Travaux', count: 0, color: '#FB8C00', icon: '🚧', visible: true },
        { id: 4, label: 'Inondation', count: 0, color: '#039BE5', icon: '💧', visible: true },
        { id: 5, label: 'Nid de poule', count: 0, color: '#C62828', icon: '🕳️', visible: true },
        { id: 6, label: 'Résolu', count: 0, color: '#43A047', icon: '✓', visible: true },
        { id: 7, label: 'Électricité', count: 0, color: '#FDD835', icon: '⚡', visible: true }
      ]
    }
  },
  mounted() {
    this.initMap()
    this.listenSignalsFromFirestore()
  },
  beforeUnmount() {
    if (this.unsubscribeSignals) this.unsubscribeSignals()
    if (this.map) this.map.remove()
  },
  methods: {
    initMap() {
      this.map = L.map(this.$refs.mapContainer).setView([-18.9100, 47.5300], 13)

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 19
      }).addTo(this.map)

      this.markersLayer = L.layerGroup().addTo(this.map)
    },

    listenSignalsFromFirestore() {
      // Si createdAt n’existe pas encore sur certains docs, Firestore les mettra quand même dans la liste.
      const q = query(collection(db, 'signals'), orderBy('createdAt', 'desc'))

      this.unsubscribeSignals = onSnapshot(
        q,
        (snap) => {
          this.firestoreSignals = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
          this.recomputeCounts()
          this.renderMarkers()
        },
        (err) => {
          console.error('Firestore onSnapshot error:', err)
        }
      )
    },

    recomputeCounts() {
      const counts = new Map()
      for (const s of this.firestoreSignals) {
        const typeId = Number(s.typeId)
        if (!Number.isFinite(typeId)) continue
        counts.set(typeId, (counts.get(typeId) || 0) + 1)
      }

      this.signals.forEach((t) => {
        t.count = counts.get(t.id) || 0
      })
    },

    formatStatus(status) {
      const s = (status || '').toString().toLowerCase()
      if (s === 'signale' || s === 'signalé') return 'Signalé'
      if (s === 'en_cours' || s === 'encours') return 'En cours'
      if (s === 'resolu' || s === 'résolu') return 'Résolu'
      return status ? status.toString() : 'Signalé'
    },

    renderMarkers() {
      if (!this.markersLayer) return
      this.markersLayer.clearLayers()

      const visibleTypeIds = new Set(this.signals.filter((t) => t.visible).map((t) => t.id))

      for (const s of this.firestoreSignals) {
        const lat = Number(s.latitude)
        const lng = Number(s.longitude)
        const typeId = Number(s.typeId)
        if (!Number.isFinite(lat) || !Number.isFinite(lng) || !Number.isFinite(typeId)) continue
        if (!visibleTypeIds.has(typeId)) continue

        const type = this.signals.find((t) => t.id === typeId) || {
          label: s.type || 'Signalement',
          color: s.color || '#2196F3',
          icon: s.icon || '📍'
        }

        const customIcon = L.divIcon({
          className: 'custom-marker',
          html: `<div class="marker-pin" style="background-color: ${type.color}">
                   <span>${type.icon}</span>
                 </div>`,
          iconSize: [40, 40],
          iconAnchor: [20, 40]
        })

        const title = (s.title || type.label || '').toString()
        const statusLabel = this.formatStatus(s.status)

        const popupHtml = `
          <div class="signal-popup">
            <div class="signal-popup__title">${title}</div>
            <div class="signal-popup__type" style="color:${type.color}">${type.label}</div>
            <div class="signal-popup__status">Statut: ${statusLabel}</div>
          </div>
        `

        const marker = L.marker([lat, lng], { icon: customIcon })
        marker.bindPopup(popupHtml, {
          closeButton: false,
          autoPan: true,
          offset: L.point(0, -18)
        })

        marker.addTo(this.markersLayer)
      }
    },

    toggleSignal(signalId) {
      const signal = this.signals.find((s) => s.id === signalId)
      if (!signal) return
      signal.visible = !signal.visible
      this.renderMarkers()
    },

    showSignalTypeSelector() {
      this.showTypeSelector = true
    },
    closeTypeSelector() {
      this.showTypeSelector = false
    },
    selectSignalType(signal) {
      this.closeTypeSelector()
      this.$router.push({
        name: 'NewSignal',
        params: { typeId: signal.id.toString() }
      })
    }
  }
}
</script>

<style scoped>
.map-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* Top Bar */
.top-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1001;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.top-bar h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
  font-weight: 600;
}

.btn-signaler {
  padding: 10px 20px;
  background: white;
  color: #2196F3;
  border: 2px solid #2196F3;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-signaler:hover {
  background: #E3F2FD;
}

/* Floating Sidebar */
.sidebar {
  position: absolute;
  top: 80px;
  left: 20px;
  width: 280px;
  max-height: 400px;
  background: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.tab-header {
  display: flex;
  border-bottom: 1px solid #e0e0e0;
  background: white;
}

.tab {
  flex: 1;
  padding: 12px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #666;
  transition: all 0.3s;
}

.tab.active {
  color: #2196F3;
  border-bottom: 2px solid #2196F3;
}

.signals-list {
  padding: 10px 0;
  overflow-y: auto;
}

.signal-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.signal-item:hover {
  background: #f5f5f5;
}

.signal-item input[type="checkbox"] {
  margin-right: 12px;
  cursor: pointer;
}

.signal-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 16px;
}

.signal-label {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.signal-count {
  font-size: 12px;
  color: #999;
}

/* Map */
#map {
  flex: 1;
  height: 100%;
  width: 100%;
}

/* Bottom Navigation */
.bottom-nav {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #1a1a1a;
  display: flex;
  justify-content: space-around;
  z-index: 1000;
}

.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: #888;
  cursor: pointer;
  transition: color 0.3s;
  font-size: 11px;
}

.nav-item.active {
  color: #2196F3;
}

.nav-item:hover {
  color: #2196F3;
}

.nav-item i {
  font-size: 20px;
  margin-bottom: 4px;
}

/* Custom marker styles */
:deep(.custom-marker) {
  background: none;
  border: none;
}

:deep(.marker-pin) {
  width: 40px;
  height: 40px;
  border-radius: 50% 50% 50% 0;
  position: relative;
  transform: rotate(-45deg);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

:deep(.marker-pin span) {
  transform: rotate(45deg);
  font-size: 18px;
}

/* Bottom Sheet */
.bottom-sheet-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  align-items: flex-end;
}

.bottom-sheet {
  width: 100%;
  background: white;
  border-radius: 20px 20px 0 0;
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease-out;
}

.bottom-sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e0e0e0;
}

.bottom-sheet-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.btn-close {
  background: none;
  border: none;
  color: #E53935;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  padding: 5px 10px;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 10px 0;
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.2s;
}

.type-card:active {
  transform: scale(0.95);
}

.type-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: box-shadow 0.2s;
}

.type-card:hover .type-icon {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}

.type-label {
  font-size: 13px;
  color: #666;
  text-align: center;
  font-weight: 500;
}

/* Animations */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: opacity 0.3s;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
}

.slide-up-enter-active .bottom-sheet,
.slide-up-leave-active .bottom-sheet {
  transition: transform 0.3s ease-out;
}

.slide-up-enter-from .bottom-sheet,
.slide-up-leave-to .bottom-sheet {
  transform: translateY(100%);
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .sidebar {
    width: calc(100% - 40px);
    max-width: 280px;
  }
  
  .top-bar h2 {
    font-size: 16px;
  }
  
  .btn-signaler {
    padding: 8px 15px;
    font-size: 12px;
  }
  
  .type-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 15px;
  }
  
  .type-icon {
    width: 50px;
    height: 50px;
    font-size: 24px;
  }
  
  .type-label {
    font-size: 12px;
  }
}

/* Popup style (comme la capture) */
:deep(.leaflet-popup-content-wrapper) {
  border-radius: 8px;
}

:deep(.leaflet-popup-content) {
  margin: 10px 12px;
}

:deep(.signal-popup__title) {
  font-size: 14px;
  font-weight: 700;
  color: #333;
  margin-bottom: 4px;
}

:deep(.signal-popup__type) {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
}

:deep(.signal-popup__status) {
  font-size: 12px;
  color: #666;
}
</style>