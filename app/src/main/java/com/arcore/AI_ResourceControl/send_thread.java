package com.arcore.AI_ResourceControl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import android.content.Context;
import android.os.Environment;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class send_thread extends Thread {
    //@Override
 double reward;
    public send_thread(double reward_inp) {
        this.reward = reward_inp;

    }


    public void run() {
        // Perform socket connection here
        try {
            Socket socket = new Socket("192.168.1.42", 4444);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //send reward result
            //int reward=2000;
            String msg_toserver="sending_rewards:"+reward;

            out.println(msg_toserver);
            //flush stream
            out.flush();

            while (!((new String(in.readLine())).equals("File received"))) ;// when file is recieved, we close the socket

            out.close();
            socket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        // Do something with the socket (e.g., read/write data)


    }

    public static void main(String[] args) {
        // Create and start the thread for socket connection
//        send_thread connectionThread = new send_thread();
//        connectionThread.start();
    }
}
