
/* This is the updated code on Dec 2023 to trigger HBO multiple times as needed by java app
 YOU need to change max_iterations based on the value in the python client*/
//to comile code use javac -d . TwoClientsServer_new.java
//everytime you rub the app, you need to start the server again using java -cp ./ server.TwoClientsServer_new
package server;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
public class TwoClientsServer_new {

	public static long startTime=0;
	public static long endTime=0;
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
	public static String fileseries=dateFormat.format(new Date());
	public static void main(String[] args) {

	int exploration=5;
	//int max_iterations=10;
	int max_iterations=3;
	int iterations=	exploration +max_iterations+1; // the last +1 is to have the system apply the best reward

		int client_number=1;// this is num of times we call HBO
// we assume here that java client has triggered an inital connection to the server, then we run python client, so we are sure
		//that client1 is java client
		try {
		  ServerSocket serverSocket = new ServerSocket(4444); // Choose any available port
		  System.out.println("Server is running. Waiting for clients...");

		  Socket clientSocket2 = serverSocket.accept();// first connect the python (run the code)
		  System.out.println("Client2_"+client_number+" connected: " + clientSocket2.getInetAddress().getHostAddress());

		  Socket clientSocket1 = serverSocket.accept(); // second connect the app (Push server Button)
		  System.out.println("Client1 connected: " + clientSocket1.getInetAddress().getHostAddress());

		  while ( true){ // this runs for each time we trigger HBO

			 if(client_number!=1)// not for the first round
			 { clientSocket2 = serverSocket.accept();
			   System.out.println("Client2_"+client_number+" connected: " + clientSocket2.getInetAddress().getHostAddress());}

			System.out.println("Server and Client are Connected");
			// assume that client2 is python that will send input
            if (clientSocket2.getInetAddress().getHostAddress().equals( "127.0.0.1")==true)// this is python client
				{
					if(client_number>1)// if we have more than one call for HBO, we need to connect to the server again since we have shutdown the connection in the prev cycle through DelegateRequestRunnable class
					{// we need to wait again for the java server to be connected to
						clientSocket1 = serverSocket.accept();
						System.out.println("Client1 connected: " + clientSocket1.getInetAddress().getHostAddress());
					}
					// this is to send, we need both directions (send&receive) here on the server
					System.out.println("is sending activation from app  ");

					javaClientHandler fromJava_sender = new javaClientHandler( clientSocket1,clientSocket2);// this is to recieve a result back from java client
					fromJava_sender.start();// activate HBO

					for (int i=0;i<iterations;i++) {//this runs up to the count of iterations in bayesian

						clientSocket2 = serverSocket.accept();// here python runs the line 95 and connects again for objective function
						System.out.println("Rcved Activation and is sending BO input from python  ");
						pythonClientHandler client1Handler = new pythonClientHandler(clientSocket2, clientSocket1);
						client1Handler.start(); // receive inputs from python and send to java client

						//if(i!=iterations-1) {// we call below lines to get the reward back to the python
						fromJava_sender = new javaClientHandler(clientSocket1, clientSocket2);
						fromJava_sender.start();//receive inputs from java client and send to python
						//}
						//client_number+=1;
					}

				}
			else
				clientSocket1=clientSocket2;// clientSocket1 is always the java client

				client_number+=1;
				//client1Socket.close();
		}


		} catch (IOException e) {
			e.printStackTrace();
		}



	}

	private static class pythonClientHandler extends Thread {// this is to recieve from pthon client and send to java client
		private final Socket clientSocket;
		private final Socket otherClientSocket;

		public pythonClientHandler(Socket clientSocket, Socket otherClientSocket) {
			this.clientSocket = clientSocket;
			this.otherClientSocket = otherClientSocket;
		}

		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				PrintWriter out = new PrintWriter(otherClientSocket.getOutputStream(), true);

				// Read input from the client
				String input = in.readLine();
				System.out.println("Received from pythonClient and sent to the java client: " + input);
				startTime = System.currentTimeMillis();
				// Send the input to the other client
				out.println(input);

			//	clientSocket.close();// shouldn't close it cause we want to recieve from java to this socket
			} catch (IOException e) {
				e.printStackTrace();
			}
		}



	}

	private static class javaClientHandler extends Thread {// this is to read from java client and write to python client
		private final Socket clientSocket;
		private final Socket otherClientSocket;

		public javaClientHandler(Socket clientSocket, Socket otherClientSocket) {
			this.clientSocket = clientSocket;
			this.otherClientSocket = otherClientSocket;
		}

		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				PrintWriter out = new PrintWriter(otherClientSocket.getOutputStream(), true);
				// Read input from the client
				System.out.println("Receiveing from javaClient and sending to python: " );

				while (!in.ready());// loop until data is ready to read
				endTime = System.currentTimeMillis();
				String input = in.readLine();
				System.out.println("Received data " + input);
				out.println(input);// send it to python!

				String data = "Execution time for reward: " + input +" : " + (endTime - startTime) + " milliseconds \n";
				System.out.println(data);
				File new_file = new File( "time"+fileseries+".txt");
				FileOutputStream fout = new FileOutputStream(new_file,true);
				fout.write(data.getBytes());
				fout.close();

				//clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}



	}



}
