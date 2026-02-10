import { LocalNotifications } from '@capacitor/local-notifications';
import { Preferences } from '@capacitor/preferences';
import { Capacitor } from '@capacitor/core';
import { doc, setDoc, onSnapshot, collection, query, where, Timestamp, getDocs } from 'firebase/firestore';
import { db, auth } from '@/config/firebase';

class NotificationService {
  private unsubscribeListeners: (() => void)[] = [];
  private isInitialized = false;
  private statusCache: Map<string, string> = new Map();

  // Initialiser les notifications
  async initialize(): Promise<void> {
    if (this.isInitialized) return;

    try {
      console.log('🔔 Initialisation du service de notifications...');
      console.log('📱 Plateforme:', Capacitor.getPlatform());

      // Demander les permissions pour les notifications locales
      const permStatus = await LocalNotifications.requestPermissions();
      console.log('📱 Permission notifications:', permStatus.display);

      if (permStatus.display === 'granted') {
        // Configurer le channel de notification pour Android (important !)
        if (Capacitor.getPlatform() === 'android') {
          await this.createNotificationChannel();
        }
        
        // Configurer le listener pour les notifications cliquées
        await this.setupNotificationListeners();
        this.isInitialized = true;
        console.log('✅ Service de notifications initialisé');
      } else {
        console.warn('⚠️ Permission de notification refusée');
      }

    } catch (error) {
      console.error('❌ Erreur initialisation notifications:', error);
    }
  }

  // Créer un channel de notification pour Android 8+
  private async createNotificationChannel(): Promise<void> {
    try {
      await LocalNotifications.createChannel({
        id: 'status_updates',
        name: 'Mises à jour de statut',
        description: 'Notifications pour les changements de statut des signalements',
        importance: 5, // Max importance pour afficher en heads-up
        visibility: 1, // Public
        vibration: true,
        sound: 'default',
        lights: true,
        lightColor: '#2196F3'
      });
      console.log('✅ Channel de notification créé');
    } catch (error) {
      console.error('❌ Erreur création channel:', error);
    }
  }

  // Configurer les listeners de notifications
  private async setupNotificationListeners(): Promise<void> {
    // Quand une notification est reçue (app en premier plan)
    await LocalNotifications.addListener('localNotificationReceived', (notification) => {
      console.log('📬 Notification reçue:', notification);
    });

    // Quand l'utilisateur clique sur une notification
    await LocalNotifications.addListener('localNotificationActionPerformed', (action) => {
      console.log('👆 Action notification:', action);
      const data = action.notification.extra;
      
      if (data?.issueId) {
        console.log('📍 Navigation vers signalement:', data.issueId);
        // Naviguer vers le signalement
        window.location.href = `/tabs/map?issueId=${data.issueId}`;
      }
    });
  }

  // Sauvegarder une valeur dans le cache persistant
  private async setStoredStatus(key: string, value: string): Promise<void> {
    try {
      await Preferences.set({ key, value });
    } catch {
      localStorage.setItem(key, value);
    }
  }

  // Récupérer une valeur du cache persistant
  private async getStoredStatus(key: string): Promise<string | null> {
    try {
      const { value } = await Preferences.get({ key });
      return value;
    } catch {
      return localStorage.getItem(key);
    }
  }

  // Initialiser le cache des statuts
  private async initializeStatusCache(userId: string): Promise<void> {
    console.log('📦 Initialisation du cache des statuts...');

    try {
      // Charger depuis 'signals'
      const signalsQuery = query(
        collection(db, 'signals'),
        where('reportedBy', '==', userId)
      );
      const signalsSnapshot = await getDocs(signalsQuery);

      signalsSnapshot.docs.forEach(doc => {
        const data = doc.data();
        const status = (data.statusId?.toString() || data.status || '1').toString();
        const cacheKey = `issue_status_signals_${doc.id}`;
        this.statusCache.set(cacheKey, status);
        this.setStoredStatus(cacheKey, status);
      });

      // Charger depuis 'road_issues'
      const roadIssuesQuery = query(
        collection(db, 'road_issues'),
        where('reportedBy', '==', userId)
      );
      const roadIssuesSnapshot = await getDocs(roadIssuesQuery);

      roadIssuesSnapshot.docs.forEach(doc => {
        const data = doc.data();
        const status = (data.statusId?.toString() || data.status || '1').toString();
        const cacheKey = `issue_status_road_issues_${doc.id}`;
        this.statusCache.set(cacheKey, status);
        this.setStoredStatus(cacheKey, status);
      });

      console.log(`✅ Cache initialisé avec ${this.statusCache.size} signalements`);

    } catch (error) {
      console.error('❌ Erreur initialisation cache:', error);
    }
  }

  // Écouter les changements de statut des signalements
  async startListeningToMyIssues(): Promise<void> {
    const currentUser = auth.currentUser;
    if (!currentUser) {
      console.warn('⚠️ Utilisateur non connecté');
      return;
    }

    console.log('👂 Début écoute des signalements pour:', currentUser.uid);

    // Initialiser le cache
    await this.initializeStatusCache(currentUser.uid);

    // Écouter 'signals'
    const signalsQuery = query(
      collection(db, 'signals'),
      where('reportedBy', '==', currentUser.uid)
    );

    const unsubscribeSignals = onSnapshot(signalsQuery, (snapshot) => {
      snapshot.docChanges().forEach((change) => {
        if (change.type === 'modified') {
          const data = change.doc.data();
          this.checkStatusChange(change.doc.id, data, 'signals');
        }
      });
    }, (error) => {
      console.error('❌ Erreur écoute signals:', error);
    });

    // Écouter 'road_issues'
    const roadIssuesQuery = query(
      collection(db, 'road_issues'),
      where('reportedBy', '==', currentUser.uid)
    );

    const unsubscribeRoadIssues = onSnapshot(roadIssuesQuery, (snapshot) => {
      snapshot.docChanges().forEach((change) => {
        if (change.type === 'modified') {
          const data = change.doc.data();
          this.checkStatusChange(change.doc.id, data, 'road_issues');
        }
      });
    }, (error) => {
      console.error('❌ Erreur écoute road_issues:', error);
    });

    this.unsubscribeListeners.push(unsubscribeSignals, unsubscribeRoadIssues);
    console.log('✅ Écoute des signalements activée');
  }

  // Vérifier si le statut a changé
  private async checkStatusChange(
    issueId: string,
    newData: any,
    collectionName: string
  ): Promise<void> {
    const cacheKey = `issue_status_${collectionName}_${issueId}`;

    // Récupérer l'ancien statut
    let previousStatus = this.statusCache.get(cacheKey);
    if (!previousStatus) {
      previousStatus = await this.getStoredStatus(cacheKey);
    }

    const currentStatus = (newData.statusId?.toString() || newData.status || '').toString();

    console.log(`🔍 Vérification ${issueId}:`);
    console.log(`   - Ancien: ${previousStatus || 'AUCUN'}`);
    console.log(`   - Nouveau: ${currentStatus}`);

    // Si le statut a changé, envoyer une notification
    if (previousStatus && previousStatus !== currentStatus && currentStatus) {
      console.log(`🔔 CHANGEMENT DÉTECTÉ !`);
      await this.sendStatusChangeNotification(newData, previousStatus, currentStatus, issueId);
    }

    // Mettre à jour le cache
    if (currentStatus) {
      this.statusCache.set(cacheKey, currentStatus);
      await this.setStoredStatus(cacheKey, currentStatus);
    }
  }

  // Envoyer une notification de changement de statut
  private async sendStatusChangeNotification(
    issueData: any,
    oldStatus: string,
    newStatus: string,
    issueId: string
  ): Promise<void> {
    const statusLabels: Record<string, string> = {
      '1': 'Signalé',
      '2': 'En cours',
      '3': 'Résolu',
      '4': 'Rejeté'
    };

    const oldLabel = statusLabels[oldStatus] || oldStatus;
    const newLabel = statusLabels[newStatus] || newStatus;
    const title = issueData.title || 'Signalement';

    // Choisir l'emoji selon le nouveau statut
    const statusEmoji: Record<string, string> = {
      '1': '🔔',
      '2': '🔧',
      '3': '✅',
      '4': '❌'
    };
    const emoji = statusEmoji[newStatus] || '📢';

    try {
      // Générer un ID unique pour la notification
      const notificationId = Math.floor(Math.random() * 2147483647);
      
      await LocalNotifications.schedule({
        notifications: [
          {
            id: notificationId,
            title: `${emoji} Mise à jour de statut`,
            body: `"${title}" : ${oldLabel} → ${newLabel}`,
            largeBody: `Votre signalement "${title}" a changé de statut.\n\nAncien statut: ${oldLabel}\nNouveau statut: ${newLabel}`,
            summaryText: 'Signalement mis à jour',
            channelId: 'status_updates', // Utiliser le channel créé
            extra: {
              issueId: issueId,
              oldStatus: oldStatus,
              newStatus: newStatus
            },
            // Notification immédiate
            schedule: { at: new Date(Date.now() + 500) },
            sound: 'default',
            // Utiliser l'icône par défaut de l'app
            smallIcon: 'ic_launcher_foreground',
            largeIcon: 'ic_launcher',
            // Options pour Android
            autoCancel: true,
            ongoing: false
          }
        ]
      });

      console.log('✅ Notification système envoyée !');
      console.log(`   ID: ${notificationId}`);
      console.log(`   Titre: ${emoji} Mise à jour de statut`);
      console.log(`   Corps: "${title}" : ${oldLabel} → ${newLabel}`);

      // Sauvegarder dans l'historique Firestore
      await this.saveNotificationToHistory(issueData, oldLabel, newLabel, issueId);

    } catch (error) {
      console.error('❌ Erreur envoi notification:', error);
    }
  }

  // Sauvegarder dans l'historique
  private async saveNotificationToHistory(
    issueData: any,
    oldStatus: string,
    newStatus: string,
    issueId: string
  ): Promise<void> {
    const currentUser = auth.currentUser;
    if (!currentUser) return;

    try {
      const notificationData = {
        userId: currentUser.uid,
        issueId: issueId,
        issueTitle: issueData.title || 'Sans titre',
        type: 'status_change',
        oldStatus: oldStatus,
        newStatus: newStatus,
        read: false,
        createdAt: Timestamp.now()
      };

      await setDoc(
        doc(db, 'notifications', `${currentUser.uid}_${Date.now()}`),
        notificationData
      );

      console.log('✅ Notification sauvegardée dans l\'historique');
    } catch (error) {
      console.error('❌ Erreur sauvegarde historique:', error);
    }
  }

  // Arrêter l'écoute
  stopListening(): void {
    this.unsubscribeListeners.forEach(unsubscribe => unsubscribe());
    this.unsubscribeListeners = [];
    console.log('🛑 Écoute arrêtée');
  }
}

export const notificationService = new NotificationService();