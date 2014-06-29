package nl.vu.cs.cn;

import java.io.IOException;
import java.util.Arrays;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCPSegment.TCPSegmentType;
import nl.vu.cs.cn.TCPWithPacketLoss.Socket;
import android.test.AndroidTestCase;
import android.util.Log;

public class TCPWithPacketLossTest extends AndroidTestCase {
	
	TCPWithPacketLoss serverStack;
	Socket serverSocket;
	TCPWithPacketLoss clientStack;
	Socket clientSocket;
	
	/*
	 * Packets sent by this application:
	 * 	Client:
	 * 		1. SYN
	 * 		2. ACK
	 * 		3. Hello World
	 * 		4. ACK (to How Are You)
	 * 		5. FIN
	 * 		6. ACK to fin
	 * 		
	 * 	Server:
	 * 		1. SYNACK
	 * 		2. ACK (to hello world)
	 * 		3. How are you
	 * 		Asynchronous closing:
	 * 		4. ACK to fin
	 * 		5. FIN
	 * 		Synchronous Closing:
	 * 		4. FIN
	 * 		5. ACK to fin
	 * 
	 * 	Note that packets are resent, so if for example packet 1 (SYN) would be lost, packet 2 would be the retransmitted
	 *  SYN packet, not the next one.
	 *  
	 *  These arrays indicate to the TCP layer which packets should be lost by passing them to the constructor of the
	 *  TCPWithPacketLoss class. 
	 */
	
	private final int[] noloss = {};
	private final int[] synloss = {0};
	private final int[] moresynloss = {0,1,2,3,4};
	private final int[] totalLoss = {0,1,2,3,4,5,6,7,8,9};
	
	private final int[] ackloss = {1};
	private final int[] synandackloss = {0,2};
	
	private final int[] datalossClnt = {2};
	private final int[] dataAckLossServ = {1};
	
	private final int[] finLossServAsyn = {5};
	private final int[] finLossServSyn = {4};
	private final int[] ackfinLossServAsyn = {4};
	private final int[] ackfinLossServSyn = {5};
	
	private final int[] finLossClnt = {5};
	private final int[] ackFinLossClnt = {6};
	
	
	private boolean doSimultaneousClosing = false;
	
	private class Client implements Runnable {

		public void run() {
			try{
				clientSocket = clientStack.socket();
				if (!clientSocket.connect(IpAddress.getAddress("192.168.0.20"), 4444))
					if(!Arrays.equals(clientStack.lostPacketNumbers, totalLoss))
						fail("unable to connect.");
					else
						return;

				/*
				 * write hello world
				 */
				String message = "Hello World";
				byte[] messageBytes = message.getBytes();
				int messageLength = message.length();
				Log.d("clientThread", "Client writes into the buffer");
				clientSocket.write(messageBytes, 0, messageLength);

				/*
				 * read how are you
				 */
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
				clientSocket.close();
				int offset = 0;
				while ((len = clientSocket.read(buffer, offset, 5)) == 5) {
					offset += len;
				}
				Thread.sleep(20000);
				assertFalse(clientStack.hasFailed);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class Server implements Runnable {
		public void run() {
			try{
				
				/*
				 * accept
				 */
				serverSocket = serverStack.socket(4444);
				if (!serverSocket.accept())
					fail("unable to accept.");
				
				/*
				 * read "hello"
				 */
				byte[] buffer= new byte[20];
				Log.d("serverThread", "Server reads into the buffer");
				
				serverSocket.read(buffer, 0, 5);
				byte[] receivedMessage= new byte[5];
				for (int i = 0; i < receivedMessage.length; i++) {
					receivedMessage[i] = buffer[i];
				}
				assertEquals("Hello", new String(receivedMessage));
				
				/*
				 * read "world"
				 */
				serverSocket.read(buffer, 5, 6);
				receivedMessage= new byte[11];
				for (int i = 0; i < receivedMessage.length; i++) {
					receivedMessage[i] = buffer[i];
				}
				assertEquals("Hello World", new String(receivedMessage));
				
				/*
				 * write "how are you"
				 */
				String reply = "How are you?";
				serverSocket.write(reply.getBytes(), 0, reply.length());
				
				if (!doSimultaneousClosing)
					Thread.sleep(5000);
				//closing the connection
				Log.d("Server", "Server closes the connection");
				serverSocket.close();
				Thread.sleep(15000);
				assertFalse(serverStack.hasFailed);
			}catch (InterruptedException e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * establish a new connection with packet synloss
	 * @param clientLoss
	 * @param serverLoss
	 */
	private void runTest(int[] clientLoss, int[] serverLoss){
		try {
			serverStack = new TCPWithPacketLoss(20, serverLoss);
			clientStack = new TCPWithPacketLoss(10, clientLoss);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Failed to create stack");
		}
		
		
		Thread thdServer = new Thread(new Server());
		Thread thdClient = new Thread(new Client());
		thdServer.start();
		thdClient.start();
		
		try {
			thdClient.join();
			thdServer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void testSendRecv(){
		try {
			serverStack = new TCPWithPacketLoss(20, noloss);
			clientStack = new TCPWithPacketLoss(10, noloss);
			String msg = "hi";
			TCPSegment pck = new TCPSegment(0, 0, 0, 0, TCPSegmentType.DATA, msg.getBytes());
			
			clientStack.send_tcp_segment(IpAddress.getAddress("192.168.0.20"), pck);
			
			TCPSegment pck2 = serverStack.recv_tcp_segment(10);
			
			assertTrue(Arrays.equals(pck.data, pck2.data));
			
		} catch (IOException e) {
			e.printStackTrace();
			fail("Failed to create stack");
		} catch (InvalidPacketException e1) {
			fail("corrupted packet: " + e1.getMessage());
		} catch (InterruptedException e2) {
			fail("timeout");
		}
	}
	
	public void testNoLoss(){
		runTest(noloss, noloss);
	}
	
	public void testSynLoss(){
		//lose the first SYN
		runTest(synloss, noloss);
		
		//lose the first 5 syn packets
		runTest(moresynloss, noloss);
	}
	
	/**
	 * tests what happens if all syn packets get lost. No server is necessary here.
	 */
	public void testConnectFail(){
		try {
			clientStack = new TCPWithPacketLoss(10, totalLoss);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Failed to create stack");
		}
		
		
		Thread thdClient = new Thread(new Client());
		thdClient.start();
		
		try {
			thdClient.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void testSynAckLoss(){
		//lose the first SYNACK
		runTest(noloss, synloss);
		
		//lose the first 5 SYNACKs
		runTest(noloss, moresynloss);
		
		//lose both first syn and synack
		runTest(synloss, synloss);
	}
	
	public void testAckLoss(){
		//lose the first ACK
		runTest(ackloss, noloss);
		
		//lose the first syn and the first ack
		runTest(synandackloss, noloss);
	}
	
	public void testDataLoss(){
		//lose first data
		runTest(datalossClnt, noloss);
		//lose data ACK
		runTest(noloss, dataAckLossServ);
		//lose both the above packets
		runTest(datalossClnt, dataAckLossServ);
	}
	
	public void testCloseLoss(){
		//lose server's fin
		runTest(noloss, finLossServAsyn);
		
		//lose server's ack to fin
		runTest(noloss, ackfinLossServAsyn);
		
		//lose Client's fin
		runTest(finLossClnt, noloss);
		
		//lose Client's ack to fin
		runTest(ackFinLossClnt, noloss);
		
		//synchronous closing
		doSimultaneousClosing = true;
		
		//fin loss
		runTest(noloss, finLossServSyn);
		
		//ack loss
		runTest(noloss, ackfinLossServSyn);
		
		doSimultaneousClosing = false;
	}
}
