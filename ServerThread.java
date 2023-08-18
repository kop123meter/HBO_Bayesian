package server;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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
	private  Socket socket;
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

			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("start reading: " );
			//recieve percent reduction in 0.0 to 1.0 format

			String value;
			value= in.readLine();

			String client_src="";
			if (value!= null) {

				System.out.println("Message: " + value);

//$$$$$$$$$$$$$ PYTHON CLIENT and Server Communicatoin  $$$$$$$$$$$$$
//$$$$$$$$$$$$$ This is not tested yet  $$$$$$$$$$$$$
					if(value.contains("python"))	{// this is a msg from python client
						// this is step 1:
						// 1.1: receive ary of delegate and triangles from python client
						// 2.1: send the received string to java client
						System.out.println("From python client to server " );
					client_src = value.substring(2, 15);
					System.out.println("msg from: " + client_src);
					int startIndex = value.indexOf("[");
					int endIndex = value.indexOf("]");

					// Extract the list substring from the input String
					String listStr=	value.substring(startIndex+1, endIndex );
						listStr = "dlg_end";
					System.out.println("listStr: " + listStr);


						byte[] buffer = new byte[160000];
						buffer=listStr.getBytes();
						// this is to write what we got from python to the phone


						// temp test niloo to see if I can reconnect the java app client
						socket = ARServer.app_socket;
						// Set clientSocket1 to use IP address "192.168.0.1"
//						InetAddress ipAddress1 = InetAddress.getByName("192.168.1.172");
//						InetSocketAddress serverAddress1 = new InetSocketAddress(ipAddress1, 4444);
//						socket.connect(serverAddress1);


						System.out.println("writing input gained from python client to the java client " );
						OutputStream socketOutputStream = socket.getOutputStream();
						socketOutputStream.write(buffer, 0, buffer.length);
						socketOutputStream.flush();

						System.out.println("File is sent, now waiting for delivery msg:" );

// after any "write" to client, we need to get signal from the client that file is received!
						while (!((new String(in.readLine())).equals("File received"))) ;
						//close all stream
// this is after any socket usage!

						socketOutputStream.close();
						socket.close();
						//decrement the number of clients
						ARServer.number_of_clients--;
						System.out.println(" Delivery MSG from java client!");
						///$$$$$$$********** I'm here as of now: Write to JAVA client the selected delegate from python



				}

//$$$$$$$$$$$$$ PYTHON CLIENT and Server Communicatoin  $$$$$$$$$$$$$
//$$$$$$$$$$$$$ PYTHON CLIENT and Server Communicatoin  $$$$$$$$$$$$$


//$$$$$$$$$$$$$ Java CLIENT and Server Communicatoin  $$$$$$$$$$$$$
				else if(value.contains("delegate_req")){// this is requesting to get the delegate input
						System.out.println("From Java client to server " );
						File  out_file;
						byte[] buffer = new byte[160000];

						//read from java client and write to the socket
						out_file = new File("./java_client.txt");
						FileInputStream outputFile = new FileInputStream(out_file);
						PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
						OutputStream socketOutputStream = socket.getOutputStream();
						int read;
						while ((read = outputFile.read(buffer)) != -1) {
							long startTime = System.currentTimeMillis();
							socketOutputStream.write(buffer, 0, read);
							long endTime = System.currentTimeMillis();
							System.out.println("socketOutputStream buffer [" + read + " bytes] write time 1: " + (endTime - startTime) + " milliseconds");
							socketOutputStream.flush();

						}
						//send end of message
						out.print("dlg_end");
						out.flush();
// after any "write" to client, we need to get signal from the client that file is received!
						while (!((new String(in.readLine())).equals("File received"))) ;
						//close all stream
// this is after any socket usage!
						out.close();
						in.close();
						outputFile.close();
						socketOutputStream.close();
						socket.close();
						//decrement the number of clients
						ARServer.number_of_clients--;
						System.out.println("File delivery MSG is received!");
					}

					else if(value.contains( "sending_rewards")){// this is reward fro server, we need to read and wriite it to python client

						System.out.println("This is reward from java client to the server " );
						System.out.println(value );
						String reward = value.substring(16, value.length());
						System.out.println("reward from client is: " + reward);

						PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
						out.print("File received");
						out.flush();


				}

//$$$$$$$$$$$$$ Java CLIENT and Server Communicatoin  $$$$$$$$$$$$$



				else// this is decimate
					     value=in.readLine();// this is the percentage of decimation
				// or is when we test direct input dlegate from the server


			}

/* This is to run python_client.py automatically, this is for the last part
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
				///Write to python client the reward result
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
			}

			else {
				System.out.println("This is response from java client to give you reward");
			}
*/



		} catch (Exception e) {
			System.out.println(e.getMessage());

		}



	}


}



