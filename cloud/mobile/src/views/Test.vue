<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>Test Firestore</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      <ion-button expand="block" @click="addTest" :disabled="loading">
        <ion-spinner v-if="loading" name="crescent"></ion-spinner>
        <span v-else>Ajouter un road issue (test)</span>
      </ion-button>
      
      <ion-text color="success" v-if="success">
        <p class="ion-text-center">{{ success }}</p>
      </ion-text>
      
      <ion-text color="danger" v-if="error">
        <p class="ion-text-center">{{ error }}</p>
      </ion-text>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import {
  IonPage,
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonSpinner,
  IonText
} from '@ionic/vue';

import { collection, addDoc, Timestamp, GeoPoint } from 'firebase/firestore';
import { db, auth } from '@/config/firebase';

const loading = ref(false);
const success = ref('');
const error = ref('');

const addTest = async () => {
  loading.value = true;
  success.value = '';
  error.value = '';
  
  try {
    // Récupérer l'utilisateur connecté
    const currentUser = auth.currentUser;
    const reportedBy = currentUser?.uid || 'anonymous';
    
    // Ajouter le document à Firestore
    const docRef = await addDoc(collection(db, 'road_issues'), {
      title: 'Route abîmée',
      description: 'Test Ionic Vue - Route nécessitant des réparations urgentes',
      
      // Location: Antananarivo
      location: new GeoPoint(-18.8792, 47.5079),
      
      surface_m2: 15.5,
      budget: 1000000,
      
      status_id: 1,
      company_id: 1,
      
      reported_by: reportedBy,
      
      reported_at: Timestamp.now(),
      updated_at: Timestamp.now(),
      
      is_synced: false,
      firebase_id: '' // Sera mis à jour après
    });
    
    // Mettre à jour avec l'ID Firebase
    // await updateDoc(docRef, { firebase_id: docRef.id });
    
    success.value = `Document ajouté avec succès ! ID: ${docRef.id}`;
    console.log('Document créé avec ID:', docRef.id);
    
  } catch (e: any) {
    console.error('Erreur lors de l\'ajout:', e);
    error.value = e.message || 'Erreur lors de l\'ajout du document';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
ion-text {
  margin-top: 20px;
}
</style>
