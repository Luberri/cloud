<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>Inscription</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <div class="register-container">
        <ion-item>
          <ion-label position="floating">Nom complet</ion-label>
          <ion-input v-model="fullName" type="text"></ion-input>
        </ion-item>
        <ion-item>
          <ion-label position="floating">Email</ion-label>
          <ion-input v-model="email" type="email"></ion-input>
        </ion-item>
        <ion-item>
          <ion-label position="floating">Mot de passe</ion-label>
          <ion-input v-model="password" type="password"></ion-input>
        </ion-item>
        <ion-item>
          <ion-label position="floating">Confirmer le mot de passe</ion-label>
          <ion-input v-model="confirmPassword" type="password"></ion-input>
        </ion-item>
        
        <ion-button expand="block" @click="register" :disabled="loading" class="ion-margin-top">
          <ion-spinner v-if="loading" name="crescent"></ion-spinner>
          <span v-else>S'inscrire</span>
        </ion-button>
        
        <ion-text color="danger" v-if="error">
          <p class="ion-text-center">{{ error }}</p>
        </ion-text>
        
        <ion-text color="success" v-if="success">
          <p class="ion-text-center">{{ success }}</p>
        </ion-text>
        
        <div class="ion-text-center ion-margin-top">
          <p>Déjà un compte ?</p>
          <ion-button fill="clear" router-link="/login">
            Se connecter
          </ion-button>
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
import { createUserWithEmailAndPassword, updateProfile } from 'firebase/auth';
import { auth } from '@/config/firebase';

const router = useRouter();
const fullName = ref('');
const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const loading = ref(false);
const error = ref('');
const success = ref('');

const register = async () => {
  error.value = '';
  success.value = '';
  
  // Validation
  if (!fullName.value || !email.value || !password.value) {
    error.value = 'Veuillez remplir tous les champs';
    return;
  }
  
  if (password.value !== confirmPassword.value) {
    error.value = 'Les mots de passe ne correspondent pas';
    return;
  }
  
  if (password.value.length < 6) {
    error.value = 'Le mot de passe doit contenir au moins 6 caractères';
    return;
  }
  
  loading.value = true;
  
  try {
    // Créer l'utilisateur dans Firebase
    const userCredential = await createUserWithEmailAndPassword(
      auth,
      email.value,
      password.value
    );
    
    // Mettre à jour le profil avec le nom complet
    await updateProfile(userCredential.user, {
      displayName: fullName.value
    });
    
    success.value = 'Inscription réussie ! Redirection vers la connexion...';
    
    // Rediriger vers login après 2 secondes
    setTimeout(() => {
      router.push('/login');
    }, 2000);
    
  } catch (e: any) {
    error.value = e.message || 'Erreur lors de l\'inscription';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.register-container {
  max-width: 400px;
  margin: 30px auto;
}
</style>