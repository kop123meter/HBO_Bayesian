package com.arcore.AI_ResourceControl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class ServiceBinderUtility {
    private static final String TAG = "ServiceBinderUtility";

    private Context context;
    private ThermalDataService thermalDataService;
    private boolean isBound = false;
    private ServiceBinderCallback callback;

    public ServiceBinderUtility(Context context, ServiceBinderCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    // Service connection to manage connection state
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ThermalDataService.LocalBinder binder = (ThermalDataService.LocalBinder) service;
            thermalDataService = binder.getService();
            isBound = true;
            Log.d(TAG, "Service is connected.");
            if (callback != null) {
                callback.onServiceConnected(thermalDataService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            thermalDataService = null;
            Log.d(TAG, "Service is disconnected.");
            if (callback != null) {
                callback.onServiceDisconnected();
            }
        }
    };

    // Method to bind to the service
    public void bindToService() {
        Intent intent = new Intent(context, ThermalDataService.class);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    // Method to unbind from the service
    public void unbindFromService() {
        if (isBound) {
            context.unbindService(connection);
            isBound = false;
        }
    }

    public boolean isBound() {
        return isBound;
    }

    public ThermalDataService getService() {
        return thermalDataService;
    }

    // Callback interface to notify when the service is connected or disconnected
    public interface ServiceBinderCallback {
        void onServiceConnected(ThermalDataService service);
        void onServiceDisconnected();
    }
}
