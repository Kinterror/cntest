package nl.vu.cs.cn;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCP.Socket;
import android.test.AndroidTestCase;
import android.util.Log;

public class TCPTest extends AndroidTestCase{

	TCP serverStack;
	Socket serverSocket;
	TCP clientStack;
	Socket clientSocket;
	
	private boolean doSimultaneousClosing = false, doReuseSocket = false;
	
	private void init(){
		//System.setProperty("PACKET_LOSS", "50");
		Thread thdClient = new Thread(new Runnable(){
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
					if (doReuseSocket){
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
		});

		Thread thdServer = new Thread(new Runnable() {
			public void run() {
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
					
					if (doReuseSocket){
						serverSocket.accept();
						byte[] result = new byte[4];
						assertTrue(serverSocket.read(result, 0, 4) == 4);
						
						assertTrue(serverSocket.close());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	
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
	
	public void testSocketReuse(){
		doReuseSocket = true;
		init();
		doReuseSocket = false;
	}
}