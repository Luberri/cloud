import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

// Configuration Firebase
const firebaseConfig = {
  apiKey: "AIzaSyCGKUQ7erBesYSmf_JJ5l61Gy0lr6jINOQ",
  authDomain: "cloud-5c339.firebaseapp.com",
  projectId: "cloud-5c339",
  storageBucket: "cloud-5c339.firebasestorage.app",
  messagingSenderId: "648861042145",
  appId: "1:648861042145:web:c76c87ad2c8122e924fea0",
  measurementId: "G-J4VT6W6SNR"
};

// Initialiser Firebase
const app = initializeApp(firebaseConfig);

// Exporter l'instance d'authentification
export const auth = getAuth(app);

// Exporter l'instance Firestore
export const db = getFirestore(app);

// Exporter l'app si nécessaire
export default app;