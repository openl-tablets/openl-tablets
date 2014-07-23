package org.openl.binding.impl.cast;


import org.junit.Assert;
import org.junit.Test;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

public class CastFactoryTest {

	public CastFactoryTest() {
		factory  = new CastFactory();
		factory.setMethodFactory(NullOpenClass.the);
	}
	
	CastFactory factory;

	@Test
	public void testPrimitives()
	{
		javaCastTest(Integer.class, int.class);
		javaCastTest(int.class, Integer.class);
		javaCastTest(Boolean.class, boolean.class);
		javaCastTest(boolean.class, Boolean.class);
		javaCastTest(Void.class, void.class);
		javaCastTest(void.class, Void.class);
	}
	
	
	
	void javaCastTest(Class<?> from, Class<?> to) {
		
		IOpenCast cast = factory.getCast(JavaOpenClass.getOpenClass(from), JavaOpenClass.getOpenClass(to));
		Assert.assertNotNull(cast);
		Assert.assertTrue(cast.isImplicit());
		
		
	}

}
