package org.openl.codegen;

import junit.framework.Assert;
import junit.framework.TestCase;

public class JavaCodeGenTest extends TestCase {

	public void testGenClass() {
		//fail("Not yet implemented");
	}

	public void testGenField() {
		//fail("Not yet implemented");
	}

	public void testGenLiteralArray() {
		//fail("Not yet implemented");
	}

	public void testGenLiteralChar() {
		//fail("Not yet implemented");
	}

	public void testGenLiteralDouble() {
		//fail("Not yet implemented");
	}

	public void testGenLiteralInt() {
		//fail("Not yet implemented");
	}

	public void testGenLiteralString() {
		//fail("Not yet implemented");
	}

	public void testGenLiteralNull() {
		//fail("Not yet implemented");
	}

	public void testGenMethod() {
		//fail("Not yet implemented");
	}

	public void testGenMethodEnd() {
		//fail("Not yet implemented");
	}

	public void testGenMethodStart() {
		//fail("Not yet implemented");
	}

	public void testGenMultiLineComment() {
		//fail("Not yet implemented");
	}

	public void testGenSingleLineComment() {
		//fail("Not yet implemented");
	}

	public void testGenClassEnd() {
		//fail("Not yet implemented");
	}

	public void testGenClassStart() {
		//fail("Not yet implemented");
	}

	public void testGenModuleEnd() {
		//fail("Not yet implemented");
	}

	public void testGenModuleStart() {
		//fail("Not yet implemented");
	}

	public void testGenBeanAttribute() {
		//fail("Not yet implemented");
	}

	public void testGenEscapedChar() {
		Assert.assertEquals("a", cg.genEscapedChar('a', sb()).toString() );
		Assert.assertEquals("\\n", cg.genEscapedChar('\n', sb()).toString() );
		Assert.assertEquals("\\033", cg.genEscapedChar('\033', sb()).toString() );
		Assert.assertEquals("\\uab7c", cg.genEscapedChar('\uab7c', sb()).toString() );
	}

	public void testGenAttribute() {
		//fail("Not yet implemented");
	}

	public void testGenLiteralBool() {
		//fail("Not yet implemented");
	}






	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
	}
	
	JavaCodeGen cg = new JavaCodeGen("org.test.codegen", "Test", 0);
	StringBuilder sb(){return new StringBuilder();}

}
