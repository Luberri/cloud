<template>
  <ion-page>
    <ion-content class="login-content" :fullscreen="true">
      <!-- Background gradient -->
      <div class="login-background">
        <div class="gradient-circle circle-1"></div>
        <div class="gradient-circle circle-2"></div>
      </div>
      
      <div class="login-wrapper">
        <!-- Logo et titre -->
        <div class="login-header">
          <div class="app-logo">
            <img src="@/image.png" alt="SignalRoute" />
          </div>
          <h1 class="app-name">SignalRoute</h1>
          <p class="app-tagline">Signalez les problèmes de voirie</p>
        </div>
        
        <!-- Formulaire -->
        <div class="login-card">
          <h2 class="form-title">Connexion</h2>
          
          <div class="input-group">
            <label class="input-label">
              <ion-icon :icon="mailOutline"></ion-icon>
              Email
            </label>
            <input 
              v-model="email" 
              type="email" 
              class="modern-input"
              placeholder="votre@email.com"
            />
          </div>
          
          <div class="input-group">
            <label class="input-label">
              <ion-icon :icon="lockClosedOutline"></ion-icon>
              Mot de passe
            </label>
            <input 
              v-model="password" 
              type="password" 
              class="modern-input"
              placeholder="••••••••"
            />
          </div>
          
          <button 
            class="login-btn" 
            @click="login" 
            :disabled="loading"
          >
            <ion-spinner v-if="loading" name="crescent" class="btn-spinner"></ion-spinner>
            <template v-else>
              <ion-icon :icon="logInOutline"></ion-icon>
              <span>Se connecter</span>
            </template>
          </button>
          
          <p v-if="error" class="error-message">
            <ion-icon :icon="alertCircleOutline"></ion-icon>
            {{ error }}
          </p>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  IonPage,
  IonContent,
  IonIcon,
  IonSpinner
} from '@ionic/vue';
import { mailOutline, lockClosedOutline, logInOutline, alertCircleOutline } from 'ionicons/icons';
import { signInWithEmailAndPassword } from 'firebase/auth';
import { auth } from '@/config/firebase';

const router = useRouter();
const email = ref('');
const password = ref('');
const loading = ref(false);
const error = ref('');

const login = async () => {
  loading.value = true;
  error.value = '';
  
  try {
    const userCredential = await signInWithEmailAndPassword(auth, email.value, password.value);
    const firebaseIdToken = await userCredential.user.getIdToken();
    
    localStorage.setItem('jwt_token', firebaseIdToken);
    localStorage.setItem('user_email', userCredential.user.email || '');
    
    router.push('/tabs/map');
  } catch (e: any) {
    error.value = e.message || 'Erreur de connexion';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-content {
  --background: #0f0f1a;
}

.login-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.gradient-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.6;
}

.circle-1 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  top: -100px;
  right: -50px;
}

.circle-2 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, #f093fb, #f5576c);
  bottom: 100px;
  left: -80px;
}

.login-wrapper {
  position: relative;
  z-index: 1;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 40px 24px;
}

/* Header */
.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.app-logo {
  width: 120px;
  height: 120px;
  margin: 0 auto 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 15px 50px rgba(102, 126, 234, 0.5);
}

.app-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 32px;
}

.app-name {
  margin: 0;
  font-size: 32px;
  font-weight: 800;
  color: white;
  letter-spacing: -1px;
}

.app-tagline {
  margin: 8px 0 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

/* Card */
.login-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 32px 24px;
}

.form-title {
  margin: 0 0 24px 0;
  font-size: 20px;
  font-weight: 700;
  color: white;
  text-align: center;
}

/* Input */
.input-group {
  margin-bottom: 20px;
}

.input-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
}

.input-label ion-icon {
  font-size: 16px;
  color: #667eea;
}

.modern-input {
  width: 100%;
  padding: 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  font-size: 16px;
  color: white;
  outline: none;
  transition: all 0.3s ease;
}

.modern-input::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.modern-input:focus {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.15);
}

/* Button */
.login-btn {
  width: 100%;
  padding: 16px 24px;
  margin-top: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: all 0.3s ease;
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.35);
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(102, 126, 234, 0.45);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-btn ion-icon {
  font-size: 20px;
}

.btn-spinner {
  width: 20px;
  height: 20px;
  --color: white;
}

/* Error */
.error-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 16px 0 0 0;
  padding: 12px 16px;
  background: rgba(245, 101, 101, 0.15);
  border: 1px solid rgba(245, 101, 101, 0.3);
  border-radius: 12px;
  color: #fc8181;
  font-size: 13px;
}

.error-message ion-icon {
  font-size: 18px;
  flex-shrink: 0;
}

</style>