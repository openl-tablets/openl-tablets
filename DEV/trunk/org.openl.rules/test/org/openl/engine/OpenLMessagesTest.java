package org.openl.engine;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.message.OpenLMessages;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.runtime.EngineFactory;
import org.openl.syntax.exception.CompositeOpenlException;

public class OpenLMessagesTest {
	
	public static final String src1 = "test/rules/messages/project1.xls";
	public static final String src2 = "test/rules/messages/project2.xlsx";
	
	@Test
	public void testInSeriesCompileMessages1() {
		// test using wrapper generation
		//
		BaseOpenlBuilderHelper helper1 = new BaseOpenlBuilderHelper() {};
		helper1.build(src1);
		OpenLMessages messages = OpenLMessages.getCurrentInstance();
		assertEquals("Should be one message from current module", 1, messages.getMessages().size());
		
		BaseOpenlBuilderHelper helper2 = new BaseOpenlBuilderHelper() {};
		helper2.build(src2);
		OpenLMessages messages1 = OpenLMessages.getCurrentInstance();
		assertEquals("Messages should be 5, just from current module", 5, messages1.getMessages().size());
	}
	
	public interface Project1Int {
		String hello(int hour);
	}
	
	public interface Project2Int {
		int test(int a);
	}
	
	@Test
	public void testInSeriesCompileMessages2() {
		// test using engine factory
		//
		String sourceType = "org.openl.xls";       

        EngineFactory<Project1Int> engineFactory = new EngineFactory<Project1Int>(sourceType, src1, Project1Int.class);
        engineFactory.setExecutionMode(false);
        try {
        	engineFactory.makeInstance();
        } catch (CompositeOpenlException e) {
		} 
        OpenLMessages messages = OpenLMessages.getCurrentInstance();
		assertEquals("Should be one message from current module", 1, messages.getMessages().size());
		
		EngineFactory<Project2Int> engineFactory1 = new EngineFactory<Project2Int>(sourceType, src2, Project2Int.class);
        engineFactory.setExecutionMode(false);
        try {
        	engineFactory1.makeInstance();
        } catch(CompositeOpenlException ex) {
        	
        }
        OpenLMessages messages1 = OpenLMessages.getCurrentInstance();
		assertEquals("Messages should be 5, just from current module", 5, messages1.getMessages().size());
	}
}
