import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'io.ionic.starter',
  appName: 'photo-gallery',
  webDir: 'dist',
  plugins: {
    LocalNotifications: {
      smallIcon: "ic_launcher_foreground",
      iconColor: "#2196F3",
      sound: "default"
    },
    PushNotifications: {
      presentationOptions: ["badge", "sound", "alert"]
    }
  }
};

export default config;
