package org.openl.rules.lang.xls.types;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 * @author DLiauchuk
 *
 */
public class DatatypeOpenClassTest {
	
	private final String DEFAULT_PACKAGE = "default.test";
	private final String DEFAULT_NAME = "DatatypeTest";
	
	@Test
	public void testEquals() {
		DatatypeOpenClass doc1 = new DatatypeOpenClass(null, DEFAULT_NAME, DEFAULT_PACKAGE);
		DatatypeOpenClass doc2 = new DatatypeOpenClass(null, DEFAULT_NAME, DEFAULT_PACKAGE);
		DatatypeOpenClass doc3 = new DatatypeOpenClass(null, DEFAULT_NAME, DEFAULT_PACKAGE);
		// reflexive check
		//
		assertTrue(doc1.equals(doc1));
		assertEquals(doc1.hashCode(), doc1.hashCode());
		
		// symmetric check
		//
		assertTrue(doc1.equals(doc2));
		assertTrue(doc2.equals(doc1));		
		assertEquals(doc1.hashCode(), doc2.hashCode());
		
		// transitive check
		//
		assertTrue(doc1.equals(doc2));
		assertTrue(doc2.equals(doc3));
		assertTrue(doc3.equals(doc1));
		
		//consistent check
		//
		assertTrue(doc1.equals(doc2));
		assertTrue(doc1.equals(doc2));
		assertTrue(doc1.equals(doc2));
		
		// null check
		//
		assertFalse(doc1.equals(null));
		
		DatatypeOpenClass doc4 = new DatatypeOpenClass(null, DEFAULT_NAME, DEFAULT_PACKAGE + "suffix");		
		assertFalse(doc1.equals(doc4));
		assertFalse(doc4.equals(doc1));
		assertFalse(doc1.hashCode() == doc4.hashCode());
	}
}
