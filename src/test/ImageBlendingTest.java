package test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Assert.*;
//import static org.hamcrest.CoreMatchers.is;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import game.*;

public class ImageBlendingTest {
	
	//Display display;
	Display display = new Display("JUnit Test");
	
	/*
	ImageBlendingTest(){
		display = new Display();
	}
	*/
	

	@Before
	public void setUp() {
	}
			
	
	@Test
	public void testCreateIdentityMatrix() {

		int numPics = 6;
		double[][] expect = new double [numPics][numPics]; 
		
		for (int i = 0; i < numPics; i++) {
			expect[i][i] = 1; 		
		}
		/*
		for (int i=0; i<expect.length; i++) {
			System.out.println(Arrays.toString(expect[i]));
		}
		*/
		
		display.setNumPics(numPics);
		
		display.createIdentityMatrix();
		assertArrayEquals(expect, display.getmInv());
	}
	
	
	@Test @Ignore
	public void testNumPics() {
		int expect = 6;
		display.setNumPics(expect);
		int actual = display.getNumPics();
		assertEquals(expect, actual);
	}
	
}
