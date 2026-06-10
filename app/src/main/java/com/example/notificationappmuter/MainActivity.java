package com.example.notificationappmuter;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Button btnStartTime, btnEndTime, btnSave, btnPermission, btnAppDetails, btnRestartService, btnTestNotify, btnSelectAll, btnUnselectAll;
    private TextView tvServiceStatus, tvDebugLogs, tvSelectAppsLabel;
    private android.widget.EditText etSearch;
    private CheckBox[] dayCheckBoxes;
    private RecyclerView rvApps;
    private AppAdapter adapter;
    private List<AppInfo> installedApps = new ArrayList<>();

    private int startHour = 0, startMinute = 0;
    private int endHour = 0, endMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPermission = findViewById(R.id.btnPermission);
        btnAppDetails = findViewById(R.id.btnAppDetails);
        btnRestartService = findViewById(R.id.btnRestartService);
        btnTestNotify = findViewById(R.id.btnTestNotify);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        btnSave = findViewById(R.id.btnSave);
        tvServiceStatus = findViewById(R.id.tvServiceStatus);
        tvDebugLogs = findViewById(R.id.tvDebugLogs);
        rvApps = findViewById(R.id.rvApps);
        tvSelectAppsLabel = findViewById(R.id.tvSelectAppsLabel);
        etSearch = findViewById(R.id.etSearch);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnUnselectAll = findViewById(R.id.btnUnselectAll);

        dayCheckBoxes = new CheckBox[]{
                findViewById(R.id.cbMon), findViewById(R.id.cbTue),
                findViewById(R.id.cbWed), findViewById(R.id.cbThu),
                findViewById(R.id.cbFri), findViewById(R.id.cbSat),
                findViewById(R.id.cbSun)
        };

        btnPermission.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        });

        btnAppDetails.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            android.net.Uri uri = android.net.Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
            Toast.makeText(this, "Kliknij 3 kropki w prawym górnym rogu i wybierz 'Zezwalaj na ustawienia ograniczone'", Toast.LENGTH_LONG).show();
        });

        btnRestartService.setOnClickListener(v -> {
            restartNotificationListener();
        });

        btnTestNotify.setOnClickListener(v -> {
            updateLogs("UI: Requesting test notification...");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                } else {
                    sendTestNotification();
                }
            } else {
                sendTestNotification();
            }
        });

        btnStartTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                startHour = hourOfDay;
                startMinute = minute;
                btnStartTime.setText(String.format(Locale.getDefault(), "Start: %02d:%02d", hourOfDay, minute));
            }, startHour, startMinute, true);
            timePickerDialog.show();
        });

        btnEndTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                endHour = hourOfDay;
                endMinute = minute;
                btnEndTime.setText(String.format(Locale.getDefault(), "End: %02d:%02d", hourOfDay, minute));
            }, endHour, endMinute, true);
            timePickerDialog.show();
        });

        tvDebugLogs.setOnLongClickListener(v -> {
            tvDebugLogs.setText("Logs cleared...");
            return true;
        });

        adapter = new AppAdapter(installedApps);
        adapter.setOnSelectionChangedListener(count -> {
            tvSelectAppsLabel.setText("Select Apps (" + count + "):");
        });
        rvApps.setLayoutManager(new LinearLayoutManager(this));
        rvApps.setAdapter(adapter);

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnSelectAll.setOnClickListener(v -> {
            adapter.selectFiltered(true);
        });

        btnUnselectAll.setOnClickListener(v -> {
            adapter.selectFiltered(false);
        });

        loadInstalledApps();

        btnSave.setOnClickListener(v -> saveSettings());
        
        loadSettings();
        checkServiceStatus();
        registerLogReceiver();
    }

    private void updateLogs(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        String logLine = "[" + timestamp + "] " + message;
        String currentText = tvDebugLogs.getText().toString();
        if (currentText.startsWith("System logs")) currentText = "";
        String newText = logLine + "\n" + currentText;
        if (newText.length() > 5000) newText = newText.substring(0, 5000);
        tvDebugLogs.setText(newText);
    }

    private final BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String log = intent.getStringExtra("log");
            if (log != null) {
                String currentText = tvDebugLogs.getText().toString();
                if (currentText.startsWith("System logs")) currentText = "";
                String newText = log + "\n" + currentText;
                if (newText.length() > 5000) newText = newText.substring(0, 5000);
                tvDebugLogs.setText(newText);
            }
        }
    };

    private void registerLogReceiver() {
        IntentFilter filter = new IntentFilter("com.example.notificationappmuter.DEBUG_LOG");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(logReceiver, filter);
        }
        updateLogs("UI: Log receiver registered");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(logReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkServiceStatus();
    }

    private void sendTestNotification() {
        android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        String channelId = "test_channel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "Test", android.app.NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }
        android.app.Notification notification = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setContentTitle("Test Muter")
                .setContentText("Check if service sees this!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        nm.notify(123, notification);
        Toast.makeText(this, "Test Notification Sent", Toast.LENGTH_SHORT).show();
    }

    private void restartNotificationListener() {
        android.content.ComponentName componentName = new android.content.ComponentName(this, NotificationMuterService.class);
        android.content.pm.PackageManager pm = getPackageManager();
        
        // Wyłączamy i włączamy komponent usługi - to wymusza na Androidzie ponowne bindowanie
        pm.setComponentEnabledSetting(componentName, 
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
                android.content.pm.PackageManager.DONT_KILL_APP);
        
        pm.setComponentEnabledSetting(componentName, 
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 
                android.content.pm.PackageManager.DONT_KILL_APP);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            NotificationMuterService.requestRebind(componentName);
        }
        
        Toast.makeText(this, "Service Refreshed! Wait for 'Usługa POŁĄCZONA' in logs.", Toast.LENGTH_LONG).show();
    }

    private void checkServiceStatus() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean isEnabled = enabledListeners != null && enabledListeners.contains(getPackageName());
        if (isEnabled) {
            tvServiceStatus.setText("Service Status: ACTIVE");
            tvServiceStatus.setTextColor(android.graphics.Color.GREEN);
        } else {
            tvServiceStatus.setText("Service Status: INACTIVE (Grant Access!)");
            tvServiceStatus.setTextColor(android.graphics.Color.RED);
        }
    }

    private void loadInstalledApps() {
        updateLogs("UI: Loading apps...");
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppInfo> tempApps = new ArrayList<>();
            for (ApplicationInfo packageInfo : packages) {
                CharSequence label = pm.getApplicationLabel(packageInfo);
                if (label != null) {
                    tempApps.add(new AppInfo(
                            label.toString(),
                            packageInfo.packageName,
                            pm.getApplicationIcon(packageInfo)
                    ));
                }
            }
            tempApps.sort((a, b) -> a.name.compareToIgnoreCase(b.name));

            runOnUiThread(() -> {
                installedApps.clear();
                installedApps.addAll(tempApps);
                
                // Re-apply selection from settings after loading
                SharedPreferences prefs = getSharedPreferences("MuterPrefs", MODE_PRIVATE);
                Set<String> selectedPackages = prefs.getStringSet("selectedApps", new HashSet<>());
                for (AppInfo app : installedApps) {
                    if (selectedPackages.contains(app.packageName)) {
                        app.isSelected = true;
                    }
                }
                
                adapter.sortBySelection();
                updateLogs("UI: Apps loaded: " + installedApps.size());
            });
        }).start();
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences("MuterPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("startHour", startHour);
        editor.putInt("startMinute", startMinute);
        editor.putInt("endHour", endHour);
        editor.putInt("endMinute", endMinute);

        StringBuilder days = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (dayCheckBoxes[i].isChecked()) {
                days.append(i + 1).append(","); 
            }
        }
        editor.putString("muteDays", days.toString());

        Set<String> selectedPackages = new HashSet<>();
        for (AppInfo app : installedApps) {
            if (app.isSelected) {
                selectedPackages.add(app.packageName);
            }
        }
        editor.putStringSet("selectedApps", selectedPackages);
        editor.commit(); // Zmieniamy na commit(), aby mieć pewność zapisu przed wyjściem

        Toast.makeText(this, "Settings Saved - Restarting Service...", Toast.LENGTH_SHORT).show();
        
        adapter.sortBySelection();
        // Odświeżenie usługi (wyłączenie i włączenie nasłuchiwania - symulowane)
        // W systemie Android 13+ najlepiej zrestartować uprawnienie ręcznie, 
        // ale wymuszenie zapisu commit() pomoże usłudze odczytać nowe dane.
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("MuterPrefs", MODE_PRIVATE);
        startHour = prefs.getInt("startHour", 0);
        startMinute = prefs.getInt("startMinute", 0);
        endHour = prefs.getInt("endHour", 0);
        endMinute = prefs.getInt("endMinute", 0);

        btnStartTime.setText(String.format(Locale.getDefault(), "Start: %02d:%02d", startHour, startMinute));
        btnEndTime.setText(String.format(Locale.getDefault(), "End: %02d:%02d", endHour, endMinute));

        String days = prefs.getString("muteDays", "");
        for (int i = 0; i < 7; i++) {
            dayCheckBoxes[i].setChecked(days.contains(String.valueOf(i + 1)));
        }
        // App selections are now handled in loadInstalledApps after the background thread finishes
    }
}