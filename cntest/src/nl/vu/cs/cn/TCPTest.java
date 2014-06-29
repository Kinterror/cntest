package nl.vu.cs.cn;

import java.io.IOException;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCP.Socket;
import android.test.AndroidTestCase;
import android.util.Log;

public class TCPTest extends AndroidTestCase{

	private boolean doSimultaneousClosing = false, 
			doReuseSocketSv = false,
			doReuseSocketCl = false;
	
	private class Client implements Runnable {
		TCP clientStack;
		Socket clientSocket;
		
		public void run() {
			try {
				Log.d("testConnect","threadClient creates a new TCP stack with the address 192.168.0.10 .");
				clientStack = new TCP(10);
				clientSocket = clientStack.socket();
				Log.d("testConnect", "Client tries to connect on "+ IpAddress.getAddress("192.168.0."+20));
				if (!clientSocket.connect(IpAddress.getAddress("192.168.0."+20), 4444))
					fail("unable to connect.");
				
				String message = "Hello World";
				byte[] messageBytes = message.getBytes();
				int messageLength = message.length();
				Log.d("clientThread", "Client writes into the buffer");
				clientSocket.write(messageBytes, 0, messageLength);
				
				//Client tries to read the answer from the server
				byte[] buffer= new byte[12];
				Log.d("clientThread", "Client reads into the buffer");
				int len = clientSocket.read(buffer, 0, 8);
				byte[] tmp = new byte[len];
				for (int i = 0; i < tmp.length; i++) {
					tmp[i] = buffer[i];
				}
				assertEquals("How are ", new String(tmp));
				
				//Closing connection
				Log.d("Client", "Client closes the connection");
				assertTrue(clientSocket.close());
				int offset = 0;
				while ((len = clientSocket.read(buffer, offset, 5)) == 5) {
					offset += len;
				}
				Thread.sleep(20000);
				
				//maybe reuse the socket
				if (doReuseSocketCl){
					assertTrue(clientSocket.connect(IpAddress.getAddress("192.168.0."+20), 4444));
					
					String str = "test";
					clientSocket.write(str.getBytes(), 0, 4);
					
					assertTrue(clientSocket.close());
					buffer= new byte[12];
					assertTrue(clientSocket.read(buffer, 0, 12) == 0);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * second client
	 */
	private class Client2 implements Runnable {

		public void run() {
			TCP clientStack;
			Socket clientSocket;
			
			try {
				Log.d("testConnect","threadClient creates a new TCP stack with the address 192.168.0.30 .");
				clientStack = new TCP(30);
				clientSocket = clientStack.socket();
				Log.d("testConnect", "Client tries to connect on "+ IpAddress.getAddress("192.168.0."+20));
				if (!clientSocket.connect(IpAddress.getAddress("192.168.0."+20), 4444))
					fail("unable to connect.");
				
				String message = "cl.2";
				byte[] messageBytes = message.getBytes();
				int messageLength = message.length();
				Log.d("clientThread", "Client writes into the buffer");
				clientSocket.write(messageBytes, 0, messageLength);
				
				//Closing connection
				Log.d("Client", "Client closes the connection");
				assertTrue(clientSocket.close());
				Thread.sleep(20000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class Server implements Runnable {
		
		
		public void run() {
			TCP serverStack;
			Socket serverSocket;
			
			try {
				Log.d("testConnect","threadServer creates a new TCP stack with the address 192.168.0.20 .");
				serverStack = new TCP(20);
				serverSocket = serverStack.socket(4444);
				Log.d("testConnect", "Server starts to accept");
				serverSocket.accept();
				
				byte[] buffer= new byte[20];
				Log.d("serverThread", "Server reads into the buffer");
				
				serverSocket.read(buffer, 0, 5);
				byte[] receivedMessage= new byte[5];
				for (int i = 0; i < receivedMessage.length; i++) {
					receivedMessage[i] = buffer[i];
				}
				assertEquals("Hello", new String(receivedMessage));
				serverSocket.read(buffer, 5, 6);
				receivedMessage= new byte[11];
				for (int i = 0; i < receivedMessage.length; i++) {
					receivedMessage[i] = buffer[i];
				}
				assertEquals("Hello World", new String(receivedMessage));
				
				//The server responses with another message
				String reply = "How are you?";
				serverSocket.write(reply.getBytes(), 0, reply.length());
				
				if(!doSimultaneousClosing)
					Thread.sleep(5000);
				//closing the connection
				Log.d("Server", "Server closes the connection");
				serverSocket.close();
				Thread.sleep(15000);
				
				if (doReuseSocketSv){
					serverSocket.accept();
					byte[] result = new byte[4];
					assertTrue(serverSocket.read(result, 0, 4) == 4);
					
					assertTrue(serverSocket.close());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void init(){
		//System.setProperty("PACKET_LOSS", "50");
		Thread thdClient = new Thread(new Client());
		
		Thread thdServer = new Thread(new Server());
	
		Log.d("testConnect", "threadServ starts");
		thdServer.start();
		Log.d("testConnect", "threadClient starts");
		thdClient.start();
		try {
			thdClient.join();
			thdServer.join(60000);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail("server's join timed out.");
		}
	}
	
	public void testDefault(){
		init();
	}
	
	public void testSimultaneousClosing (){
		doSimultaneousClosing = true;
		init();
		doSimultaneousClosing = false;
	}
	
	/**
	 * test what happens if after connection, the same client tries to connect to the same server 
	 * on the same sockets after having closed the connection
	 */
	public void testSocketReuse(){
		doReuseSocketCl = doReuseSocketSv = true;
		init();
		doReuseSocketCl = doReuseSocketSv = false;
	}
	
	/**
	 * try to connect to a server. After the first one finishes, the server will listen again and a second (different)
	 * client will connect.
	 */
	public void testConnectAfterConnect(){
		doReuseSocketSv = true;
		Thread thdServer = new Thread(new Server());
		thdServer.start();
		Thread thdClient1 = new Thread(new Client());
		thdClient1.start();
		
		try {
			thdClient1.join();
		} catch (InterruptedException e) {
			fail();
		}
		Thread thdClient2 = new Thread(new Client2());
		thdClient2.start();
		
		try {
			thdClient2.join();
			thdServer.join();
		} catch (InterruptedException e) {
			fail();
		}
		
		doReuseSocketSv = false;
	}
	
	/**
	 * Test what happens if another client tries to connect while the connection is already established
	 */
	public void testMultipleClients(){
		Thread thdClient = new Thread(new Client());
		Thread thdServer = new Thread(new Server());
	
		Log.d("testConnect", "threadServ starts");
		thdServer.start();
		Log.d("testConnect", "threadClient starts");
		thdClient.start();
		
		try {
			Thread.sleep(1000);
			TCP clientStack2 = new TCP(30);
		
			Socket clientSocket2 = clientStack2.socket();
			Log.d("testConnect", "Client 2 tries to connect on "+ IpAddress.getAddress("192.168.0."+20));
			if (clientSocket2.connect(IpAddress.getAddress("192.168.0."+20), 4444))
				fail("managed to connect.");

			thdClient.join();
			thdServer.join(60000);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail("server's join timed out.");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	public void testReadWriteWithoutConnection(){
		Thread thdServer = new Thread(new Server());
		
		Thread thdClient = new Thread(new Runnable() {
			public void run(){
				TCP clientStack;
				Socket clientSocket;
				
				try {
					Log.d("testConnect","threadClient creates a new TCP stack with the address 192.168.0.30 .");
					clientStack = new TCP(30);
					clientSocket = clientStack.socket();
					
					byte[] temp = new byte[10];
					
					//read before connect should return -1
					assertEquals(-1, clientSocket.read(temp, 0, 0));
					
					//same for write
					assertEquals(-1, clientSocket.write(temp, 0, 0));
					
					//connect
					Log.d("testConnect", "Client tries to connect on "+ IpAddress.getAddress("192.168.0."+20));
					if (!clientSocket.connect(IpAddress.getAddress("192.168.0."+20), 4444))
						fail("unable to connect.");
					
					String message = "Hello World";
					byte[] messageBytes = message.getBytes();
					int messageLength = message.length();
					Log.d("clientThread", "Client writes into the buffer");
					clientSocket.write(messageBytes, 0, messageLength);
					
					//this should fail
					assertFalse(clientSocket.connect(null, 0));
					
					//this should do nothing
					clientSocket.accept();
					
					//Closing connection
					Log.d("Client", "Client closes the connection");
					assertTrue(clientSocket.close());
					
					//Write after close should fail
					assertEquals(-1, clientSocket.write(messageBytes, 0, 10));
					
					//Client tries to read the answer from the server after the connection was closed
					byte[] buffer= new byte[12];
					Log.d("clientThread", "Client reads into the buffer");
					int len = clientSocket.read(buffer, 0, 8);
					byte[] tmp = new byte[len];
					for (int i = 0; i < tmp.length; i++) {
						tmp[i] = buffer[i];
					}
					assertEquals("How are ", new String(tmp));
					
					//try to close after is has closed
					assertFalse(clientSocket.close());
					
					Thread.sleep(20000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		Log.d("testConnect", "threadServ starts");
		thdServer.start();
		thdClient.start();
		
		try {
			thdClient.join();
			thdServer.join();
		} catch (InterruptedException e) {
			fail();
		}
	}
}