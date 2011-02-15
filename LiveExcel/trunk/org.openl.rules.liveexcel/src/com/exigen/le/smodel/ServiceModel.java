/**
 * 
 */
package com.exigen.le.smodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.evaluator.selector.SelectorFactory;


/**
 * LE Service 
 * @author vabramovs
 *
 */
@XmlRootElement
public class ServiceModel {
	
	private static final Log LOG = LogFactory.getLog(ServiceModel.class);
	
	List<Type> types;
	List<Function>  functions;
	List<TableDesc>  tables;
	
	
  /**
 *  This is dummy constructor to create init "dummy"ServiceModel  in ThreadEvaluationContext
 */
public ServiceModel(){
		   types = new ArrayList<Type>(); 
		   functions = new ArrayList<Function>();
		   tables  = new ArrayList<TableDesc>();
	  }

/**
 * @param types
 * @param functions
 * @param tables
 */
public ServiceModel(List<Type> types, List<Function> functions,
		List<TableDesc> tables) {
	super();
	this.types = types;
	this.functions = functions;
	this.tables = tables;
}
/**
 * @return the types
 */
public List<Type> getTypes() {
	return types;
}

/**
 * @return the types
 */
public Type getType(String typeName) {
	// try primitive
	Type primitive = Type.Primary.getTypeByNameOrClass(typeName);
	if (primitive != null){
		return primitive;
	}
	
	String path=typeName+"";
	String[] segments = path.split("\\.");
	List<Type> candidates = getTypes();
	Type current = null;
	for(int i=0;i<segments.length;i++){
		String segment = segments[i];
		if(segment==null || segment.equals("null")){
			System.out.println("Problem parsing "+typeName);
		}
		current = getType(segment, candidates );
		candidates = new ArrayList<Type>();
		if(i<(segments.length-1)){
			for(Property child:current.getChilds()){
				if(child.getType()!=null)
					candidates.add(child.getType());
			}
		}
	}
	return current;
}
private Type getType(String typeName,List<Type> candidates ){
	for(Type type:candidates){
		if(type.getName().equalsIgnoreCase(typeName)){
			return type;
		}
	}
	String msg = "Type "+typeName+ " is not found in Service Model";
	LOG.error(msg);
	throw new RuntimeException(msg);
	
}
/**
 * Get type by fully qualified path from SM root
 * @param root
 * @param path
 * @return
 */
public static Type getTypeByPath(Type root,String path) {
	Vector<String> segments = new Vector<String>(Arrays.asList(path.split("\\.")));
	if(root.getName().equalsIgnoreCase(segments.get(0)))  // Path contains optional root name
		segments.remove(0);
	Type type = root;
	boolean found = true;
	while(segments.size()>0 && found){
		found = false;
		for(Property prop:type.getChilds()){
			if(prop.getName().equalsIgnoreCase(segments.get(0))){
				type = prop.getType();
				segments.remove(0);
				found = true;
				break;
			}
		}
	}
	if(found)
		return type;
	else
		return null;
}

/**
 * @return the functions
 */
public List<Function> getFunctions() {
	return functions;
}
/**
 * Get service functions name
 * Only service functions may be invoked outside
 * @return
 */
public List<Function> getServiceFunctions(){
	List<Function> answer = new ArrayList<Function>();	
	List<Function> funcs =getFunctions();
	for(Function func:funcs){
		if(func.isService()){
	   		Function function = SelectorFactory.getInstance().getFunctionSelector().selectFunction(func.getName(),funcs,ThreadEvaluationContext.getInstance());
	   		if(!answer.contains(function))
	   			answer.add(function);
		}
	}
	return answer;
}
/**
 * @return list of unique function names
 */
public List<String> getUniqueFunctionName(){
	List<String> result = new ArrayList<String>();
	for(Function func:functions){
		if(!result.contains(func.getName()))
			result.add(func.getName());
	}
	return result;
}

/**
 * @return the tables
 */
public List<TableDesc> getTables() {
	return tables;
}

/**
 * @param types the types to set
 */
public void setTypes(List<Type> types) {
	this.types = types;
}

/**
 * @param functions the functions to set
 */
public void setFunctions(List<Function> functions) {
	this.functions = functions;
}

/**
 * @param tables the tables to set
 */
public void setTables(List<TableDesc> tables) {
	this.tables = tables;
}

}
