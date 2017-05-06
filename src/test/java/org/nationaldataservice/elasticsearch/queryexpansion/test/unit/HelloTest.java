package org.nationaldataservice.elasticsearch.queryexpansion.test.unit;

import org.elasticsearch.test.ESTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HelloTest extends ESTestCase {
	@Before
	public void setUp() {
		System.out.println("Set up!");
	}
	
	@After
	public void tearDown() {
		System.out.println("Tear down!");
	}
	
	@Test
	public void basicTest() {
		System.out.println("Test!");
	}
}
