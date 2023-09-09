package server;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
public class TwoClientsServer {

	public static long startTime=0;
	public static long endTime=0;
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
	public static String fileseries=dateFormat.format(new Date());
	public static void main(String[] args) {


		int client_number=1;
// we assume here that java client has triggered an inital connection to the server, then we run python client, so we are sure
		//that client1 is java client
		try {
			ServerSocket serverSocket = new ServerSocket(4444); // Choose any available port
			System.out.println("Server is running. Waiting for clients...");

			Socket clientSocket1 = serverSocket.accept();
			System.out.println("Client1 connected: " + clientSocket1.getInetAddress().getHostAddress());

			while ( true){
			Socket clientSocket2 = serverSocket.accept();

			System.out.println("Client2_"+client_number+" connected: " + clientSocket2.getInetAddress().getHostAddress());
			// assume that client2 is python that will send input
            if (clientSocket2.getInetAddress().getHostAddress().equals( "127.0.0.1")==true)// this is python client
				{
					// this is to send, we need both directions (send&receive) here on the server
					System.out.println("is sending from Python client  ");
					pythonClientHandler client1Handler = new pythonClientHandler(clientSocket2, clientSocket1);
					client1Handler.start();

					//startTime = System.currentTimeMillis();

					javaClientHandler fromJava_sender = new javaClientHandler( clientSocket1,clientSocket2);// this is to recieve a result back from java client
					fromJava_sender.start();

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
				System.out.println("Received from pythonClient and sent to the java client " + input);
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


//		public String read(){
//
//			try {
//				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//				System.out.println("Receiveing from javaClient and sending to python: " );
//
//
//				String input = in.readLine();
//				System.out.println("Received data " + input);
//
//				return  input;
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			return null;
//		}


	}



}
