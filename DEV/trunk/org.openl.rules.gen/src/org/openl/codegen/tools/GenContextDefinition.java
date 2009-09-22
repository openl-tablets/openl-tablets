package org.openl.codegen.tools;

import java.io.IOException;

import org.openl.codegen.FileCodeGen;
import org.openl.codegen.ICodeGenAdaptor;
import org.openl.codegen.JavaCodeGen;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.types.IOpenClass;
import org.openl.util.StringTool;
import org.openl.rules.context.properties.ContextPropertyDefinition;
import org.openl.rules.context.properties.DefaultContextPropertyDefinitions;
import org.openl.rules.context.DefaultRulesContext;
import org.openl.rules.context.IRulesContext;

public class GenContextDefinition {
	
	private static final String SOURCE_LOC = "../org.openl.rules/src/";
	
	/**
	 * Replace TMP_FILE with null, it will generate java "in-place"
	 */
	private static final String TMP_FILE = null;
	
	private static final String ARRAY_NAME = "definitions";
	
	private static final String DEFINITIONS_XLS = "../org.openl.rules/doc/TablePropertyDefinition.xlsx";
	
	private ContextPropertyDefinition[] contextPropertyDefinitions;
	
	private ContextPropertyDefinition[] loadDefinitions() {
		
		RuleEngineFactory<IDefinitionLoader> rf = new RuleEngineFactory<IDefinitionLoader>(DEFINITIONS_XLS,
		        IDefinitionLoader.class);
		
		return rf.newInstance().getContextDefinitions();
	}
	
	public void run() throws IOException {
		
		// Load definitions from file.
		// 
		contextPropertyDefinitions = loadDefinitions();
		
		FileCodeGen fileGen = new FileCodeGen(SOURCE_LOC
		        + StringTool.getFileNameOfJavaClass(DefaultContextPropertyDefinitions.class), TMP_FILE, null);
		
		fileGen.processFile(new ICodeGenAdaptor() {
			
			public void processInsertTag(String line, StringBuilder sb) {
				JavaCodeGen jcgen = new JavaCodeGen();
				
				jcgen.setGenLevel(JavaCodeGen.METHOD_BODY_LEVEL);
				jcgen.genInitializeBeanArray(ARRAY_NAME, contextPropertyDefinitions, ContextPropertyDefinition.class,
				        null, sb);
				
			}
			
		});
		
		fileGen = new FileCodeGen(SOURCE_LOC + StringTool.getFileNameOfJavaClass(IRulesContext.class), TMP_FILE, null);
		
		fileGen.processFile(new ICodeGenAdaptor() {
			
			public void processInsertTag(String line, StringBuilder sb) {
				
				sb.append(genInterfaceDeclarations(contextPropertyDefinitions));
				
			}
			
			private String genInterfaceDeclarations(ContextPropertyDefinition[] contextPropertyDefinitions) {
				
				StringBuilder stringBuilder = new StringBuilder();
				
				for (ContextPropertyDefinition contextPropertyDefinition : contextPropertyDefinitions) {
					
					String name = contextPropertyDefinition.getName();
					IOpenClass openClass = contextPropertyDefinition.getType();
					
					stringBuilder
							.append(openClass.getInstanceClass().getName())
							.append(" ")
							.append("get")
							.append(name.substring(0, 1).toUpperCase())
							.append(name.substring(1, name.length()))
							.append("()")
					        .append(";");
					
					stringBuilder.append("\n");
					
					stringBuilder
							.append("void")
							.append(" ")
							.append("set")
							.append(name.substring(0, 1).toUpperCase())
					        .append(name.substring(1, name.length()))
					        .append("(")
					        .append(openClass.getInstanceClass().getName())
					        .append(" ")
					        .append(name)
					        .append(")")
					        .append(";");
					
					stringBuilder.append("\n");
				}
				
				return stringBuilder.toString();
			}
		});
		
		fileGen = new FileCodeGen(SOURCE_LOC + StringTool.getFileNameOfJavaClass(DefaultRulesContext.class), TMP_FILE, null);
		
		fileGen.processFile(new ICodeGenAdaptor() {
			
			public void processInsertTag(String line, StringBuilder sb) {
				
				sb.append(genMethods(contextPropertyDefinitions));
			}
			
			private String genMethods(ContextPropertyDefinition[] contextPropertyDefinitions) {
				
				StringBuilder stringBuilder = new StringBuilder();
				
				for (ContextPropertyDefinition contextPropertyDefinition : contextPropertyDefinitions) {
					
					String name = contextPropertyDefinition.getName();
					IOpenClass openClass = contextPropertyDefinition.getType();
					
					stringBuilder
						.append("public")
						.append(" ")
						.append(openClass.getInstanceClass().getName())
						.append(" ")
						.append("get")
						.append(name.substring(0, 1).toUpperCase())
						.append(name.substring(1, name.length()))
						.append("()")
						.append("{")
						.append("\n")
						.append("return")
						.append(" ")
						.append(String.format("(%s)", openClass.getInstanceClass().getName()))
						.append(String.format("internalMap.get(\"%s\");", name))
					    .append("\n")
					    .append("}");
					
					stringBuilder.append("\n");
					
					stringBuilder
						.append("public")
						.append(" ")
						.append("void")
						.append(" ")
						.append("set")
						.append(name.substring(0, 1).toUpperCase())
					    .append(name.substring(1, name.length()))
					    .append("(")
					    .append(openClass.getInstanceClass().getName())
					    .append(" ")
					    .append(name)
					    .append(")")
						.append("{")
						.append("\n")
						.append(String.format("internalMap.put(\"%s\", %s);", name, name))
					    .append("\n")
					    .append("}");
					
					stringBuilder.append("\n");
				}
				
				return stringBuilder.toString();
            }
		});
	}
	
	public static void main(String[] args) throws IOException {
		new GenContextDefinition().run();
	}
	
	public static interface IDefinitionLoader {
		ContextPropertyDefinition[] getContextDefinitions();
	}
}
