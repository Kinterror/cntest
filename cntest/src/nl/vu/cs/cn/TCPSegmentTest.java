package nl.vu.cs.cn;

import java.util.Arrays;

import android.test.AndroidTestCase;
import android.util.Log;
import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCPSegment.TCPSegmentType;

public class TCPSegmentTest extends AndroidTestCase{

	public void testTCPSegmentSyn(){
		
		int src_port = 12345;
		int dest_port = 9876;
		long seq_nr = 1000;
		long ack_nr = 2002;

		TCPSegment segSyn = new TCPSegment(src_port, dest_port, seq_nr, ack_nr, TCPSegmentType.SYN, null);
		assertTrue(segSyn.src_port == src_port);
		assertTrue(segSyn.dest_port == dest_port);
		assertTrue(segSyn.seq_nr == seq_nr);
		assertTrue(segSyn.ack_nr == ack_nr);
		
		assertTrue(segSyn.syn == 1);
		assertTrue(segSyn.cwr == 0 &&  segSyn.ece == 0 && segSyn.urg == 0 && segSyn.psh == 1 && segSyn.rst== 0 &&
				segSyn.ns == 0 && segSyn.windowSize== 1 && segSyn.urgent_pointer == 0);
		
	}
	
	
	public void testTCPSegmentWrongPortNumber(){
		
		int src_ip = 256;
		int dest_ip = 253;
		int src_port = 65536;	//Not a correct port number
		int dest_port = 65535;
		long seq_nr = 1000;
		long ack_nr = 2002;
		
		String message = "This is a message.";
		byte[] messageData = message.getBytes(); 
		TCPSegment segSyn = new TCPSegment(src_port, dest_port, seq_nr, ack_nr, TCPSegmentType.DATA, messageData);
		short cs = TCPSegment.calculateChecksum(src_ip, dest_ip, messageData.length, messageData);
		segSyn.checksum = cs;
		byte[] data = segSyn.encode();
		segSyn = TCPSegment.decode(data, data.length);
		assertTrue(segSyn.src_port == 0);	//Expected behavior: set to 0
		assertTrue(segSyn.dest_port == dest_port);
		assertTrue(segSyn.seq_nr == seq_nr);
		assertTrue(segSyn.ack_nr == ack_nr);
		
		assertTrue(Arrays.equals(messageData, segSyn.data));
		
		assertTrue(segSyn.cwr == 0 &&  segSyn.ece == 0 && segSyn.urg == 0 && segSyn.psh == 1 && segSyn.rst== 0 &&
				segSyn.ns == 0 && segSyn.windowSize== 1 && segSyn.urgent_pointer == 0);
		assertTrue(cs == segSyn.checksum);
		assertTrue(segSyn.getSegmentType().equals(TCPSegmentType.DATA));
	}
	
	public void testNtohl(){
		int address = IpAddress.getAddress("192.168.0."+10).getAddress();
		Log.d("address", ""+address);
		Log.d("ntohl", ""+TCPSegment.ntohl(address));
		assertEquals(0xc0a8000a, TCPSegment.ntohl(address));
	}
	
	public void testChecksum(){

		int src_ip = 1;
		int dest_ip = 2;
		int src_port = 2048;
		int dest_port = 12345;
		long seq_nr = 1403268599;
		long ack_nr = 4099013128l;
		
		
		TCPSegment segSyn = new TCPSegment(src_port, dest_port, seq_nr, ack_nr, TCPSegmentType.SYNACK, new byte[0]);
		segSyn.windowSize=8152;
		segSyn.psh=0;
		
		
		
		//encode tcp packet
    	byte[] bytes = segSyn.encode();
    	
    	short cs = TCPSegment.calculateChecksum(IpAddress.getAddress("192.168.0."+src_ip).getAddress(),
				IpAddress.getAddress("192.168.0."+dest_ip).getAddress(), bytes.length, bytes);
		segSyn.checksum = cs;
    	
    	//hexdump the packet for debugging
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
    	//Check first if the computed checksum is right
        assertEquals(0x6677, cs&0xffff);
		
		Log.d("send_tcp_segment()","Packet bytes: " + sb.toString());
		Log.d("send_tcp_segment()","Packet to be sent: " + segSyn.toString());
    	Log.d("send_tcp_segment()","to IP : " + "192.168.0."+dest_ip +
    			" at port : " + segSyn.dest_port + " From IP: " + "192.168.0."+src_ip + " at port " + segSyn.src_port);
    	Log.d("send_tcp_segment()","Other pseudo header fields - length: " + bytes.length + " protocol: " + IP.TCP_PROTOCOL);
    	
    	//Then check if the validate checksum method works 
    	Log.d("cs of the packet", "segSyn.checksum = "+segSyn.checksum);
    	Log.d("cd computed", "cs = "+cs);
    	Log.d("test val cs", "src = "+IpAddress.getAddress("192.168.0."+src_ip).getAddress() + 
    			" , dst = "+IpAddress.getAddress("192.168.0."+dest_ip).getAddress());
    	assertTrue(TCPSegment.calculateChecksum(
    			IpAddress.getAddress("192.168.0."+src_ip).getAddress(),
				IpAddress.getAddress("192.168.0."+dest_ip).getAddress(),
				bytes.length,
				bytes
				) == segSyn.checksum);
		
	}
	
	public void testReferenceChecksum(){

		int src_ip = 1;
		int dest_ip = 2;
		int src_port = 2048;
		int dest_port = 12345;
		long seq_nr = 1403173209l;
		long ack_nr = 2563626109l;
		int window_size = 8152;
		
		TCPSegment segSyn = new TCPSegment(src_port, dest_port, seq_nr, ack_nr, TCPSegmentType.SYNACK, new byte[0]);
		segSyn.psh=0;
		segSyn.windowSize = window_size;
		
		
    	byte[] bytes = segSyn.encode();
    	
    	//hexdump the packet for debugging
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        
        short cs = TCPSegment.calculateChecksum(IpAddress.getAddress("192.168.0."+src_ip).getAddress(),
				IpAddress.getAddress("192.168.0."+dest_ip).getAddress(),
				20, bytes);
        
        segSyn.checksum = cs;
    	
		Log.d("send_tcp_segment()","Packet bytes: " + sb.toString());
		Log.d("send_tcp_segment()","Packet to be sent: " + segSyn.toString());
    	Log.d("send_tcp_segment()","to IP : " + "192.168.0."+dest_ip +
    			" at port : " + segSyn.dest_port + " From IP: " + "192.168.0."+src_ip + " at port " + segSyn.src_port);
    	Log.d("send_tcp_segment()","Other pseudo header fields - length: " + bytes.length + " protocol: " + IP.TCP_PROTOCOL);
    	
    	//this was checked using a script that generates a TCP packet and sends it. 
    	//After that, the expected checksum could be read in wireshark
		assertEquals(0x5c26, cs);
	}
}
