<template>
  <ion-page>
    <ion-tabs>
      <ion-router-outlet></ion-router-outlet>
      <ion-tab-bar slot="bottom">
        <ion-tab-button tab="map" href="/tabs/map">
          <ion-icon :icon="mapOutline" />
          <ion-label>Carte</ion-label>
        </ion-tab-button>

        <ion-tab-button tab="test" href="/tabs/test">
          <ion-icon :icon="flaskOutline" />
          <ion-label>Test</ion-label>
        </ion-tab-button>

        <ion-tab-button @click="logout">
          <ion-icon :icon="logOutOutline" />
          <ion-label>Déconnexion</ion-label>
        </ion-tab-button>
      </ion-tab-bar>
    </ion-tabs>
  </ion-page>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import {
  IonTabBar,
  IonTabButton,
  IonTabs,
  IonLabel,
  IonIcon,
  IonPage,
  IonRouterOutlet
} from '@ionic/vue';
import { mapOutline, flaskOutline, logOutOutline } from 'ionicons/icons';
import { signOut } from 'firebase/auth';
import { auth } from '@/config/firebase';

const router = useRouter();

const logout = async () => {
  try {
    await signOut(auth);
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_email');
    router.push('/login');
  } catch (e) {
    console.error('Erreur lors de la déconnexion:', e);
  }
};
</script> 