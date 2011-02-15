package com.exigen.le.beangenerator;

import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.exigen.le.smodel.MappedProperty;
import com.exigen.le.smodel.Type;



import static org.junit.Assert.*;

public class BeanGeneratorTest {

	GeneratorClassLoader cl;
	
	@Before
	public void setUp(){
		cl = new GeneratorClassLoader();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void nameTest() throws Exception{
		String beanName = "a.b.C";
		Type type = new Type();
		type.setName("C");
		type.setPath(beanName);
		type.setComplex(true);
		
		List<MappedProperty> childs = new ArrayList<MappedProperty>();
		MappedProperty c1 = new MappedProperty("i", Type.DOUBLE);
		childs.add(c1);
		type.setChilds(childs);
		
		Class c = cl.defineClass(BeanGenerator.getQualifiedType(type), BeanGenerator.generate(type,new PrintWriter(System.out)));
		Object o = c.newInstance(); // object of type "a.b.C"
		assertEquals(o.getClass().getCanonicalName(),BeanGenerator.getQualifiedType(type));
		
		
	}

	/*
	 * public class C{
	 * 	int i;
	 *  boolean b;
	 *  short s;
	 *  long l;
	 *  char c;
	 *  byte by;
	 *  float f;
	 *  double d;
	 *  
	 *    ... set/get
	 * }
	*/

	
//	@SuppressWarnings("unchecked")
//	@Test
//	public void primitiveTypesTest()throws Exception{
//		String beanName = "a.b.C";
//		ids.add(new ItemDescription("i", "int", false));
//		ids.add(new ItemDescription("b", "boolean", false));
//		ids.add(new ItemDescription("s", "short", false));
//		ids.add(new ItemDescription("l", "long", false));
//		ids.add(new ItemDescription("c", "char", false));
//		ids.add(new ItemDescription("by", "byte", false));
//		ids.add(new ItemDescription("f", "float", false));
//		ids.add(new ItemDescription("d", "double", false));
//
//		Class c = cl.defineClass(beanName, BeanGenerator.generate(beanName, ids));
//		Object o = c.newInstance(); // object of type "a.b.C"
//		reflectionHelper(o, "I", int.class, new Integer(10));
//		reflectionHelper(o, "B", boolean.class, new Boolean(true));
//		reflectionHelper(o, "S", short.class, new Short((short)10));
//		reflectionHelper(o, "L", long.class, new Long(10));
//		reflectionHelper(o, "C", char.class, new Character('a'));
//		reflectionHelper(o, "By", byte.class, new Byte((byte)1));
//		reflectionHelper(o, "F", float.class, new Float(5.));
//		reflectionHelper(o, "D", double.class, new Double(10.0d));
//	}
//	
	/*
	 * public class C{
	 * 	int[] i;
	 *    ... set/get
	 * }
	*/

	
//	@SuppressWarnings("unchecked")
//	@Test
//	public void primitiveCollectionTest() throws Exception{
//		String beanName = "a.b.C";
//		ids.add(new ItemDescription("i", "int", true));
//		Class c = cl.defineClass(beanName, BeanGenerator.generate(beanName, ids));
//		Object o = c.newInstance(); // object of type "a.b.C"
//		
//		// call setter
//		Class[] types = new Class[]{int[].class};
//		Method method = o.getClass().getMethod("setI", types);
//		int[] k = new int[]{1,2,3};
//		Object[] args = new Object[]{k};
//		method.invoke(o, args);
//		
//		// call getter
//		types = new Class[]{};
//		method = o.getClass().getMethod("getI", types);
//		Object result = method.invoke(o,new Object[0]);
//		assertArrayEquals(k, (int[]) result); 
//
//		
//		// set & get element
//		types = new Class[]{int.class, int.class};
//		method = o.getClass().getMethod("setI", types);
//		int index = 1;
//		int value = 7;
//		args =new Object[]{index, value};
//		method.invoke(o, args);
//		
//		// retrieve and compare
//		types = new Class[]{int.class};
//		method = o.getClass().getMethod("getI", types);
//		args = new Object[]{index};
//		result = method.invoke(o,args);
//		assertEquals(value, result); 		
//	}
//	
	/*
	 * public class C{
	 * 	String s;
	 *    ... set/get
	 * }
	*/
	@SuppressWarnings("unchecked")
	@Test
	public void objectTypeTest() throws Exception {
		String beanName = "a.b.C";
		String t = "test";

		Type type = new Type();
		type.setName("C");
		type.setPath(beanName);
		type.setComplex(true);
		
		List<MappedProperty> childs = new ArrayList<MappedProperty>();
		MappedProperty c1 = new MappedProperty("s", Type.STRING);
		childs.add(c1);
		type.setChilds(childs);
		
		
		
		Class c = cl.defineClass(BeanGenerator.getQualifiedType(type), BeanGenerator.generate(type));
		Object o = c.newInstance(); // object of type "a.b.C"
		reflectionHelper(o, "S", Class.forName("java.lang.String"), t);
		
		// !-- to be deleted

//		Thread.currentThread().setContextClassLoader(cl);
//
//		XMLEncoder enc = new XMLEncoder(new BufferedOutputStream( new FileOutputStream("E:/temp/lll.txt")));
//		enc.writeObject(o);
//		enc.close();
		
	}
	
	/*
	 * public class C{
	 * 	String[] s;
	 *    ... set/get
	 * }
	*/
	@SuppressWarnings("unchecked")
	@Test
	public void objectCollectionTest() throws Exception {
		String beanName = "a.b.C";
		String[] t = {"test1","test2"};
		
		Type type = new Type();
		type.setName("C");
		type.setPath(beanName);
		type.setComplex(true);
		
		List<MappedProperty> childs = new ArrayList<MappedProperty>();
		MappedProperty c1 = new MappedProperty("s", Type.STRING);
		c1.setCollection(true);
		childs.add(c1);
		type.setChilds(childs);

		
		Class c = cl.defineClass(BeanGenerator.getQualifiedType(type), BeanGenerator.generate(type));
		Object o = c.newInstance(); // object of type "a.b.C"
		reflectionHelper(o, "S", t.getClass(), t,true);
		
		// test indexed element
		// set & get element
		Class[] types = new Class[]{int.class, String.class};
		Method method = o.getClass().getMethod("setS", types);
		int index = 1;
		String value = "mmm";
		Object[]args =new Object[]{index, value};
		method.invoke(o, args);
		
		// retrieve and compare
		types = new Class[]{int.class};
		method = o.getClass().getMethod("getS", types);
		args = new Object[]{index};
		Object result = method.invoke(o,args);
		assertEquals(value, result); 		
		
	}
	
	/*
	 * public class C{
	 * 	String s;
	 * 	x.y.Z b2;
	 *    ... set/get
	 * }
	 * 
	 * public class Z {
	 *   int i;
	 *   ... set/get
	 * }
	 *  
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void beanInBeanTest() throws Exception {
		String bean1Name = "a.b.C";
		String bean2Name = "x.y.Z";
		
		
		
		Type type2 = new Type();
		type2.setName("Z");
		type2.setPath(bean2Name);
		type2.setComplex(true);
		
		List<MappedProperty> childs2 = new ArrayList<MappedProperty>();
		MappedProperty p2 = new MappedProperty("i", Type.DOUBLE);
		childs2.add(p2);
		type2.setChilds(childs2);
		Class c2 = cl.defineClass(BeanGenerator.getQualifiedType(type2), BeanGenerator.generate(type2));
		
		Type type1 = new Type();
		type1.setName("C");
		type1.setPath(bean1Name);
		type1.setComplex(true);
		
		List<MappedProperty> childs1 = new ArrayList<MappedProperty>();
		MappedProperty p1 = new MappedProperty("s", Type.STRING);
		childs1.add(p1);
		MappedProperty p11 = new MappedProperty("b2", type2);
		childs1.add(p11);
		type1.setChilds(childs1);
		Class c1 = cl.defineClass(BeanGenerator.getQualifiedType(type1), BeanGenerator.generate(type1));
		
		
		
		
		

		
		String t = "test";
		Object o = c1.newInstance(); // object of type "a.b.C"
		reflectionHelper(o, "S", t.getClass(), t);
		
		Object o2 = c2.newInstance();
		reflectionHelper(o2, "I", Class.forName("java.lang.Double"), new Double(10));
		
		reflectionHelper(o,"B2", c2, o2);
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void beanInBeanTreeTest() throws Exception{
		String bean1Name = "a.b.C";
		String bean2Name = "x.y.Z";

		
		Type type2 = new Type();
		type2.setName("Z");
		type2.setPath(bean2Name);
		type2.setComplex(true);
		
		List<MappedProperty> childs2 = new ArrayList<MappedProperty>();
		MappedProperty p2 = new MappedProperty("i", Type.DOUBLE);
		childs2.add(p2);
		type2.setChilds(childs2);
		
		Type type1 = new Type();
		type1.setName("C");
		type1.setPath(bean1Name);
		type1.setComplex(true);
		
		List<MappedProperty> childs1 = new ArrayList<MappedProperty>();
		MappedProperty p1 = new MappedProperty("s", Type.STRING);
		childs1.add(p1);
		MappedProperty p11 = new MappedProperty("b2", type2,true);
		childs1.add(p11);
		type1.setChilds(childs1);

		
		GeneratorClassLoader cl = new GeneratorClassLoader();
		
		// defines and loads bean tree
		Class cBean1 = BeanTreeGenerator.loadBeanClasses(BeanGenerator.getQualifiedType(type1), type1, cl);
		Class[] types = new Class[]{};
		Method method = cBean1.getMethod("get"+"B2", types);
		Class cBean2 = method.getReturnType();
		
		Object b1 = cBean1.newInstance();
		Object b2 = cBean2.newInstance();
		
		String t = "test";
		reflectionHelper(b1, "S", t.getClass(), t);
		reflectionHelper(b2, "I", Class.forName("java.lang.Double"), new Double(10));
		
		reflectionHelper(b1,"B2", cBean2, b2);
		
		
	}
	
	

	/*
	 * public class C{
	 * 	String s;
	 * 	x.y.Z[] b2;
	 *    ... set/get
	 * }
	 * 
	 * public class Z {
	 *   int i;
	 *   ... set/get
	 * }
	 *  
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void beanInBeanCollectionTest() throws Exception{
		String bean1Name = "a.b.C";
		String bean2Name = "x.y.Z";

		
		Type type2 = new Type();
		type2.setName("Z");
		type2.setPath(bean2Name);
		type2.setComplex(true);
		
		List<MappedProperty> childs2 = new ArrayList<MappedProperty>();
		MappedProperty p2 = new MappedProperty("i", Type.DOUBLE);
		childs2.add(p2);
		type2.setChilds(childs2);
		Class c2 = cl.defineClass(BeanGenerator.getQualifiedType(type2), BeanGenerator.generate(type2));
		
		Type type1 = new Type();
		type1.setName("C");
		type1.setPath(bean1Name);
		type1.setComplex(true);
		
		List<MappedProperty> childs1 = new ArrayList<MappedProperty>();
		MappedProperty p1 = new MappedProperty("s", Type.STRING);
		childs1.add(p1);
		MappedProperty p11 = new MappedProperty("b2", type2);
		p11.setCollection(true);
		childs1.add(p11);
		type1.setChilds(childs1);
		Class c1 = cl.defineClass(BeanGenerator.getQualifiedType(type1), BeanGenerator.generate(type1));

		
		
		Object o = c1.newInstance(); // object of type "a.b.C"
		
		Object o2 = c2.newInstance(); // object of type "x.y.Z"
		reflectionHelper(o2, "I", Class.forName("java.lang.Double"), new Double(10)); // 10 assigned to i
		
		Object o3 = c2.newInstance(); // object of type "x.y.Z"
		reflectionHelper(o3, "I", Class.forName("java.lang.Double"), new Double(20)); // 20 assigned to i
		
		Object array = Array.newInstance(c2, 2); // new x.y.Z[2]
		Array.set(array, 0, o2);
		Array.set(array, 1, o3);
		
		reflectionHelper(o, "B2", array.getClass(), array,true);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void beanInBeanCollectionTreeTest() throws Exception{
		String bean1Name = "a.b.C";
		String bean2Name = "x.y.Z";

		
		Type type2 = new Type();
		type2.setName("Z");
		type2.setPath(bean2Name);
		type2.setComplex(true);
		
		List<MappedProperty> childs2 = new ArrayList<MappedProperty>();
		MappedProperty p2 = new MappedProperty("i", Type.DOUBLE);
		childs2.add(p2);
		type2.setChilds(childs2);
		
		Type type1 = new Type();
		type1.setName("C");
		type1.setPath(bean1Name);
		type1.setComplex(true);
		
		List<MappedProperty> childs1 = new ArrayList<MappedProperty>();
		MappedProperty p1 = new MappedProperty("s", Type.STRING);
		childs1.add(p1);
		MappedProperty p11 = new MappedProperty("b2", type2,true);
		p11.setCollection(true);
		childs1.add(p11);
		type1.setChilds(childs1);
		
		GeneratorClassLoader cl = new GeneratorClassLoader();
		
		// defines and loads bean tree
		Class cBean1 = BeanTreeGenerator.loadBeanClasses(BeanGenerator.getQualifiedType(type1), type1, cl);
		Class[] types = new Class[]{};
		Method method = cBean1.getMethod("get"+"B2", types);
		Class cBean2Arr = method.getReturnType(); //returns ...x.y.Z[]
		String cBean2Name = cBean2Arr.getCanonicalName().substring(0, cBean2Arr.getCanonicalName().length()-2);
		Class cBean2 = cl.loadClass(cBean2Name);
		
		Object o = cBean1.newInstance(); // object of type "a.b.C"
		
		Object o2 = cBean2.newInstance(); // object of type "x.y.Z"
		reflectionHelper(o2, "I", Class.forName("java.lang.Double"), new Double(10)); // 10 assigned to i
		
		Object o3 = cBean2.newInstance(); // object of type "x.y.Z"
		reflectionHelper(o3, "I", Class.forName("java.lang.Double"), new Double(20)); // 20 assigned to i
		
		Object array = Array.newInstance(cBean2, 2); // new x.y.Z[2]
		Array.set(array, 0, o2);
		Array.set(array, 1, o3);
		
		reflectionHelper(o, "B2", array.getClass(), array,true);
		
		
	}
	

	
	
	
	
	@SuppressWarnings("unchecked")
	// call setter and getter on object via reflection and compare results
	private void reflectionHelper(Object target, String propertyName, Class cls, Object value, boolean collection) throws Exception{
		
		// setter
		Class[] types = new Class[]{cls};
		Method method = target.getClass().getMethod("set"+propertyName, types);
		method.invoke(target,new Object[]{value});
		
		//getter
		types = new Class[]{};
		method = target.getClass().getMethod("get"+propertyName, types);
		Object result = method.invoke(target, new Object[0]);
		
		if (!collection)
			assertEquals(value, result);
		else
			assertArrayEquals((Object[])value, (Object[])result);
	}
	
	@SuppressWarnings("unchecked")
	private void reflectionHelper(Object target, String propertyName, Class cls, Object value) throws Exception{
		reflectionHelper(target, propertyName, cls, value, false);

	}
	
	
	
}
