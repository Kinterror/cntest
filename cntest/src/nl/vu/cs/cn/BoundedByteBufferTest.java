package nl.vu.cs.cn;

import java.util.Arrays;

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
			fail("buffer is full");
		}
		assertEquals(buf.length(), data.length + data2.length);
		assertFalse(buf.isEmpty());
	}

	/**
	 * test with matching buffer size
	 */
	public void testMatchingBufferSize(){
		BoundedByteBuffer buf = new BoundedByteBuffer(10);
		byte[] array = {1,2,3,4,5};
		byte[] array2 = new byte[5];
		
		try {
			buf.buffer(array);
		} catch (FullCollectionException e) {
			fail();
		}
		
		buf.deBuffer(array2, 0, 5);
		
		assertTrue(Arrays.equals(array, array2));
	}
	
	/**
	 * remove more elements than present in the buffer
	 */
	public void testDeBufferMore() {
		int max_length = 40;
		BoundedByteBuffer buf = new BoundedByteBuffer(max_length);
		byte[] arr = new byte[40];
		try {
			buf.buffer(data);
			System.out.println(buf.length());

			buf.buffer(data2);
		} catch (FullCollectionException e) {
			e.printStackTrace();
			fail("buffer is full");
		}
		int n = buf.deBuffer(arr, 0, 40);
		System.out.println("testDeBuffer " + new String(arr));
		assertTrue(buf.isEmpty());
		assertEquals(n, data.length + data2.length);
	}
	
	/**
	 * remove less elements than present
	 */
	public void testDeBufferLess(){
		int max_length = 2;
		BoundedByteBuffer buf = new BoundedByteBuffer(max_length);
		
		String s = "aa";
		byte[] b = s.getBytes();
		byte[] arr = new byte[1];
		
		try {
			buf.buffer(b);
			
		} catch (FullCollectionException e) {
			e.printStackTrace();
			fail("exception");
		}
		buf.deBuffer(arr, 0, 1);
		assertEquals(new String(arr), "a");
	}
	
	/**
	 * test if the remaining bytes are put back into the buffer correctly with mismatched sizes
	 */
	public void testDeBufferTwice(){
		int max_length = 10;
		BoundedByteBuffer buf = new BoundedByteBuffer(max_length);
		
		String s = "aa";
		byte[] b = s.getBytes();
		byte[] arr = new byte[5];
		
		try {
			//add five elements of 2 bytes.
			buf.buffer(b);
			buf.buffer(b);
			buf.buffer(b);
			buf.buffer(b);
			buf.buffer(b);
			
		} catch (FullCollectionException e) {
			e.printStackTrace();
			fail("exception");
		}
		buf.deBuffer(arr, 0, 5);
		//now the buffer should contain first one element of one byte, and then two elements of two bytes.
		System.out.println("testDeBuffer4 - arr : " + new String(arr));
		
		//remove 10 bytes from the buffer, which contains only 5. Now the five elements should be returned
		byte[] tmp = new byte[10];
		int n = buf.deBuffer(tmp, 0, 10);
		System.out.println("testDeBuffer4 - arr : " + new String(tmp));
		assertTrue(buf.isEmpty());
		assertEquals(n, 5);
	}
	
	/**
	 * test if an exception is raised when buffering if the buffer is full
	 */
	public void testDeBufferFull(){
		BoundedByteBuffer buf = new BoundedByteBuffer(10);
		byte[] array = {1,2,3,4,5,6,7,8,9,0};
		byte[] array2 = {1,2,3,4,5,6,7,8,9,10,11};
		byte[] array3 = {0};
		
		//add 11 elements to the buffer. Exception is expected.
		try {
			buf.buffer(array2);
			fail();
		} catch (FullCollectionException e) {}
		
		//now add 10 elements. This should work, as the previous elements should not have been added.
		try{
			buf.buffer(array);
			
		} catch (FullCollectionException e) {
			fail();
		}
		
		//add one additional element to the buffer, which is already full. An exception should be thrown.
		try{
			buf.buffer(array3);
			fail();
		} catch (FullCollectionException e) {}
	}

}
