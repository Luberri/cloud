import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: "AIzaSyBF6FcOMpyUFMIt3m11c7ZSRBXL-u3KIA4",
  authDomain: "testfirebase-f69fb.firebaseapp.com",
  projectId: "testfirebase-f69fb",
  storageBucket: "testfirebase-f69fb.firebasestorage.app",
  messagingSenderId: "1009321928506",
  appId: "1:1009321928506:web:3ac087a565474dbb3f1203",
  measurementId: "G-NYQ0QCW8D3"
};

const app = initializeApp(firebaseConfig);

// Firestore
const db = getFirestore(app);

// Analytics (optionnel, peut échouer sur certains environnements)
let analytics = null;
try {
  analytics = getAnalytics(app);
} catch (e) {
  analytics = null;
}

export { db, analytics };