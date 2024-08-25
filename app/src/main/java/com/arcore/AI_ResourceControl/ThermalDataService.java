package com.arcore.AI_ResourceControl;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;


public class ThermalDataService extends Service {
    private static final String TAG = "ThermalDataService";
    private static final String CHANNEL_ID = "ThermalDataServiceChannel";
    private Thread thermalThread;
    private Socket main_socket;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        ThermalDataService getService() {
            // Return this instance of ThermalDataService so clients can call public methods
            return ThermalDataService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public Socket getMainSocket() {
        return main_socket;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();

        String server_IP_address = intent.getStringExtra("server_IP_address");
        int server_PORT = intent.getIntExtra("server_PORT", 0);

        thermalThread = new Thread(() -> {
            try {
                Log.d(TAG, "Trying to connect to server");
                main_socket = new Socket(server_IP_address, server_PORT);

                if (main_socket != null && main_socket.isConnected()) {
                    Log.d(TAG, "Connected to server");
                    ThermalDataCollectionRunnable runnable = new ThermalDataCollectionRunnable(main_socket);
                    runnable.run();
                } else {
                    Log.e(TAG, "Failed to connect to server");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to server", e);
            }
        });
        thermalThread.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (main_socket != null && !main_socket.isClosed()) {
                main_socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
        if (thermalThread != null && !thermalThread.isInterrupted()) {
            thermalThread.interrupt();
        }
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Thermal Data Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Thermal Data Service")
                .setContentText("Collecting thermal data in the background")
                .setSmallIcon(R.drawable.ic_notification)
                .build();

        startForeground(1, notification);
    }
}
