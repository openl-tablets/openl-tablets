package org.openl.binding.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;
import org.openl.binding.ICastFactory;
import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.TypeCastFactory;
import org.openl.meta.DoubleValue;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class MethodSearchTest {

	public static class ClassWithMethods {
		public void method1(int arg1, double arg2) {
		};

		public void method1(int arg1, byte arg2) {
		};

		public void method2(int arg1, double arg2) {
		};
	}

	public static class SecondClassWithMethods extends ClassWithMethods implements Serializable {
		private static final long serialVersionUID = 1L;

		public void method1(int arg1, double arg2) {
		};

		public void method3(int arg1, double arg2) {
		};
	}

	public static class ClassWithGenerics {
		public <T> T[] method1(T arg1, T arg2) {
			return null;
		};
	}

	public static class ThirdClassWithMethods extends SecondClassWithMethods {
		private static final long serialVersionUID = 1L;
	}

	private ICastFactory getCastFactory() {
		TypeCastFactory typecast = new TypeCastFactory();
		TypeCastFactory.JavaCastComponent javacast = new TypeCastFactory.JavaCastComponent();
		javacast.setLibraryClassName(org.openl.binding.impl.Operators.class.getName());
		javacast.setClassName(org.openl.binding.impl.cast.CastFactory.class.getName());
		typecast.addJavaCast(javacast);
		OpenLConfiguration openLConfiguration = new OpenLConfiguration();
		openLConfiguration.setTypeCastFactory(typecast);
		openLConfiguration.setConfigurationContext(new ConfigurableResourceContext(null));
		return openLConfiguration;
	}

	private static boolean methodEquals(IMethodCaller methodCaller1, IMethodCaller methodCaller2) {
		if (methodCaller1.getMethod().getName().equals(methodCaller2.getMethod().getName())) {
			if (methodCaller1.getMethod().getSignature().equals(methodCaller2.getMethod().getSignature())) {
				return true;
			}
		}
		return false;
	}

	@Test
	public void testMethodChoosing() {
		ICastFactory castFactory = getCastFactory();

		JavaOpenClass javaOpenClass = JavaOpenClass.getOpenClass(ClassWithMethods.class);
		JavaOpenClass javaOpenClass2 = JavaOpenClass.getOpenClass(SecondClassWithMethods.class);

		IMethodCaller methodCaller1 = MethodSearch.getMethodCaller("method1",
				new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.INT }, castFactory, javaOpenClass);

		assertTrue(methodEquals(
				javaOpenClass.getMethod("method1", new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }),
				methodCaller1));

		IMethodCaller methodCaller2 = MethodSearch.getMethodCaller("method1",
				new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.INT }, castFactory, javaOpenClass2);

		assertTrue(methodEquals(
				javaOpenClass2.getMethod("method1", new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }),
				methodCaller2));

	}

	@Test
	public void testMethodChoosingWithGenerics() {
		ICastFactory castFactory = getCastFactory();

		JavaOpenClass javaOpenClass = JavaOpenClass.getOpenClass(ClassWithGenerics.class);

		IMethodCaller methodCaller1 = MethodSearch.getMethodCaller("method1",
				new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.STRING }, castFactory, javaOpenClass);

		assertNotNull(methodCaller1);

		IMethodCaller methodCaller2 = MethodSearch.getMethodCaller("method1",
				new IOpenClass[] { JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.STRING }, castFactory,
				javaOpenClass);

		Assert.assertNull(methodCaller2);
	}

	@Test
	public void testMethodInheritance() {
		ICastFactory castFactory = getCastFactory();

		JavaOpenClass javaOpenClass = JavaOpenClass.getOpenClass(ClassWithMethods.class);
		JavaOpenClass javaOpenClass2 = JavaOpenClass.getOpenClass(SecondClassWithMethods.class);
		JavaOpenClass javaOpenClass3 = JavaOpenClass.getOpenClass(ThirdClassWithMethods.class);
		assertNotNull(javaOpenClass.getMethod("method2", new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }));
		assertNotNull(
				javaOpenClass2.getMethod("method2", new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }));

		assertNotNull(MethodSearch.getMethodCaller("method2",
				new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }, castFactory, javaOpenClass2));

		assertNotNull(MethodSearch.getMethodCaller("method3",
				new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }, castFactory, javaOpenClass2));

		assertNotNull(MethodSearch.getMethodCaller("method3",
				new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }, castFactory, javaOpenClass3));
	}

}
