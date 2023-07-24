package server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Arrays;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.*;
import  java.text.SimpleDateFormat;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.text.ParseException;
import java.text.SimpleDateFormat;




/**
 *
 *	Server thread handling single connection
 */
public class
ServerThread extends Thread {
	private final Socket socket;
	//download chunk size, make sure to match on ModelRequestRunnable
	private final int BUFFER_SIZE = 200;
	server.ARServer ser;
	public ServerThread(Socket socket, server.ARServer server) {
		this.socket = socket;
		this.ser=server;
	}


	//run a thread to handle a connection
	public void run() {
		try {


//input from python clinet startw with "input_params"

//			BufferedWriter writer = null;
//			writer = new BufferedWriter(new OutputStreamWriter(
//					new FileOutputStream("Latency.txt", true)));

			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("start reading: " );
			//recieve percent reduction in 0.0 to 1.0 format

			String value;
			value= in.readLine();

			String client_src="";
			if (value!= null) {

				System.out.println("value: " + value);

				//if(value.length()>10) {
					if(value.contains("python"))	{
					client_src = value.substring(2, 15);
					System.out.println("msg from: " + client_src);
					int startIndex = value.indexOf("[");
					int endIndex = value.indexOf("]");

					// Extract the list substring from the input String
					String listStr = value.substring(startIndex, endIndex + 1);
					System.out.println("listStr: " + listStr);

				/* This is for java client to convert the String ary to Double Ary

				// Split the list substring by commas and convert the values to doubles
				String[] elements = listStr.substring(1, listStr.length() - 1).split(",");
				// Create a double array to store the converted elements
				double[] doubleArray = new double[elements.length];

				// Convert the elements to double and store them in the double array
				for (int i = 0; i < elements.length; i++) {
					doubleArray[i] = Double.parseDouble(elements[i].trim());
				}

				// Print the double array
				for (double x : doubleArray) {
					System.out.println(x);
				}*/
				}
			else
				     value=in.readLine();// this is the percentage of decimation

			}


			System.out.println("before running python client or " +client_src);
			String command;
			if (!client_src.equals( "python_client"))// we only run python cient in two ways: either through the code here, or we run it manually through spyder
			{

				System.out.println("Bayesian optimization is started");

			command = "./bin/python python_client.py";
			System.out.println(command);
			Process process;
			process = Runtime.getRuntime().exec(command);
			long startT1 = System.currentTimeMillis();
			process.waitFor();
			long endT1 = System.currentTimeMillis();
			System.out.println(" execution time : " + (endT1 - startT1) + " milliseconds");
		}

			else
				System.out.println("This is response from java client to give you reward");



			///Write to python client thr reward result
			File  out_file;
			//for decimated
			out_file = new File("./result.txt");

			FileInputStream outputFile = new FileInputStream(out_file);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			OutputStream socketOutputStream = socket.getOutputStream();
			//wait for input stream of file
			int read;
			byte[] buffer = new byte[160000];

			//write chunks of bytes
			int writingtime=0;
			while ((read = outputFile.read(buffer)) != -1) {
				long startTime = System.currentTimeMillis();
				socketOutputStream.write(buffer, 0, read);
				long endTime = System.currentTimeMillis();
				System.out.println("socketOutputStream buffer [" + read + " bytes] write time 1: " + (endTime - startTime) + " milliseconds");
				//System.out.println("Sending " + out_file + "(" + mybytearray.length + " bytes)");
				writingtime+= (endTime - startTime);
				socketOutputStream.flush();

			}
			out.flush();



		} catch (Exception e) {
			System.out.println(e.getMessage());

		}



	}


}



