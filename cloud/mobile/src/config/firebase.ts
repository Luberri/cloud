import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

// Configuration Firebase
const firebaseConfig = {
  apiKey: "AIzaSyBF6FcOMpyUFMIt3m11c7ZSRBXL-u3KIA4",
  authDomain: "testfirebase-f69fb.firebaseapp.com",
  projectId: "testfirebase-f69fb",
  storageBucket: "testfirebase-f69fb.firebasestorage.app",
  messagingSenderId: "1009321928506",
  appId: "1:1009321928506:web:3ac087a565474dbb3f1203",
  measurementId: "G-NYQ0QCW8D3"
};

// Initialiser Firebase
const app = initializeApp(firebaseConfig);

// Exporter l'instance d'authentification
export const auth = getAuth(app);

// Exporter l'instance Firestore
export const db = getFirestore(app);

// Exporter l'app si nécessaire
export default app;