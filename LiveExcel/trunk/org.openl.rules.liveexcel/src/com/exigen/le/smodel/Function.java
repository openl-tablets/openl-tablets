package com.exigen.le.smodel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * ServiceModel Function
 * @author vabramovs
 *
 */
public class Function {
	
	public static final String EFFECTIVE_DATE ="Effective Date";
	
	private static final Log LOG = LogFactory.getLog(Function.class);

	String excel; // excel file name
	String sheet; // sheet in excel
	String name; // function name
	String returnArea; // String with returnSpace declaration use for exchange purpose
	@XmlTransient  ExcelSpace returnSpace = null; // return - internal use 
	Type returnType=null; // reference to existing type or null, if no return type
						// defined
	String returnTypeName; // Name of existing type or null, if no return type
	boolean isReturnCollection = false;

	// defined
	String functionDescription; // function description
	String definitionArea; // String with definitionRange declaration use for exchange purpose
	Range definitionRange; // definition range
	List<FunctionArgument> arguments = new ArrayList<FunctionArgument>();
	Map<String, String> attributes; // function attribute: effective date,
									// service, arg. type for service etc.
	
	// TODO delete after real service definition
	boolean service;
	public void setService(boolean service){
		this.service = service;
	}
	
	public Function(){
		
	}
	
	// params should be in pairs: (name, inputArea)
	/**
	 * @param excel Excel file name
	 * @param sheet Sheet name
	 * @param name  Function name
	 * @param returnArea Location of return value
	 * @param args Arguments in pairs: description, returnArea
	 */
	public Function(String excel, String sheet, String name, String returnArea, String...args){
		this.excel = excel;
		this.sheet = sheet;
		this.name = name;
		this.returnArea = returnArea;
		if (args.length %2 !=0){
			throw new RuntimeException("Args should be odd: " + args.length);
		}
		for(int i=0; i<args.length; i=i+2){
			arguments.add(new FunctionArgument(args[i], args[i+1]));
		}
		
	}
	
	public Date getEffectiveDate() {
		try {
			String ed = attributes.get(EFFECTIVE_DATE);
			DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
			if(ed != null){
					Date date = df.parse(ed);
					return date;
			}
		} catch (Exception e) {
		}
		String msg = "Undefined or wrong "+EFFECTIVE_DATE;
		LOG.trace(msg);
		return null;
	}
	
	public boolean isService(){
		// TODO - review service definition
		return service;
	}
	
	public Type getArgumentType(){
		//TODO  returns argument type or null for service. Always null for functions 
		return null;
	}
	

	public String getExcel() {
		return excel;
	}

	public void setExcel(String excel) {
		this.excel = excel;
	}

	public String getSheet() {
		return sheet;
	}

	public void setSheet(String sheet) {
		this.sheet = sheet;
	}

    /**
     * @return the name in upper cÅase
     */
    public String getName() {
        return name.toUpperCase();
    }

    /**
     * @return the declared name
     */
    public String getDeclaredName() {
        return name;
    }

	public void setName(String name) {
		this.name = name;
	}

	// This getter begin with "obtain" to exclude this field from JAXB conversion
	public ExcelSpace obtainReturnSpace() {
		if(returnSpace==null){
			returnSpace = ExcelSpace.Factory.create(returnArea);
		}
		return returnSpace;
	}

	public void setReturnSpace(ExcelSpace returnSpace) {
		this.returnSpace = returnSpace;
		returnArea=returnSpace.toString();
	}

	/**
	 * @return the returnArea
	 */
	public String getReturnArea() {
		return returnArea;
	}

	/**
	 * @param returnArea the returnArea to set
	 */
	public void setReturnArea(String returnArea) {
		this.returnArea = returnArea;
		this.returnSpace = null;
	}


	public Type getReturnType() {
		return returnType;
	}

	public void setReturnType(Type returnType) {
		this.returnType = returnType;
	}
	/**
	 * @return the returnTypeName
	 */
	public String getReturnTypeName() {
		return returnTypeName;
	}

	/**
	 * @param returnTypeName the returnTypeName to set
	 */
	public void setReturnTypeName(String returnTypeName) {
		this.returnTypeName = returnTypeName;
	}
	/**
	 * @return the isReturnCollection
	 */
	public boolean isReturnCollection() {
		return isReturnCollection;
	}

	/**
	 * @param isReturnCollection the isReturnCollection to set
	 */
	public void setReturnCollection(boolean isReturnCollection) {
		this.isReturnCollection = isReturnCollection;
	}

	public String getFunctionDescription() {
		return functionDescription;
	}

	public void setFunctionDescription(String functionDescription) {
		this.functionDescription = functionDescription;
	}

	/**
	 * @return the definitionArea
	 */
	public String getDefinitionArea() {
		return definitionArea;
	}

	/**
	 * @param definitionArea the definitionArea to set
	 */
	public void setDefinitionArea(String definitionArea) {
		this.definitionArea = definitionArea;
		this.definitionRange = null;
	}
	// This getter begin with "obtain" to exclude this field from JAXB conversion
	public Range obtainDefinitionRange() {
		if(definitionRange==null){
			definitionRange = new Range().init(definitionArea);
		}
		return definitionRange;
	}

	public void setDefinitionRange(Range definitionRange) {
		this.definitionRange = definitionRange;
		this.definitionArea = definitionRange.toString();
	}

	public List<FunctionArgument> getArguments() {
		return arguments;
	}
	
	public FunctionArgument getArgument(int ordinalIndex){
		for(FunctionArgument arg:arguments){
			if(arg.getOrdination()==ordinalIndex){
				return arg;
			}
		}
		return null;
	}

	public void setArguments(List<FunctionArgument> arguments) {
		
//		TreeSet<FunctionArgument> sorted = new TreeSet<FunctionArgument>(arguments.get(0));
//		sorted.addAll(arguments);
//		this.arguments = new ArrayList<FunctionArgument>(sorted);
		this.arguments = arguments;
	}
	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	/** 
	 * Find function in Set by name, return null if no
	 * @param functionName
	 * @param functionSet
	 * @return
	 */
	public static Function findFunction(String functionName, List<Function> functionSet){
		Function answer = null;
		for(Function func: functionSet){
			if(func.getName().equalsIgnoreCase(functionName)){
				answer = func;
			}
		}
		return answer;
	}
	public static class FunctionArgument implements Comparator<FunctionArgument> {
		int ordination;     // Argument ordination index: first arg has 0;second- 1 and so on 
		String description;   
		String inputArea;
		ExcelSpace input=null;
		Type type=null;
		String typeName;

		public FunctionArgument(){
			
		}
		
		public FunctionArgument(String typeName, String inputArea){
			this.typeName = typeName;
			this.inputArea = inputArea;
		}
		public int compare(FunctionArgument o1, FunctionArgument o2) {
				return o2.ordination-o1.ordination;
		}
	
		/**
		 * @return the ordination
		 */
		public int getOrdination() {
			return ordination;
		}

		/**
		 * @param ordination the ordination to set
		 */
		public void setOrdination(int ordination) {
			this.ordination = ordination;
		}

		/**
		 * @return the type
		 */
		public Type getType() {
			return type;
		}

		/**
		 * @param type the type to set
		 */
		@XmlTransient
		public void setType(Type type) {
			this.type = type;
		}
		
		/**
		 * @return the typeName
		 */
		public String getTypeName() {
			return typeName;
		}

		/**
		 * @param typeName the typeName to set
		 */
		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public ExcelSpace obtainInput() {
			if(input==null){
				input = ExcelSpace.Factory.create(inputArea);
			}
			return input;
		}

		/**
		 * @return the inputArea
		 */
		public String getInputArea() {
			return inputArea;
		}

		/**
		 * @param inputArea the inputArea to set
		 */
		public void setInputArea(String inputArea) {
			this.inputArea = inputArea;
			this.input=null;
		}

		public void setInput(ExcelSpace input) {
			this.input = input;
			this.inputArea = input.toString();
		}

	}

}
