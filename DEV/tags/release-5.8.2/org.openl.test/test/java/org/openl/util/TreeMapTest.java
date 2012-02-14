/**
 * Created Jul 21, 2007
 */
package org.openl.util;

import java.util.TreeMap;

import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class TreeMapTest extends TestCase {

	public void testInterval() {
		IntervalMap<String, String> map = new IntervalMap<String, String>();
		
		map.putInterval("1", "9", "18");
		map.putInterval("2", "4", "23");

		map.putInterval("6", "9", "68");
		map.putInterval("3", "7", "36");

		map.putInterval("5", "7", "56");
		map.putInterval("1", "9", "18X");
		
		assertEquals(0, map.getInInterval("0").size());		
		
		assertEquals(2, map.getInInterval("1").size());
		assertEquals("18",map.getInInterval("1").get(0));
		assertEquals("18X",map.getInInterval("1").get(1));
		
		assertEquals(3, map.getInInterval("2").size());
		assertEquals("18",map.getInInterval("2").get(0));
		assertEquals("23",map.getInInterval("2").get(1));
		assertEquals("18X",map.getInInterval("2").get(2));
		
		assertEquals(4, map.getInInterval("3").size());
		assertEquals("18",map.getInInterval("3").get(0));
		assertEquals("23",map.getInInterval("3").get(1));
		assertEquals("36",map.getInInterval("3").get(2));
		assertEquals("18X",map.getInInterval("3").get(3));
		
		assertEquals(3, map.getInInterval("4").size());
		assertEquals("18",map.getInInterval("4").get(0));
		assertEquals("36",map.getInInterval("4").get(1));
		assertEquals("18X",map.getInInterval("4").get(2));
		
		assertEquals(4, map.getInInterval("5").size());
		assertEquals("18",map.getInInterval("5").get(0));
		assertEquals("36",map.getInInterval("5").get(1));
		assertEquals("56",map.getInInterval("5").get(2));
		assertEquals("18X",map.getInInterval("5").get(3));
		
		assertEquals(5, map.getInInterval("6").size());
		assertEquals("18",map.getInInterval("6").get(0));
		assertEquals("68",map.getInInterval("6").get(1));
		assertEquals("36",map.getInInterval("6").get(2));
		assertEquals("56",map.getInInterval("6").get(3));
		assertEquals("18X",map.getInInterval("6").get(4));
	
		assertEquals(3, map.getInInterval("7").size());
		assertEquals("18",map.getInInterval("7").get(0));
		assertEquals("68",map.getInInterval("7").get(1));
		assertEquals("18X",map.getInInterval("7").get(2));
	
		assertEquals(3, map.getInInterval("8").size());
		assertEquals("18",map.getInInterval("8").get(0));
		assertEquals("68",map.getInInterval("8").get(1));
		assertEquals("18X",map.getInInterval("8").get(2));
		
		assertEquals(0, map.getInInterval("9").size());		

		map.removeInterval("1", "9", "18");
		map.removeInterval("1", "9", "18X");
		map.removeInterval("6", "9", "68");
		map.removeInterval("3", "7", "36");

		//After remove 18, 18X, 36, 68"
		//
		assertEquals(0, map.getInInterval("0").size());		
		
		assertEquals(0, map.getInInterval("1").size());		
		
		assertEquals(1, map.getInInterval("2").size());
		assertEquals("23",map.getInInterval("2").get(0));	
		
		assertEquals(1, map.getInInterval("3").size());
		assertEquals("23",map.getInInterval("3").get(0));
		
		assertEquals(0, map.getInInterval("4").size());		
		
		assertEquals(1, map.getInInterval("5").size());
		assertEquals("56",map.getInInterval("5").get(0));
		
		assertEquals(1, map.getInInterval("6").size());
		assertEquals("56",map.getInInterval("6").get(0));
		
		assertEquals(0, map.getInInterval("7").size());	
		
		assertEquals(0, map.getInInterval("8").size());	
		
		assertEquals(0, map.getInInterval("9").size());	

		map.putInterval("3", "5", "34");
		map.putInterval("4", "8", "47");
		map.removeInterval("3", "5", "34");
		
		//After add 34"
		//
		
		assertEquals(0, map.getInInterval("0").size());		
		
		assertEquals(0, map.getInInterval("1").size());		
		
		assertEquals(1, map.getInInterval("2").size());
		assertEquals("23",map.getInInterval("2").get(0));
		
		assertEquals(1, map.getInInterval("3").size());
		assertEquals("23",map.getInInterval("3").get(0));
		
		assertEquals(1, map.getInInterval("4").size());
		assertEquals("47",map.getInInterval("4").get(0));		
		
		assertEquals(2, map.getInInterval("5").size());
		assertEquals("56",map.getInInterval("5").get(0));
		assertEquals("47",map.getInInterval("5").get(1));
		
		assertEquals(2, map.getInInterval("6").size());
		assertEquals("56",map.getInInterval("6").get(0));
		assertEquals("47",map.getInInterval("6").get(1));
		
		assertEquals(1, map.getInInterval("7").size());
		assertEquals("47",map.getInInterval("7").get(0));		
		
		assertEquals(0, map.getInInterval("8").size());	
		
		assertEquals(0, map.getInInterval("9").size());
	}
	
	public void test1() {
		String value = "AnyValue";
		IntervalMap<Integer, String> map = new IntervalMap<Integer, String>();
		map.putInterval(Integer.valueOf(11), Integer.valueOf(23), value);
		assertEquals("There is one value in the list for given coordinate", 1, map.getInInterval(12).size());
		assertEquals(value, map.getInInterval(12).get(0));
		
		assertEquals("There is one value in the list for given coordinate", 1, map.getInInterval(22).size());
		assertEquals(value, map.getInInterval(22).get(0));
		
		assertEquals("There is no any value in the list for given coordinate. Means that right border is not included", 
				0, map.getInInterval(23).size());
		
		map.putInterval(Integer.valueOf(11), Integer.valueOf(50), "Yo");		
		assertEquals("There are two values in the list for given coordinate", 2, map.getInInterval(12).size());
		
		assertEquals("There is one value in the list for given coordinate", 1, map.getInInterval(23).size());
	}

    public void testTree() {
        TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();

        for (int i = 0; i < 10; i++) {
            map.put(new Integer(i * 2), new Integer(i * 2));
        }
        
        assertEquals(3, map.headMap(new Integer(5)).size());
        assertEquals(7, map.tailMap(new Integer(5)).size());
        assertEquals(10, map.headMap(new Integer(55)).size());
        assertEquals(0, map.tailMap(new Integer(55)).size());
        assertEquals(2, map.subMap(new Integer(4), new Integer(8)).size());
        assertEquals(1, map.subMap(new Integer(5), new Integer(8)).size());
        assertEquals(4, map.subMap(new Integer(-5), new Integer(8)).size());
    }

}
