package nl.vu.cs.cn;

import android.test.AndroidTestCase;
import junit.framework.Assert;


public class BoundedByteBufferTest extends AndroidTestCase{

	String str = "This is a string.";
	String str2 = "This is another string.";
	byte[] data = str.getBytes();
	byte[] data2 = str2.getBytes();
	

	public void testBoundedByteBuffer() {
		
		BoundedByteBuffer buf = new BoundedByteBuffer(10);
		
		Assert.assertTrue(buf.isEmpty());	
	}

	public void testBuffer() {
		int max_length = 40;
		BoundedByteBuffer buf = new BoundedByteBuffer(max_length);
		try {
			buf.buffer(data);
			System.out.println(buf.length());

			buf.buffer(data2);
		} catch (FullCollectionException e) {
			e.printStackTrace();
			Assert.fail("buffer is full");
		}
		System.out.println(buf.length());
		Assert.assertTrue(!buf.isEmpty());
	}

	public void testDeBuffer() {
		int max_length = 40;
		BoundedByteBuffer buf = new BoundedByteBuffer(max_length);
		byte[] arr = new byte[40];
		try {
			buf.buffer(data);
			System.out.println(buf.length());

			buf.buffer(data2);
		} catch (FullCollectionException e) {
			e.printStackTrace();
			Assert.fail("buffer is full");
		}
		buf.deBuffer(arr, 0, 40);
		System.out.println("testDeBuffer " + new String(arr));
		Assert.assertTrue(buf.isEmpty());
		
	}
	
	public void testDeBuffer2(){
		int max_length = 2;
		BoundedByteBuffer buf = new BoundedByteBuffer(max_length);
		
		String s = "aa";
		byte[] b = s.getBytes();
		byte[] arr = new byte[3];
		
		try {
			buf.buffer(b);
			
		} catch (FullCollectionException e) {
			e.printStackTrace();
			Assert.fail("exception");
		}
		buf.deBuffer(arr, 0, 3);
		System.out.println("testDeBuffer2 " + new String(arr));
	}
	
	public void testDeBuffer3(){
		int max_length = 2;
		BoundedByteBuffer buf = new BoundedByteBuffer(max_length);
		
		String s = "aa";
		byte[] b = s.getBytes();
		byte[] arr = new byte[1];
		
		try {
			buf.buffer(b);
			
		} catch (FullCollectionException e) {
			e.printStackTrace();
			Assert.fail("exception");
		}
		buf.deBuffer(arr, 0, 1);
		System.out.println("testDeBuffer3 " + new String(arr));
		
	}
	
	
	public void testDeBuffer4(){
		int max_length = 10;
		BoundedByteBuffer buf = new BoundedByteBuffer(max_length);
		
		String s = "aa";
		byte[] b = s.getBytes();
		byte[] arr = new byte[5];
		
		try {
			buf.buffer(b);
			buf.buffer(b);
			buf.buffer(b);
			buf.buffer(b);
			buf.buffer(b);
			
		} catch (FullCollectionException e) {
			e.printStackTrace();
			Assert.fail("exception");
		}
		buf.deBuffer(arr, 0, 5);
		System.out.println("testDeBuffer4 - arr : " + new String(arr));
		
		byte[] tmp = new byte[10];
		buf.deBuffer(tmp, 0, 10);
		System.out.println("testDeBuffer4 - arr : " + new String(tmp));
		Assert.assertTrue(buf.isEmpty());
	}

}
