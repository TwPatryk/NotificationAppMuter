package com.example.notificationappmuter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NotificationMuterService extends NotificationListenerService {
    private static final String TAG = "MuterService";

    private void sendDebugLog(String message) {
        Log.d(TAG, message);
        Intent intent = new Intent("com.example.notificationappmuter.DEBUG_LOG");
        intent.setPackage(getPackageName()); 
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        intent.putExtra("log", "[" + timestamp + "] " + message);
        sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sendDebugLog("Service onCreate");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        
        // LOGUJEMY WSZYSTKO CO PRZYCHODZI (dla testu)
        sendDebugLog(">>> Wykryto: " + packageName);

        if (packageName.equals(getPackageName())) {
            sendDebugLog("Pominięto: własna aplikacja");
            return;
        }

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("MuterPrefs", Context.MODE_PRIVATE);
        
        // 1. Sprawdź listę aplikacji
        Set<String> selectedApps = prefs.getStringSet("selectedApps", new HashSet<>());
        if (selectedApps == null || !selectedApps.contains(packageName)) {
            sendDebugLog("Pominięto: " + packageName + " (brak na liście)");
            return;
        }

        // 2. Sprawdź dni tygodnia
        Calendar now = Calendar.getInstance();
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
        // Nasze mapowanie: 1=Mon, 2=Tue, ..., 7=Sun
        int mappedDay;
        if (dayOfWeek == Calendar.SUNDAY) mappedDay = 7;
        else mappedDay = dayOfWeek - 1;
        
        String muteDays = prefs.getString("muteDays", "");
        if (!muteDays.contains(String.valueOf(mappedDay))) {
            sendDebugLog("Pominięto: zły dzień (" + mappedDay + ", wybrane: " + muteDays + ")");
            return;
        }

        // 3. Sprawdź czas
        int startHour = prefs.getInt("startHour", 0);
        int startMinute = prefs.getInt("startMinute", 0);
        int endHour = prefs.getInt("endHour", 0);
        int endMinute = prefs.getInt("endMinute", 0);

        int nowHour = now.get(Calendar.HOUR_OF_DAY);
        int nowMinute = now.get(Calendar.MINUTE);

        int startTime = startHour * 60 + startMinute;
        int endTime = endHour * 60 + endMinute;
        int nowTime = nowHour * 60 + nowMinute;

        boolean shouldMute = false;
        if (startTime == endTime) {
            // Jeśli godziny są identyczne (np. 00:00 - 00:00), traktujemy to jako "cały czas"
            shouldMute = true;
        } else if (startTime < endTime) {
            // Klasyczny przedział, np. 08:00 - 16:00
            if (nowTime >= startTime && nowTime < endTime) shouldMute = true;
        } else {
            // Przedział nocny, np. 22:00 - 06:00
            if (nowTime >= startTime || nowTime < endTime) shouldMute = true;
        }

        if (shouldMute) {
            sendDebugLog("!!! WYCISZANIE: " + packageName);
            cancelNotification(sbn.getKey());
        } else {
            sendDebugLog("Pominięto: poza godzinami (" + String.format(Locale.getDefault(), "%02d:%02d", nowHour, nowMinute) + ")");
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        sendDebugLog("USŁUGA POŁĄCZONA!");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        sendDebugLog("USŁUGA ROZŁĄCZONA!");
    }
}