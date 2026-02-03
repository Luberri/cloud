<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>Connexion</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <div class="login-container">
        <ion-item>
          <ion-label position="floating">Email</ion-label>
          <ion-input v-model="email" type="email"></ion-input>
        </ion-item>
        <ion-item>
          <ion-label position="floating">Mot de passe</ion-label>
          <ion-input v-model="password" type="password"></ion-input>
        </ion-item>
        <ion-button expand="block" @click="login" :disabled="loading" class="ion-margin-top">
          <ion-spinner v-if="loading" name="crescent"></ion-spinner>
          <span v-else>Se connecter</span>
        </ion-button>
        <ion-text color="danger" v-if="error">
          <p class="ion-text-center">{{ error }}</p>
        </ion-text>
        
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  IonPage,
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonItem,
  IonLabel,
  IonInput,
  IonButton,
  IonSpinner,
  IonText
} from '@ionic/vue';
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
    // Authentification Firebase
    const userCredential = await signInWithEmailAndPassword(auth, email.value, password.value);
    const firebaseIdToken = await userCredential.user.getIdToken();
    
    // Stocker le token Firebase
    localStorage.setItem('jwt_token', firebaseIdToken);
    localStorage.setItem('user_email', userCredential.user.email || '');
    
    // Rediriger vers la carte
    router.push('/tabs/map');
  } catch (e: any) {
    error.value = e.message || 'Erreur de connexion';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-container {
  max-width: 400px;
  margin: 50px auto;
}
</style>