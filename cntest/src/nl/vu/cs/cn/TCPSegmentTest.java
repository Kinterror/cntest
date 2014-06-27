package nl.vu.cs.cn;

import java.util.Arrays;

import android.test.AndroidTestCase;
import android.util.Log;
import junit.framework.Assert;
import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCPSegment.TCPSegmentType;

public class TCPSegmentTest extends AndroidTestCase{


	public void testTCPSegmentSyn(){
		
		int src_port = 12345;
		int dest_port = 9876;
		long seq_nr = 1000;
		long ack_nr = 2002;

		TCPSegment segSyn = new TCPSegment(src_port, dest_port, seq_nr, ack_nr, TCPSegmentType.SYN, null);
		Assert.assertTrue(segSyn.src_port == src_port);
		Assert.assertTrue(segSyn.dest_port == dest_port);
		Assert.assertTrue(segSyn.seq_nr == seq_nr);
		Assert.assertTrue(segSyn.ack_nr == ack_nr);
		
		Assert.assertTrue(segSyn.syn == 1);
		Assert.assertTrue(segSyn.cwr == 0 &&  segSyn.ece == 0 && segSyn.urg == 0 && segSyn.psh == 1 && segSyn.rst== 0 &&
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
		Assert.assertTrue(segSyn.src_port == 0);	//Expected behavior: set to 0
		Assert.assertTrue(segSyn.dest_port == dest_port);
		Assert.assertTrue(segSyn.seq_nr == seq_nr);
		Assert.assertTrue(segSyn.ack_nr == ack_nr);
		
		Assert.assertTrue(Arrays.equals(messageData, segSyn.data));
		
		Assert.assertTrue(segSyn.cwr == 0 &&  segSyn.ece == 0 && segSyn.urg == 0 && segSyn.psh == 1 && segSyn.rst== 0 &&
				segSyn.ns == 0 && segSyn.windowSize== 1 && segSyn.urgent_pointer == 0);
		Assert.assertTrue(cs == segSyn.checksum);
		Assert.assertTrue(segSyn.getSegmentType().equals(TCPSegmentType.DATA));
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
    	assertTrue(TCPSegment.calculateChecksum(IpAddress.getAddress("192.168.0."+src_ip).getAddress(),
				IpAddress.getAddress("192.168.0."+dest_ip).getAddress(),
				bytes.length,
				bytes
				) == segSyn.checksum);
		
		
		
	}
	
	public void testChecksum2(){

		int src_ip = 1;
		int dest_ip = 2;
		int src_port = 2048;
		int dest_port = 12345;
		long seq_nr = 1403268599;
		long ack_nr = 4099013128l;
		
		
		TCPSegment segSynAck = new TCPSegment(src_port, dest_port, seq_nr, ack_nr, TCPSegmentType.SYNACK, new byte[0]);
		segSynAck.windowSize = 8152;
		segSynAck.psh = 0;
		
		
		//encode tcp packet
    	byte[] bytes = segSynAck.encode();
    	
    	short cs = TCPSegment.calculateChecksum(IpAddress.getAddress("192.168.0."+src_ip).getAddress(),
				IpAddress.getAddress("192.168.0."+dest_ip).getAddress(), bytes.length, bytes);
		segSynAck.checksum = cs;
    	
    	//hexdump the packet for debugging
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
    	//Check first if the computed checksum is right
        //assertEquals(0x6677, cs&0xffff);
		
		Log.d("send_tcp_segment()","Packet bytes: " + sb.toString());
		Log.d("send_tcp_segment()","Packet to be sent: " + segSynAck.toString());
    	Log.d("send_tcp_segment()","to IP : " + "192.168.0."+dest_ip +
    			" at port : " + segSynAck.dest_port + " From IP: " + "192.168.0."+src_ip + " at port " + segSynAck.src_port);
    	Log.d("send_tcp_segment()","Other pseudo header fields - length: " + bytes.length + " protocol: " + IP.TCP_PROTOCOL);
    	
    	//Then check if the validate checksum method works 
    	Log.d("cs of the packet", "segSyn.checksum = "+segSynAck.checksum);
    	Log.d("cd computed", "cs = "+cs);
    	Log.d("test val cs", "src = "+IpAddress.getAddress("192.168.0."+src_ip).getAddress() + 
    			" , dst = "+IpAddress.getAddress("192.168.0."+dest_ip).getAddress());
    	
    	/*assertTrue(segSynAck.validateChecksum(IpAddress.getAddress("192.168.0."+src_ip).getAddress(),
				IpAddress.getAddress("192.168.0."+dest_ip).getAddress()));
		*/
	}
	
	public void testChecksumDiversFlags(){

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
		/*short cs = segSyn.calculateChecksum(IpAddress.getAddress("192.168.0."+src_ip).getAddress(),
				IpAddress.getAddress("192.168.0."+dest_ip).getAddress(), protocol);*/
		//segSyn.checksum = cs;
		//encode tcp packet
    	byte[] bytes = segSyn.encode();
    	
    	//hexdump the packet for debugging
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
    	
		
		
		Log.d("send_tcp_segment()","Packet bytes: " + sb.toString());
		Log.d("send_tcp_segment()","Packet to be sent: " + segSyn.toString());
    	Log.d("send_tcp_segment()","to IP : " + "192.168.0."+dest_ip +
    			" at port : " + segSyn.dest_port + " From IP: " + "192.168.0."+src_ip + " at port " + segSyn.src_port);
    	Log.d("send_tcp_segment()","Other pseudo header fields - length: " + bytes.length + " protocol: " + IP.TCP_PROTOCOL);
    	
		//assertEquals(0x5c26, cs&0xffff);
	}
}
