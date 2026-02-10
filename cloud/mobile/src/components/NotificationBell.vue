<template>
  <div class="notification-bell" @click="showNotifications = true">
    <ion-icon :icon="notificationsOutline"></ion-icon>
    <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
  </div>
  
  <ion-modal :is-open="showNotifications" @didDismiss="showNotifications = false">
    <ion-header>
      <ion-toolbar>
        <ion-title>Notifications</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="markAllAsRead" v-if="unreadCount > 0">
            Tout lire
          </ion-button>
          <ion-button @click="showNotifications = false">
            <ion-icon :icon="closeOutline"></ion-icon>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>
    
    <ion-content>
      <ion-list v-if="notifications.length > 0">
        <ion-item 
          v-for="notif in notifications" 
          :key="notif.id"
          :class="{ unread: !notif.read }"
          @click="openNotification(notif)"
        >
          <ion-avatar slot="start">
            <div class="notif-icon" :style="{ backgroundColor: getStatusColor(notif.newStatus) }">
              <ion-icon :icon="getStatusIcon(notif.newStatus)"></ion-icon>
            </div>
          </ion-avatar>
          <ion-label>
            <h3>{{ notif.issueTitle }}</h3>
            <p>Statut: {{ notif.oldStatus }} → {{ notif.newStatus }}</p>
            <p class="time">{{ formatTime(notif.createdAt) }}</p>
          </ion-label>
          <ion-icon 
            v-if="!notif.read" 
            :icon="ellipse" 
            color="primary" 
            slot="end"
          ></ion-icon>
        </ion-item>
      </ion-list>
      
      <div v-else class="empty-state">
        <ion-icon :icon="notificationsOffOutline"></ion-icon>
        <p>Aucune notification</p>
      </div>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import {
  IonModal, IonHeader, IonToolbar, IonTitle, IonContent,
  IonList, IonItem, IonLabel, IonAvatar, IonIcon, IonButton, IonButtons
} from '@ionic/vue';
import { 
  notificationsOutline, closeOutline, ellipse, 
  notificationsOffOutline, checkmarkCircleOutline,
  alertCircleOutline, timeOutline, closeCircleOutline
} from 'ionicons/icons';
import { collection, query, where, orderBy, onSnapshot, updateDoc, doc, writeBatch } from 'firebase/firestore';
import { db, auth } from '@/config/firebase';

interface Notification {
  id: string;
  issueId: string;
  issueTitle: string;
  type: string;
  oldStatus: string;
  newStatus: string;
  read: boolean;
  createdAt: any;
}

const showNotifications = ref(false);
const notifications = ref<Notification[]>([]);
let unsubscribe: (() => void) | null = null;

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length);

const getStatusColor = (status: string): string => {
  const colors: Record<string, string> = {
    'Signalé': '#ff9800',
    'En cours': '#2196f3',
    'Résolu': '#4caf50',
    'Rejeté': '#9e9e9e'
  };
  return colors[status] || '#666';
};

const getStatusIcon = (status: string): string => {
  const icons: Record<string, string> = {
    'Signalé': alertCircleOutline,
    'En cours': timeOutline,
    'Résolu': checkmarkCircleOutline,
    'Rejeté': closeCircleOutline
  };
  return icons[status] || alertCircleOutline;
};

const formatTime = (timestamp: any): string => {
  if (!timestamp?.toDate) return '';
  const date = timestamp.toDate();
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  
  if (diff < 60000) return 'À l\'instant';
  if (diff < 3600000) return `Il y a ${Math.floor(diff / 60000)} min`;
  if (diff < 86400000) return `Il y a ${Math.floor(diff / 3600000)} h`;
  return date.toLocaleDateString('fr-FR');
};

const openNotification = async (notif: Notification) => {
  // Marquer comme lu
  if (!notif.read) {
    await updateDoc(doc(db, 'notifications', notif.id), { read: true });
  }
  
  showNotifications.value = false;
  
  // Naviguer vers le signalement
  // router.push(`/issue/${notif.issueId}`);
};

const markAllAsRead = async () => {
  const batch = writeBatch(db);
  notifications.value
    .filter(n => !n.read)
    .forEach(n => {
      batch.update(doc(db, 'notifications', n.id), { read: true });
    });
  await batch.commit();
};

onMounted(() => {
  const currentUser = auth.currentUser;
  if (!currentUser) return;

  const q = query(
    collection(db, 'notifications'),
    where('userId', '==', currentUser.uid),
    orderBy('createdAt', 'desc')
  );

  unsubscribe = onSnapshot(q, (snapshot) => {
    notifications.value = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    } as Notification));
  });
});

onBeforeUnmount(() => {
  if (unsubscribe) unsubscribe();
});
</script>

<style scoped>
.notification-bell {
  position: relative;
  cursor: pointer;
  padding: 8px;
}

.notification-bell ion-icon {
  font-size: 24px;
}

.badge {
  position: absolute;
  top: 0;
  right: 0;
  background: #f44336;
  color: white;
  font-size: 10px;
  font-weight: bold;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.unread {
  background: #e3f2fd;
}

.notif-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.notif-icon ion-icon {
  font-size: 20px;
}

ion-label h3 {
  font-weight: 600;
  font-size: 14px;
}

ion-label p {
  font-size: 13px;
  color: #666;
}

ion-label .time {
  font-size: 11px;
  color: #999;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state ion-icon {
  font-size: 64px;
  margin-bottom: 16px;
}
</style>