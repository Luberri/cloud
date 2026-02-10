import { PushNotifications } from '@capacitor/push-notifications';
import { LocalNotifications } from '@capacitor/local-notifications';
import { Preferences } from '@capacitor/preferences';
import { doc, setDoc, onSnapshot, collection, query, where, Timestamp } from 'firebase/firestore';
import { db, auth } from '@/config/firebase';

// Interface pour le token FCM
interface UserNotificationToken {
  userId: string;
  token: string;
  platform: string;
  updatedAt: Date;
}

// Interface pour les préférences de notification
interface NotificationPreferences {
  statusChanges: boolean;
  newComments: boolean;
  nearbyIssues: boolean;
}

class NotificationService {
  private unsubscribeListeners: (() => void)[] = [];
  private isInitialized = false;

  // Initialiser les notifications push
  async initialize(): Promise<void> {
    if (this.isInitialized) return;
    
    try {
      // Demander permission pour les notifications locales d'abord
      const localPermStatus = await LocalNotifications.requestPermissions();
      console.log('📱 Permission notifications locales:', localPermStatus.display);
      
      // Essayer les push notifications (peut échouer sur web)
      try {
        const permStatus = await PushNotifications.requestPermissions();
        
        if (permStatus.receive === 'granted') {
          await PushNotifications.register();
          this.setupPushListeners();
          console.log('✅ Push notifications initialisées');
        } else {
          console.warn('⚠️ Permission push refusée, utilisation des notifications locales uniquement');
        }
      } catch (pushError) {
        console.warn('⚠️ Push notifications non disponibles (normal sur web):', pushError);
      }
      
      this.isInitialized = true;
      console.log('✅ Service de notifications initialisé');
      
    } catch (error) {
      console.error('❌ Erreur initialisation notifications:', error);
    }
  }

  // Configurer les listeners push
  private setupPushListeners(): void {
    PushNotifications.addListener('registration', async (token) => {
      console.log('📱 Token FCM reçu:', token.value);
      await this.saveTokenToFirestore(token.value);
    });

    PushNotifications.addListener('registrationError', (error) => {
      console.error('❌ Erreur enregistrement push:', error);
    });

    PushNotifications.addListener('pushNotificationReceived', (notification) => {
      console.log('📬 Notification push reçue:', notification);
      this.showLocalNotification(notification.title || 'Notification', notification.body || '');
    });

    PushNotifications.addListener('pushNotificationActionPerformed', (action) => {
      console.log('👆 Action notification:', action);
      this.handleNotificationAction(action);
    });
  }

  // Sauvegarder le token FCM dans Firestore
  private async saveTokenToFirestore(token: string): Promise<void> {
    const currentUser = auth.currentUser;
    if (!currentUser) return;

    const tokenData = {
      userId: currentUser.uid,
      token: token,
      platform: this.getPlatform(),
      updatedAt: Timestamp.now()
    };

    await setDoc(doc(db, 'user_tokens', currentUser.uid), tokenData);
    console.log('✅ Token sauvegardé dans Firestore');
  }

  private getPlatform(): string {
    const userAgent = navigator.userAgent.toLowerCase();
    if (userAgent.includes('android')) return 'android';
    if (userAgent.includes('iphone') || userAgent.includes('ipad')) return 'ios';
    return 'web';
  }

  private handleNotificationAction(action: any): void {
    const data = action.notification.data;
    if (data?.issueId) {
      window.location.href = `/issue/${data.issueId}`;
    }
  }

  // Utiliser Capacitor Preferences au lieu de localStorage
  private async getStoredStatus(key: string): Promise<string | null> {
    try {
      const { value } = await Preferences.get({ key });
      return value;
    } catch {
      // Fallback sur localStorage pour le web
      return localStorage.getItem(key);
    }
  }

  private async setStoredStatus(key: string, value: string): Promise<void> {
    try {
      await Preferences.set({ key, value });
    } catch {
      // Fallback sur localStorage pour le web
      localStorage.setItem(key, value);
    }
  }

  // Écouter les changements de statut des signalements de l'utilisateur
  async startListeningToMyIssues(): Promise<void> {
    const currentUser = auth.currentUser;
    if (!currentUser) {
      console.warn('⚠️ Utilisateur non connecté, impossible d\'écouter les signalements');
      return;
    }

    console.log('👂 Début de l\'écoute des signalements pour:', currentUser.uid);

    // Écouter les signalements dans 'signals'
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

    // Écouter les signalements dans 'road_issues'
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

  // Vérifier et notifier le changement de statut
  private async checkStatusChange(
    issueId: string,
    newData: any,
    collectionName: string
  ): Promise<void> {
    const cacheKey = `issue_status_${collectionName}_${issueId}`;
    const previousStatus = await this.getStoredStatus(cacheKey);
    const currentStatus = (newData.statusId?.toString() || newData.status || '').toString();

    console.log(`🔍 Vérification statut ${issueId}: ancien=${previousStatus}, nouveau=${currentStatus}`);

    if (previousStatus && previousStatus !== currentStatus && currentStatus) {
      console.log(`🔔 Changement détecté pour ${issueId}!`);
      await this.sendLocalNotification(newData, previousStatus, currentStatus);
    }

    // Mettre à jour le cache
    if (currentStatus) {
      await this.setStoredStatus(cacheKey, currentStatus);
    }
  }

  // Afficher une notification locale
  private async showLocalNotification(title: string, body: string, data?: any): Promise<void> {
    try {
      await LocalNotifications.schedule({
        notifications: [
          {
            id: Date.now(),
            title: title,
            body: body,
            extra: data,
            smallIcon: 'ic_notification',
            largeIcon: 'ic_notification',
            sound: 'default'
          }
        ]
      });
      console.log('✅ Notification locale affichée:', title);
    } catch (error) {
      console.error('❌ Erreur notification locale:', error);
    }
  }

  // Envoyer une notification locale pour changement de statut
  private async sendLocalNotification(
    issueData: any,
    oldStatus: string,
    newStatus: string
  ): Promise<void> {
    const statusLabels: Record<string, string> = {
      '1': 'Signalé',
      '2': 'En cours',
      '3': 'Résolu',
      '4': 'Rejeté',
      'signale': 'Signalé',
      'en_cours': 'En cours',
      'resolu': 'Résolu',
      'rejete': 'Rejeté'
    };

    const oldLabel = statusLabels[oldStatus] || oldStatus;
    const newLabel = statusLabels[newStatus] || newStatus;

    const title = '📢 Statut mis à jour';
    const body = `"${issueData.title || 'Signalement'}" : ${oldLabel} → ${newLabel}`;

    await this.showLocalNotification(title, body, { issueId: issueData.id });

    // Sauvegarder dans l'historique
    await this.saveNotificationToHistory(issueData, oldLabel, newLabel);
  }

  // Sauvegarder la notification dans l'historique Firestore
  private async saveNotificationToHistory(
    issueData: any,
    oldStatus: string,
    newStatus: string
  ): Promise<void> {
    const currentUser = auth.currentUser;
    if (!currentUser) return;

    try {
      const notificationData = {
        userId: currentUser.uid,
        issueId: issueData.id || '',
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

  // Arrêter les listeners
  stopListening(): void {
    this.unsubscribeListeners.forEach(unsubscribe => unsubscribe());
    this.unsubscribeListeners = [];
    console.log('🛑 Écoute des signalements arrêtée');
  }
}

export const notificationService = new NotificationService();