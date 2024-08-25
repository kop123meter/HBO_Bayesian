package com.arcore.AI_ResourceControl;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;


public class ThermalDataCollectionRunnable implements Runnable {

//    private final Context context;
    private final Socket socket;

    private final String thermalDir = "/sys/class/thermal"; // example directory
    private final String rootAccess = "su -c"; // example root access command
    private final String topic = "thermal/";

    static final int COLLECTION_FAILED = -1;
    static final int COLLECTION_PENDING = 1;
    static final int COLLECTION_STARTED = 2;
    static final int COLLECTION_COMPLETE = 3;

    public ThermalDataCollectionRunnable( Socket socket) {
//        this.context = context;
        this.socket = socket;
    }

    @Override
    public void run() {
        Log.d("ThermalDataCollection", "Entering Runnable");
        try {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String[] thermalZonePaths = getThermalZoneFilePaths(thermalDir);

            StringBuilder msg = new StringBuilder();
            msg.append("timeStamp");
            for (String thermalZonePath : thermalZonePaths) {
                    msg.append("," + getThermalZoneType(thermalZonePath) );
            }

            out.println(topic + msg.toString() + "\n");
            out.flush();

            while (!Thread.interrupted()) {
                msg = new StringBuilder();
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                msg.append(timeStamp);
                for (String thermalZonePath : thermalZonePaths) {
                    msg.append("," + getThermalZoneTemp(thermalZonePath));
                }
                out.println(topic + msg.toString() + "\n");
                out.flush();
                Thread.sleep(100); // Wait for 100milli seconds before the next sampling
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String[] getThermalZoneFilePaths(String thermalDir) throws IOException, InterruptedException {
        String cmd = String.format("%s ls %s", rootAccess.isEmpty() ? "" : rootAccess + " ", thermalDir);
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String currFileName;
        ArrayList<String> thermalZonePaths = new ArrayList<>();
        while ((currFileName = reader.readLine()) != null) {
            if (currFileName.contains("thermal_zone")) {
                String thermalZoneFilePath = thermalDir + "/" + currFileName + "/";
                Float zoneTemp = getThermalZoneTemp(thermalZoneFilePath);
                String type = getThermalZoneType(thermalZoneFilePath);
                if (zoneTemp > 1 && // ignore ADC sensors values or unavailable sensors
                        !type.contains("cp_on_chip") &&
                        !type.contains("pca94") &&
                        !type.contains("usb") &&
                        !type.contains("gnss") &&
                        !type.contains("ISP") &&
                        !type.contains("AUR") &&
                        !type.contains("batt_")) {
                    thermalZonePaths.add(thermalZoneFilePath);
                }
            }
        }

        reader.close();
        process.waitFor();
        return thermalZonePaths.toArray(new String[0]);
    }

    private String getThermalZoneType(String thermalZonePath) throws IOException {
        String cmd = String.format("%s cat %stype", rootAccess, thermalZonePath);
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String currLine = reader.readLine();
        reader.close();
        return currLine != null ? currLine : "default_zone_type";
    }

    private Float getThermalZoneTemp(String thermalZonePath) throws IOException {
        String cmd = String.format("%s cat %stemp", rootAccess, thermalZonePath);
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String currTemp = reader.readLine();
        reader.close();
        if (currTemp != null) {
            int tmpMCValue = Integer.parseInt(currTemp);
            if (tmpMCValue < 0) tmpMCValue = 0;
            return tmpMCValue / 1000f;
        }
        return -1f;
    }
}