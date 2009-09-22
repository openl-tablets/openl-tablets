package org.openl.codegen.tools;

import java.io.IOException;

import org.openl.codegen.FileCodeGen;
import org.openl.codegen.ICodeGenAdaptor;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.DefaultTableProperties;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.types.IOpenClass;
import org.openl.util.StringTool;

public class GenTableProperties {

	private static final String SOURCE_LOC = "../org.openl.rules/src/";
	
	/**
	 * Replace TMP_FILE with null, it will generate java "in-place"
	 */
	private static final String TMP_FILE = null;
	
	public void run() throws IOException {
		
		FileCodeGen fileGen;
		
		fileGen = new FileCodeGen(SOURCE_LOC + StringTool.getFileNameOfJavaClass(ITableProperties.class), TMP_FILE, null);
		
		fileGen.processFile(new ICodeGenAdaptor() {
			
			public void processInsertTag(String line, StringBuilder sb) {
				
				sb.append(genInterfaceDeclarations(DefaultPropertyDefinitions.getDefaultDefinitions()));
				
			}
			
			private String genInterfaceDeclarations(TablePropertyDefinition[] tablePropertyDefinitions) {
				
				StringBuilder stringBuilder = new StringBuilder();
				
				for (TablePropertyDefinition tablePropertyDefinition : tablePropertyDefinitions) {
					
					String name = tablePropertyDefinition.getName();
					IOpenClass openClass = tablePropertyDefinition.getType();
					
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
		
		fileGen = new FileCodeGen(SOURCE_LOC + StringTool.getFileNameOfJavaClass(DefaultTableProperties.class), TMP_FILE, null);
		
		fileGen.processFile(new ICodeGenAdaptor() {
			
			public void processInsertTag(String line, StringBuilder sb) {
				
				sb.append(genMethods(DefaultPropertyDefinitions.getDefaultDefinitions()));
			}
			
			private String genMethods(TablePropertyDefinition[] tablePropertyDefinitions) {
				
				StringBuilder stringBuilder = new StringBuilder();
				
				for (TablePropertyDefinition tablePropertyDefinition : tablePropertyDefinitions) {
					
					String name = tablePropertyDefinition.getName();
					IOpenClass openClass = tablePropertyDefinition.getType();
					
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
		new GenTableProperties().run();
	}

}
