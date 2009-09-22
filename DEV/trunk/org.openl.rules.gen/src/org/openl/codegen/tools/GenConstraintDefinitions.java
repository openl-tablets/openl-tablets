package org.openl.codegen.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openl.codegen.FileCodeGen;
import org.openl.codegen.ICodeGenAdaptor;
import org.openl.rules.context.properties.DefaultConstraintDefinitions;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.util.StringTool;

public class GenConstraintDefinitions {
	
	private static final String SOURCE_LOC = "../org.openl.rules/src/";
	
	/**
	 * Replace TMP_FILE with null, it will generate java "in-place"
	 */
	private static final String TMP_FILE = null;
	
	public void run() throws IOException {
		
		FileCodeGen fileGen = new FileCodeGen(SOURCE_LOC
		        + StringTool.getFileNameOfJavaClass(DefaultConstraintDefinitions.class), TMP_FILE, null);
		
		fileGen.processFile(new ICodeGenAdaptor() {
			
			public void processInsertTag(String line, StringBuilder sb) {
				
				sb.append(genConstraintDeclarations(DefaultPropertyDefinitions.getDefaultDefinitions()));
				
			}
			
			private String genConstraintDeclarations(TablePropertyDefinition[] tablePropertyDefinitions) {
				
				StringBuilder stringBuilder = new StringBuilder();
				TablePropertyDefinitionWrapper[] dimensionalTablePropertyDefinitions = getDimensionalTablePropertyDefinitions(tablePropertyDefinitions);
				
				stringBuilder
						.append("definitions = new ConstraintDefinition[" + dimensionalTablePropertyDefinitions.length + "];")
						.append("\n");
				
				for (int i = 0; i < dimensionalTablePropertyDefinitions.length; i++) {
					TablePropertyDefinitionWrapper wrapper = dimensionalTablePropertyDefinitions[i];
					TablePropertyDefinition definition = wrapper.getTablePropertyDefinition();
					
					String type = definition.getType().getInstanceClass().getName();
					String name = definition.getName();
					String expression = definition.getExpression();
					
					stringBuilder
							.append("definitions[" + i + "] = new ConstraintDefinition(tablePropertyDefinitions[" + wrapper.getIndex() + "], new IMatcher() {")
							.append("\n")
							.append("public boolean isMatch(IRulesContext context, ITableProperties tableProperties ) {")
							.append("\n");
					
					if (expression == null || "".equals(expression)) {
						stringBuilder.append("return true;");
					} else {
					
					stringBuilder
							.append(type)
							.append(" ")
							.append(name)
							.append(" = ")
							.append("tableProperties.get")
							.append(name.substring(0, 1).toUpperCase())
							.append(name.substring(1, name.length()))
							.append("();")
							.append("\n")
					
							.append("if (")
							.append(name)
							.append(" == null) { return true;}")
							.append("\n")
					
							.append("return")
							.append(" ")
							.append(expression)
							.append(";");
							
					}
					
					stringBuilder.append("}")
							.append("\n")
							.append("});");
				}
				
				return stringBuilder.toString();
			}
		});
	}
	
	private TablePropertyDefinitionWrapper[] getDimensionalTablePropertyDefinitions(
	        TablePropertyDefinition[] tablePropertyDefinitions) {
		
		List<TablePropertyDefinitionWrapper> dimensionalTablePropertyDefinitions = new ArrayList<TablePropertyDefinitionWrapper>();
		
		for (int i = 0; i < tablePropertyDefinitions.length; i++) {
			
			TablePropertyDefinition tablePropertyDefinition = tablePropertyDefinitions[i];
			
			if (tablePropertyDefinition.isDimensional()) {
				TablePropertyDefinitionWrapper wrapper = new TablePropertyDefinitionWrapper(tablePropertyDefinition, i);
				dimensionalTablePropertyDefinitions.add(wrapper);
			}
		}
		
		return dimensionalTablePropertyDefinitions
		        .toArray(new TablePropertyDefinitionWrapper[dimensionalTablePropertyDefinitions.size()]);
	}
	
	public static void main(String[] args) throws IOException {
		new GenConstraintDefinitions().run();
	}
	
	private class TablePropertyDefinitionWrapper {
		private TablePropertyDefinition tablePropertyDefinition;
		private int index;
		
		public TablePropertyDefinitionWrapper(TablePropertyDefinition tablePropertyDefinition, int index) {
	        super();
	        this.tablePropertyDefinition = tablePropertyDefinition;
	        this.index = index;
        }

		public TablePropertyDefinition getTablePropertyDefinition() {
        	return tablePropertyDefinition;
        }

		public int getIndex() {
        	return index;
        }
	}
}
