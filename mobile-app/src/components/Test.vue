<template>
  <div class="page">
    <div class="top-bar">
      <h2>Tableau récapitulatif</h2>
      <button class="btn-close" @click="goBack">✕</button>
    </div>

    <div class="content">
      <div class="cards">
        <div class="card">
          <div class="card-value">{{ totalSignals }}</div>
          <div class="card-label">Signalements</div>
        </div>
        <div class="card">
          <div class="card-value">{{ formatSurface(totalSurface) }}</div>
          <div class="card-label">Surface totale</div>
        </div>
        <div class="card">
          <div class="card-value">{{ progressPct.toFixed(1) }}%</div>
          <div class="card-label">Avancement</div>
        </div>
        <div class="card">
          <div class="card-value">{{ formatMGA(totalBudget) }}</div>
          <div class="card-label">Budget total</div>
        </div>
      </div>

      <div class="section">
        <div class="section-title">Avancement global</div>
        <div class="progress-wrap">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPct + '%' }"></div>
          </div>
          <div class="progress-text">{{ resolvedCount }} résolus sur {{ totalSignals }}</div>
        </div>
      </div>

      <div class="section">
        <div class="section-title">Détails par type</div>
        <div class="table">
          <div class="thead">
            <div>Type</div>
            <div>Nb</div>
            <div>Surface (m²)</div>
            <div>Avancement</div>
            <div>Budget (MGA)</div>
          </div>

          <div v-for="row in perTypeRows" :key="row.typeId" class="trow">
            <div class="type">
              <span class="type-icon" :style="{ backgroundColor: row.color }">{{ row.icon }}</span>
              <span>{{ row.label }}</span>
            </div>
            <div>{{ row.count }}</div>
            <div>{{ row.surface.toFixed(1) }}</div>
            <div class="adv">
              <div class="mini-bar">
                <div class="mini-fill" :style="{ width: row.progressPct + '%' }"></div>
              </div>
              <span class="adv-text">{{ row.progressPct.toFixed(0) }}%</span>
            </div>
            <div>{{ formatMGA(row.budget) }}</div>
          </div>

          <div class="tfoot">
            <div>TOTAL</div>
            <div>{{ totalSignals }}</div>
            <div>{{ totalSurface.toFixed(1) }}</div>
            <div>{{ progressPct.toFixed(1) }}%</div>
            <div>{{ formatMGA(totalBudget) }}</div>
          </div>
        </div>
      </div>

      <div class="section">
        <div class="section-title">Répartition par statut</div>
        <div class="status-grid">
          <div class="status-card orange">
            <div class="status-value">{{ byStatus.signale }}</div>
            <div class="status-label">Signalé</div>
            <div class="status-sub">{{ pct(byStatus.signale) }}%</div>
          </div>
          <div class="status-card blue">
            <div class="status-value">{{ byStatus.en_cours }}</div>
            <div class="status-label">En cours</div>
            <div class="status-sub">{{ pct(byStatus.en_cours) }}%</div>
          </div>
          <div class="status-card green">
            <div class="status-value">{{ byStatus.resolu }}</div>
            <div class="status-label">Résolu</div>
            <div class="status-sub">{{ pct(byStatus.resolu) }}%</div>
          </div>
          <div class="status-card gray">
            <div class="status-value">{{ byStatus.rejete }}</div>
            <div class="status-label">Rejeté</div>
            <div class="status-sub">{{ pct(byStatus.rejete) }}%</div>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-nav">
      <button class="nav-item" @click="goMap">
        <span>Carte</span>
      </button>
      <button class="nav-item active" @click="goTest">
        <span>Test</span>
      </button>
      <button class="nav-item" disabled>
        <span>Déconnexion</span>
      </button>
    </div>
  </div>
</template>

<script>
import { db } from '../firebase'
import { collection, onSnapshot, query, orderBy } from 'firebase/firestore'

export default {
  name: 'Test',
  data() {
    return {
      unsubscribeSignals: null,
      firestoreSignals: [],
      types: [
        { id: 1, label: 'Danger', color: '#E53935', icon: '⚠️' },
        { id: 2, label: 'Accident', color: '#8E24AA', icon: '🚗' },
        { id: 3, label: 'Travaux', color: '#FB8C00', icon: '🚧' },
        { id: 4, label: 'Inondation', color: '#039BE5', icon: '💧' },
        { id: 5, label: 'Nid de poule', color: '#C62828', icon: '🕳️' },
        { id: 7, label: 'Électricité', color: '#FDD835', icon: '⚡' },
        { id: 8, label: 'Végétation', color: '#43A047', icon: '🌿' }
      ]
    }
  },
  computed: {
    totalSignals() {
      return this.firestoreSignals.length
    },
    totalSurface() {
      return this.firestoreSignals.reduce((sum, s) => sum + (Number(s.surface) || 0), 0)
    },
    totalBudget() {
      return this.firestoreSignals.reduce((sum, s) => sum + (Number(s.budget) || 0), 0)
    },
    resolvedCount() {
      return this.firestoreSignals.filter((s) => this.normStatus(s.status) === 'resolu').length
    },
    progressPct() {
      if (!this.totalSignals) return 0
      return (this.resolvedCount / this.totalSignals) * 100
    },
    byStatus() {
      const out = { signale: 0, en_cours: 0, resolu: 0, rejete: 0 }
      for (const s of this.firestoreSignals) {
        const st = this.normStatus(s.status)
        if (out[st] != null) out[st]++
      }
      return out
    },
    perTypeRows() {
      const map = new Map()
      for (const s of this.firestoreSignals) {
        const typeId = Number(s.typeId)
        if (!Number.isFinite(typeId)) continue

        const type = this.types.find((t) => t.id === typeId) || {
          id: typeId,
          label: (s.type || 'Autre').toString(),
          color: s.color || '#666',
          icon: s.icon || '📍'
        }

        const cur = map.get(typeId) || {
          typeId,
          label: type.label,
          color: type.color,
          icon: type.icon,
          count: 0,
          surface: 0,
          budget: 0,
          resolved: 0
        }

        cur.count += 1
        cur.surface += Number(s.surface) || 0
        cur.budget += Number(s.budget) || 0
        if (this.normStatus(s.status) === 'resolu') cur.resolved += 1

        map.set(typeId, cur)
      }

      return Array.from(map.values())
        .map((r) => ({
          ...r,
          progressPct: r.count ? (r.resolved / r.count) * 100 : 0
        }))
        .sort((a, b) => b.count - a.count)
    }
  },
  mounted() {
    const q = query(collection(db, 'signals'), orderBy('createdAt', 'desc'))
    this.unsubscribeSignals = onSnapshot(
      q,
      (snap) => {
        this.firestoreSignals = snap.docs.map((d) => ({ id: d.id, ...d.data() }))
      },
      (err) => console.error('Firestore onSnapshot error:', err)
    )
  },
  beforeUnmount() {
    if (this.unsubscribeSignals) this.unsubscribeSignals()
  },
  methods: {
    goBack() {
      this.goMap()
    },
    goMap() {
      this.$router.push({ name: 'Map' })
    },
    goTest() {
      this.$router.push({ name: 'Test' })
    },
    normStatus(status) {
      const s = (status || '').toString().toLowerCase()
      if (s === 'signale' || s === 'signalé') return 'signale'
      if (s === 'en_cours' || s === 'encours' || s === 'en cours') return 'en_cours'
      if (s === 'resolu' || s === 'résolu') return 'resolu'
      if (s === 'rejete' || s === 'rejeté') return 'rejete'
      return 'signale'
    },
    pct(n) {
      if (!this.totalSignals) return 0
      return ((n / this.totalSignals) * 100).toFixed(1)
    },
    formatSurface(v) {
      return `${Number(v || 0).toFixed(1)} m²`
    },
    formatMGA(v) {
      const n = Number(v || 0)
      if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)} M`
      if (n >= 1_000) return `${(n / 1_000).toFixed(1)} K`
      return `${Math.round(n)}`
    }
  }
}
</script>

<style scoped>
.page {
  height: 100vh;
  width: 100vw;
  background: #0f0f0f;
  color: #fff;
  display: flex;
  flex-direction: column;
}

.top-bar {
  height: 60px;
  padding: 0 16px;
  background: #1b1b1b;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.top-bar h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.btn-close {
  background: transparent;
  color: #fff;
  border: none;
  font-size: 18px;
  cursor: pointer;
}

.content {
  flex: 1;
  overflow: auto;
  padding: 14px;
  padding-bottom: 80px;
}

.cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.card {
  background: linear-gradient(135deg, #5b3bd6, #2c84ff);
  border-radius: 10px;
  padding: 12px;
}

.card-value {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.1;
}

.card-label {
  opacity: 0.85;
  margin-top: 6px;
  font-size: 12px;
}

.section {
  margin-top: 14px;
  background: #151515;
  border-radius: 10px;
  padding: 12px;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 10px;
  opacity: 0.9;
}

.progress-bar {
  height: 10px;
  background: #2a2a2a;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #4caf50;
}

.progress-text {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.85;
  text-align: center;
}

.table {
  width: 100%;
  font-size: 12px;
}

.thead,
.tfoot,
.trow {
  display: grid;
  grid-template-columns: 1.4fr 0.4fr 0.7fr 0.9fr 0.7fr;
  gap: 8px;
  align-items: center;
}

.thead {
  opacity: 0.75;
  padding: 8px 0;
  border-bottom: 1px solid #2a2a2a;
}

.trow {
  padding: 10px 0;
  border-bottom: 1px solid #232323;
}

.tfoot {
  padding-top: 10px;
  font-weight: 700;
  opacity: 0.9;
}

.type {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-icon {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.adv {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-bar {
  flex: 1;
  height: 6px;
  background: #2a2a2a;
  border-radius: 999px;
  overflow: hidden;
}

.mini-fill {
  height: 100%;
  background: #4caf50;
}

.adv-text {
  width: 32px;
  text-align: right;
  opacity: 0.85;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.status-card {
  border-radius: 12px;
  padding: 12px;
  color: #fff;
}

.status-card.orange { background: linear-gradient(135deg, #ff8a00, #ffb300); }
.status-card.blue { background: linear-gradient(135deg, #0277bd, #29b6f6); }
.status-card.green { background: linear-gradient(135deg, #2e7d32, #66bb6a); }
.status-card.gray { background: linear-gradient(135deg, #424242, #757575); }

.status-value {
  font-size: 22px;
  font-weight: 800;
}

.status-label {
  margin-top: 4px;
  font-size: 12px;
  font-weight: 700;
}

.status-sub {
  margin-top: 2px;
  font-size: 11px;
  opacity: 0.85;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #1a1a1a;
  display: flex;
  z-index: 1000;
}

.nav-item {
  flex: 1;
  background: transparent;
  border: none;
  color: #888;
  font-size: 12px;
  cursor: pointer;
}

.nav-item.active {
  color: #2196F3;
}
</style>