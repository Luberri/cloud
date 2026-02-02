<template>
  <div class="form-container">
    <!-- Top Bar -->
    <div class="top-bar">
      <button class="btn-back" @click="goBack">
        <span class="icon-back">←</span>
      </button>
      <h2>Nouveau signalement</h2>
      <button class="btn-close" @click="goBack">✕</button>
    </div>

    <!-- Form Content -->
    <div class="form-content">
      <!-- Signal Type Icon -->
      <div class="signal-type-badge">
        <div class="type-icon-large" :style="{ backgroundColor: currentSignal.color }">
          {{ currentSignal.icon }}
        </div>
      </div>

      <!-- Position -->
      <div class="form-field">
        <label class="field-icon">📍</label>
        <div class="field-value">
          <span class="field-label">Position:</span>
          <span class="position-text">{{ position }}</span>
        </div>
        <button class="btn-refresh" @click="getCurrentPosition">🔄</button>
      </div>

      <!-- Title -->
      <div class="form-group">
        <label for="title">Titre *</label>
        <input 
          id="title"
          v-model="formData.title"
          type="text" 
          :placeholder="currentSignal.label"
          required
        />
      </div>

      <!-- Description -->
      <div class="form-group">
        <label for="description">Description *</label>
        <textarea 
          id="description"
          v-model="formData.description"
          rows="4"
          placeholder="Décrivez le problème en détail..."
          required
        ></textarea>
      </div>

      <!-- Surface -->
      <div class="form-group">
        <label for="surface">Surface (m²)</label>
        <input 
          id="surface"
          v-model="formData.surface"
          type="number" 
          placeholder="40"
          min="0"
        />
      </div>

      <!-- Budget -->
      <div class="form-group">
        <label for="budget">Budget estimé (MGA)</label>
        <input 
          id="budget"
          v-model="formData.budget"
          type="number" 
          placeholder="800000"
          min="0"
        />
      </div>

      <!-- Status -->
      <div class="form-group">
        <label for="status">Statut</label>
        <select id="status" v-model="formData.status">
          <option value="signale">Signalé</option>
          <option value="en_cours">En cours</option>
          <option value="resolu">Résolu</option>
        </select>
      </div>
    
      <!-- Submit Button -->
      <button 
        class="btn-submit" 
        @click="submitSignal"
        :disabled="isSubmitting"
      >
        {{ isSubmitting ? 'CRÉATION EN COURS...' : 'CRÉER LE SIGNALEMENT' }}
      </button>
    </div>
  </div>
</template>

<script>
import { db } from '../firebase';
import { collection, addDoc, serverTimestamp } from 'firebase/firestore';

export default {
  name: 'SignalForm',
  props: {
    typeId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      position: 'Chargement...',
      latitude: null,
      longitude: null,
      isSubmitting: false,
      formData: {
        title: '',
        description: '',
        surface: '',
        budget: '',
        status: 'signale'
      },
      signals: [
        { id: 1, label: 'Danger', color: '#E53935', icon: '⚠️' },
        { id: 2, label: 'Accident', color: '#8E24AA', icon: '🚗' },
        { id: 3, label: 'Travaux', color: '#FB8C00', icon: '🚧' },
        { id: 4, label: 'Inondation', color: '#039BE5', icon: '💧' },
        { id: 5, label: 'Nid de poule', color: '#C62828', icon: '🕳️' },
        { id: 7, label: 'Électricité', color: '#FDD835', icon: '⚡' }
      ]
    };
  },
  computed: {
    currentSignal() {
      return this.signals.find(s => s.id === parseInt(this.typeId)) || this.signals[0];
    }
  },
  mounted() {
    this.getCurrentPosition();
    this.formData.title = this.currentSignal.label;
  },
  methods: {
    getCurrentPosition() {
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (position) => {
            this.latitude = position.coords.latitude;
            this.longitude = position.coords.longitude;
            this.position = `${this.latitude.toFixed(6)}, ${this.longitude.toFixed(6)}`;
          },
          (error) => {
            console.error('Erreur de géolocalisation:', error);
            this.position = 'Position non disponible';
          }
        );
      } else {
        this.position = 'Géolocalisation non supportée';
      }
    },

    goBack() {
      this.$router.push({ name: 'Map' });
    },

    async submitSignal() {
      console.log('=== DÉBUT SOUMISSION ===');
      
      // Validation
      if (!this.formData.title || !this.formData.description) {
        console.error('Validation échouée: champs manquants');
        alert('Veuillez remplir tous les champs obligatoires');
        return;
      }

      if (!this.latitude || !this.longitude) {
        console.error('Validation échouée: position manquante');
        alert('Position non disponible. Veuillez réessayer.');
        return;
      }

      if (this.isSubmitting) {
        console.log('Soumission déjà en cours, annulation');
        return;
      }

      this.isSubmitting = true;
      console.log('isSubmitting = true');

      try {
        const signalData = {
          type: this.currentSignal.label,
          typeId: parseInt(this.typeId),
          title: this.formData.title,
          description: this.formData.description,
          surface: this.formData.surface ? parseFloat(this.formData.surface) : null,
          budget: this.formData.budget ? parseFloat(this.formData.budget) : null,
          status: this.formData.status,
          latitude: this.latitude,
          longitude: this.longitude,
          color: this.currentSignal.color,
          icon: this.currentSignal.icon,
          createdAt: serverTimestamp()
        };

        console.log('Données préparées:', signalData);
        console.log("Tentative d'ajout à Firestore...");

        const timeoutMs = 15000;
        const docRef = await Promise.race([
          addDoc(collection(db, 'signals'), signalData),
          new Promise((_, reject) =>
            setTimeout(() => reject(new Error(`Firestore timeout après ${timeoutMs}ms`)), timeoutMs)
          )
        ]);

        console.log('✅ SUCCÈS! Signal créé avec ID:', docRef.id);
        alert('Signalement créé avec succès!');
        this.goBack();
      } catch (error) {
        console.error('❌ ERREUR lors de la création:', error);
        alert(`Erreur Firestore: ${error?.message || error}`);
      } finally {
        this.isSubmitting = false;
        console.log('isSubmitting = false');
        console.log('=== FIN SOUMISSION ===');
      }
    }
  }
};
</script>

<style scoped>
.form-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  background: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* Top Bar */
.top-bar {
  width: 100%;
  height: 60px;
  background: #2c2c2c;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
}

.top-bar h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  flex: 1;
  text-align: center;
}

.btn-back, .btn-close {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  padding: 5px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-back {
  font-size: 28px;
}

/* Form Content */
.form-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

/* Signal Type Badge */
.signal-type-badge {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
  width: 100%;
}

.type-icon-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* Position Field */
.form-field {
  background: #e3f2fd;
  border-radius: 8px;
  padding: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  width: 100%;
  box-sizing: border-box;
}

.field-icon {
  font-size: 24px;
}

.field-value {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.field-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.position-text {
  font-size: 14px;
  color: #2196F3;
  font-weight: 500;
}

.btn-refresh {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 5px;
}

/* Form Groups */
.form-group {
  background: white;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  width: 100%;
  box-sizing: border-box;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #2196F3;
}

.form-group textarea {
  resize: vertical;
  min-height: 100px;
}

/* Submit Button */
.btn-submit {
  width: 100%;
  padding: 15px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 20px;
  transition: background 0.3s;
  box-sizing: border-box;
}

.btn-submit:hover {
  background: #1976D2;
}

.btn-submit:active {
  transform: scale(0.98);
}

.btn-submit:disabled {
  background: #90CAF9;
  cursor: not-allowed;
}

/* Responsive */
@media (max-width: 768px) {
  .form-content {
    padding: 15px;
  }

  .type-icon-large {
    width: 60px;
    height: 60px;
    font-size: 30px;
  }
}
</style>