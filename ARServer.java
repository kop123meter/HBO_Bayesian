package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//import android.content.Intent;


//import android.util.Log;

public class ARServer {


	private static final int KEEP_ALIVE_TIME = 10;
	private final int CORE_THREAD_POOL_SIZE = 10;

	private final int MAX_THREAD_POOL_SIZE = 10;

	private final TimeUnit KEEP_ALIVE_TIME_UNIT;

	//Default server port
	private static final int DEFAULT_PORT = 4444;
	//Maximum port since we are using ServetSocket
	private final BlockingQueue<Runnable> mWorkQueue;


	private static final int MAXIMUM_PORT = 4444;
	//Socket receiving connections
	private final ServerSocket serverSocket;
	//number of clients connected to the server
	static int number_of_clients = 0;
	//List<ServerThread> current_thread = new ArrayList<>();

	//	static {
//		try
//
//	{
//		private static ARServer Instance = new ARServer(DEFAULT_PORT);
//
//
//	} catch(IOException e)
//
//	{
//		e.printStackTrace();
//	}
//
//}
	private static ARServer Instance;
	static
	{
		try {

			Instance = new ARServer(4444);
			//Instance = new ARServer(DEFAULT_PORT);


		} catch (IOException e) {
			e.printStackTrace();}


	}

	private final ThreadPoolExecutor mDownloadThreadPool;





	public static ARServer getInstance()
	{
		return Instance;
	}
	//Socket clientSocket= new ServerSocket;
	/**
	 * Make a ARServer listening on port
	 * @param port: port on which server is listening (has to be less than 65535)
	 * @throws IOException is thrown if an errors creating the server socket
	 */
	public ARServer(int port) throws IOException {
		serverSocket= new ServerSocket(port);
		KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
		mWorkQueue = new LinkedBlockingQueue<Runnable>();
		mDownloadThreadPool = new ThreadPoolExecutor(CORE_THREAD_POOL_SIZE, MAX_THREAD_POOL_SIZE,
				KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mWorkQueue);




	}

	/**
	 * Run the server, listening for new request
	 * on success a new thread will be created handling the individual connection
	 *
	 * @throws IOException
	 */

	public  void serve(   ) throws IOException {

		while(true) {


			try {

				Socket clientSocket = serverSocket.accept();
				number_of_clients++;
				System.out.println("New connection created: " + clientSocket);
				System.out.println("Number of clients: " + number_of_clients);

// this is to start auto decimation

				Instance.mDownloadThreadPool.execute(new server.ServerThread(clientSocket,Instance));
			//	Instance.mDownloadThreadPool.execute(new offloading(clientSocket,Instance));
// this is to start offloading




			}
			catch(IOException e) {
				System.out.println("Exception found on accept connection");
				System.out.println(e.getMessage());
				e.printStackTrace();
			}


		}





	}

	public static void main(String[] args) throws IOException, InterruptedException {
		ARServer server;
		//String [] server_task=new String[]{"AutoDecimate","Offloading"};


		try {
			server=Instance;
			System.out.println( " Running");

			//temp commented
			 server.serve();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}