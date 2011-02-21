/** 
 * Class to support Excel Range witch  contains typed Object
 * 
 */
package com.exigen.le.evaluator.function.TypifiedRange;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

//import org.apache.commons.beanutils.LazyDynaBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.usermodel.DateUtil;

import com.exigen.le.evaluator.DataPool;
import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.servicedescr.evaluator.CollectionBeanWrapper;
import com.exigen.le.servicedescr.evaluator.MapWrapper;
import com.exigen.le.smodel.Primary;
import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.Range;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.accessor.CollectionValueHolder;
import com.exigen.le.smodel.accessor.ValueHolder;

/**
 * @author vabramovs
 *
 */
public class TypifiedRange {
	
	private static final Log LOG = LogFactory.getLog(TypifiedRange.class);

	private Boolean horizontal = null;   
	// Range may be one of two form:
	// Horizontal    ElementTitle1 | ElementTitle2 ...
	//               ElementValue1 | ElementValue2 ...
	// or Vertical   ElementTitle1 | ElementValue1
	//               ElementTitle2 | ElementValue2
	// 				 ....
	protected Type type;
	protected Range range;
	private ServiceModel serviceModel;
	private  EvaluationSheet sheet;
	private WorkbookEvaluator wb; 
	private boolean is1904BaseDate = false;
	
	
	public TypifiedRange(Type type,String sheetName,Range range, ServiceModel serviceModel,OperationEvaluationContext ec){
		this.range = range;
		this.serviceModel = ThreadEvaluationContext.getServiceModel();
		this.type = type;
		this.sheet=ec.getWorkbook().getSheet(ec.getWorkbook().getSheetIndex(sheetName));
		wb = ec.getWorkbookEvaluator();
		is1904BaseDate = wb.getEvaluationWorkbook().getWorkbook().isDate1904();
		
	}

	private Boolean checkHorizontal() {
				Type objectType = type;
				// Exclude artificial additional level that emulate collection
				if(objectType.getName().length()==0 && objectType.getChilds().size()==1){
					objectType = objectType.getChilds().get(0).getType();
				}
				Boolean result = Boolean.TRUE;
	       		for(int colInd= range.from().getColumnIndex();colInd<=range.to().getColumnIndex();colInd++){
	       			EvaluationCell cellInTitle = sheet.getCell(range.from().getRowIndex(), colInd);
	       			ValueEval title = wb.evaluate(cellInTitle); 
	       			if(! (title instanceof StringEval)){
	       				return Boolean.FALSE;
	       			}
	       			else{
	       				if(serviceModel.getTypeByPath(objectType,canonizeName(((StringEval)title).getStringValue()))==null){
	       						String msg = "Title "+ title+" does not match any type"; 
	       						LOG.warn(msg);
	       						System.out.println(msg);
		       			 		return Boolean.FALSE;
	       					}
	       				}
	       		}
		return result;
	}
	private static  String canonizeName(String name){
		return name.replaceAll(" ", "_");
	}
	private Boolean checkVertical() {
		Type objectType = type;
		// Exclude artificial additional level that emulate collection
		if(objectType.getName().length()==0 && objectType.getChilds().size()==1){
			objectType = objectType.getChilds().get(0).getType();
		}
		Boolean result = Boolean.TRUE;
   		for(int rowInd= range.from().getRowIndex();rowInd<=range.to().getRowIndex();rowInd++){
   			EvaluationCell cellInTitle = sheet.getCell(rowInd,range.from().getColumnIndex());
   			ValueEval title = wb.evaluate(cellInTitle); 
   			if(! (title instanceof StringEval)){
   				return Boolean.FALSE;
   			}
   			else{
   				if(serviceModel.getTypeByPath(objectType, ((StringEval)title).getStringValue())==null){
					String msg = "Title "+ title+" does not match any type"; 
						LOG.warn(msg);
						System.out.println(msg);
      				return Boolean.FALSE;
   				}
   			}
   		}
return result;
}
	
	public boolean isHorizontal(){
		if(horizontal==null){
			horizontal =checkHorizontal();
			if(horizontal==false){
				if(checkVertical()==false){
					String msg = "Range "+range+" has no title or title contains wrong name(s).";
					LOG.error(msg);
					throw new RuntimeException(msg);
				}
			}
		}
		return horizontal.booleanValue();
	}
	public boolean isVertical(){
		return !isHorizontal();
	}
	public Item getItem(String rootName, int rowIndex, int colIndex){
			boolean titleItself = false;
			EvaluationCell cellInTitle = null;
			if(isHorizontal()){
      			cellInTitle = sheet.getCell(range.from().getRowIndex(), colIndex);
				if(rowIndex==range.from().getRowIndex()){ // Title itself
					titleItself = true;
				}
			}
			else {
				cellInTitle = sheet.getCell(rowIndex, range.from().getColumnIndex());
 				if(colIndex==range.from().getColumnIndex()){ // Title itself
					titleItself = true;
				}
			}
			String title = normalizeTitle(wb.evaluate(cellInTitle));
			if((title  == null)){
				String msg = "For typified range cell ("+range.from().getSheetName()+","+rowIndex+","+colIndex+" ) has no correct title";
				LOG.error(msg);
				throw new RuntimeException(msg);
			}
			if(!titleItself){
				EvaluationCell cell = sheet.getCell(rowIndex, colIndex);
				if(cell != null){
					ValueEval value = wb.evaluate(cell);
					return new Item(rootName,title,value);
				}
				else{
					return new Item(rootName,title,BlankEval.instance);
				}
			}
			else {
				return new Item(rootName,title,BlankEval.instance);
				
			}
	}
	private String normalizeTitle(ValueEval title) {
		// Normalize title - convert it to Upper, add Highest parent if absent
		if(!(title instanceof StringEval)){
			return null;
		}
		String strValue = ((StringEval)title).getStringValue().toUpperCase();
		String parentName = type.getName().toUpperCase();
		if(parentName.length()==0 && type.getChilds().size()==1){ // Artificial node to keep highest collection
			parentName = type.getChilds().get(0).getType().getName();
		}
		if(!strValue.startsWith(parentName+".")){
			strValue=parentName+"."+strValue;
		}
		return strValue;
	}

	public List<Item> getTitleList(){
		return getVirtualRow(0).getContent();	
	}
	public VirtualRow getVirtualRow(int index){
		VirtualRow result = new VirtualRow(); 
		List<Item> content = new ArrayList<Item>();
		if(isHorizontal()){
			for(int colIndex=range.from().getColumnIndex();colIndex<=range.to().getColumnIndex();colIndex++){
				content.add(getItem(type.getName(),range.from().getRowIndex()+index, colIndex));
			}
		}
		else{
			for(int rowIndex=range.from().getRowIndex();rowIndex<=range.to().getRowIndex();rowIndex++){
				content.add(getItem(type.getName(),rowIndex,range.from().getColumnIndex()+index));
			}
			
		}
		result.setContent(content);
	return result;	
		
	}
	
	public ValueHolder getObject(){
//		Node node = Node.buildNodeStructure(type,type.getName());
//		ValueHolder root  = new BeanWrapper(new LazyDynaBean(),type);;
		ValueHolder root  = new MapWrapper(type);;
		Iterator<VirtualRow> it = new VirtualRowIterator(this);
		VirtualRow previous = null;
		while(it.hasNext()){
			VirtualRow row = it.next();
			// First row is expected as Title row and is considered as Blank row 
			if(!row.isBlank()){
				row = replenishRow(row,previous);
				decomposeByHierarchy(row, root,type.getName(),type,type.getName(),false);
				previous = row;
			}
		}
		ValueHolder result  = normalizeBean(root,type);
	return result;
}
   private VirtualRow replenishRow(VirtualRow row, VirtualRow previous) {
	   for(Item item:row.getContent()){
		   if(item.getValue() instanceof BlankEval){
			   //TODO Prev and current has the same order of element ?
			   int index = row.getContent().indexOf(item);
			   item.setValue(previous.getContent().get(index).getValue());
			   item.setWasBlank(true);
		   }
		   else{
			   item.setWasBlank(false);
		   }
	   }
	   return row;
	}

private ValueHolder normalizeBean(ValueHolder valueHolder, Type type ) {
	   // Normalizing include:
	   // replace all List<primitive type> to primitive type[]
//	   ValueHolder result = new BeanWrapper(new LazyDynaBean(),valueHolder.getModel());
	   ValueHolder result = new MapWrapper(type);
	   
	   for(Property child:type.getChilds()){
		   if(child.getType().isComplex()){
			   if(child.isCollection()) {
				   for(int i=0;i<valueHolder.size(child.getName());i++){
					   result.set(child.getName(),i, normalizeBean((ValueHolder)valueHolder.getValue(child.getName(),i),child.getType()));
				   }
			   }
			   else {
				   result.set(child.getName(),normalizeBean((ValueHolder)valueHolder.getValue(child.getName()),child.getType()));
			   }
		   }
		   else{  // Primitive
			   if(child.isCollection()){
				   Object value = valueHolder.getValue(child.getName());
				   if(value instanceof CollectionValueHolder){
					   int size = ((CollectionValueHolder)value).size();
					   Primary primitiveType = Primary.getPrimary(child.getType());
					   Object array = Array.newInstance(primitiveType.getJavaClass(), size);
					   for(int i=0;i<size;i++){
						   Object elem = ((CollectionValueHolder)value).getValue(i);
						   if(elem instanceof ValueHolder){
							   elem = ((ValueHolder)elem).getValue(child.getName());
						   }
						   Array.set(array, i, elem);  
					   }
					   result.set(child.getName(), array);
				   }
				   if(value.getClass().isArray()){
					   result.set(child.getName(), value);
				   }
			   }
			   else {
				   result.set(child.getName(), valueHolder.getValue(child.getName()));
			   }
		   }
	   }
	   return result;
	}
private void decomposeByHierarchy(VirtualRow row,ValueHolder valueHolder,String pathDone,Type type,String propName, boolean isCollection ){
		   if(type.isComplex()){ // Type has childs
			   for(Property prop:type.getChilds()){
				   String childPath =prop.getName();
				   if(pathDone.length()>0){
					   childPath = pathDone+"."+prop.getName();
				   }
				   childPath = childPath.toUpperCase();
				   VirtualRow childSubRow = row.getSubRow(childPath);
				   if(childSubRow.getContent().size()==0||(childSubRow.isBlank()&& valueHolder.getValue(prop.getName())!= null)){ // all child field was blank and now new ValueHolder to keep prev. values
					   continue;
				   }
				   VirtualRow childItselfRow = childSubRow.getDirectChild(childPath);
				   List<Item> keysValue = childItselfRow.getKeySet(prop.getType());
//				   Node childElement=node.getChild(prop.getName());
				   ValueHolder childBean;
				   Type childType = prop.getType();
				   if(prop.isCollection()) { // Collection 
					   	   // TODO - may be use mapped DynaBean to link key with objects instead to find it?
						   int index = findByKey(prop.getName(),prop.getType(),valueHolder, keysValue);
						   if(index != (-1)){ // found
							   childBean = (ValueHolder)valueHolder.getValue(prop.getName(), index);
								decomposeByHierarchy(childSubRow, childBean, childPath, childType, prop.getName(), true); 
						   }
						   else {  // don't found
							   if(prop.getType().isComplex()){
								   childBean = deReferencedObject(childSubRow,childType);
								   if(childBean !=  null){
									   if(childBean instanceof CollectionBeanWrapper){ // Cooked object is already collections - so simply keep it
										   setValue(valueHolder, prop.getName(),(-1),childBean);
									   }
									   else { // Else add as element
											index = valueHolder.size(prop.getName());
										   setValue(valueHolder, prop.getName(),index,childBean);
											
									   }
									   
								   }
								   else{
									   try {
										index = valueHolder.size(prop.getName());
									} catch (Exception e) {
										// TODO Auto-generated catch block
										System.out.println("Wrong ValueHolder "+valueHolder+" for "+prop.getName());
										e.printStackTrace();
									}
									   // Check Subrow, if all non blank item is "collection"  we don't need to create new object - add items to previous collections
									   if(isAllNonBlankCollection(childType, childSubRow, childPath)){
										   // Get last parent
										   childBean = (ValueHolder)valueHolder.getValue(prop.getName(), index-1);
									   }
									   else{
										   // create new 
//										   	childBean = new BeanWrapper(new LazyDynaBean(),childType);
										   	childBean = new MapWrapper(childType);
									   		setValue(valueHolder, prop.getName(),index,childBean);
										   }
										decomposeByHierarchy(childSubRow, childBean,childPath, childType, prop.getName(), true); 
									   }
							   }
							   else { // Child is primitive type - so no need separate node  - use parent
									decomposeByHierarchy(childSubRow, valueHolder, childPath,childType,prop.getName(), true); 
							   }
						   }
					   }
				   else	{ // Scalar 
					   if(prop.getType().isComplex()){
						   childBean = deReferencedObject(childSubRow,childType);
						   if(childBean != null){
//							   	childBean = new BeanWrapper(childBean,childType);
								setValue(valueHolder, prop.getName(),(-1), childBean);
						   }
						   else{
							   // Check Subrow, if all non blank item is "collection"  we don't need to create new object - add items to previous collections
							   if(isAllNonBlankCollection(childType, childSubRow, childPath)){
								   // Get last parent
								   childBean = (ValueHolder)valueHolder.getValue(prop.getName());
							   }
							   else{
								   // create new
//								   	childBean = new BeanWrapper(new LazyDynaBean(),childType);
								   	childBean = new MapWrapper(childType);
									setValue(valueHolder, prop.getName(),(-1), childBean);
								   }
							   decomposeByHierarchy(childSubRow, childBean, childPath,childType,prop.getName(), false);
							   }
					   }
					   else { // Child is primitive type - so no need separate node  - use parent
							decomposeByHierarchy(childSubRow, valueHolder,childPath,childType, prop.getName(), false); 
					   }
				}   
		   }
	   }
	   else {// Property is atomic and row has only one column 
		   // TODO - if value is blank - need we inherit value from previous?
			   if(isCollection == false){  // Scalar
			   		setValue(valueHolder, propName,(-1), convertToJavaObject(row.content.get(0).getValue(),type));
			   }
			   else { // Collection
			   		setValue(valueHolder, propName,valueHolder.size(propName), convertToJavaObject(row.content.get(0).getValue(),type));
			   }
		   }   
}

private void setValue(ValueHolder valueHolder,String propName, int index, Object value){
		valueHolder.set(propName.toUpperCase(), index, value);
}   
private ValueHolder deReferencedObject(VirtualRow row, Type type2) {
	ValueEval value = row.content.get(0).getValue();
	if(value instanceof StringEval){
		String id = ((StringEval)value).getStringValue();
		if(DataPool.isOurUUID(id)){
			Object result = ThreadEvaluationContext.getDataPool().get(id);
			if (result instanceof ValueHolder){
				// Check has object right type(contains all expected properties)? 
				for(Property prop:type2.getChilds()){
					try {
						if(((ValueHolder)result).getValue(prop.getName())==null){
							return null;
						}
					} catch (Exception e) {
						return null;
					}
				}
				if(result instanceof ValueHolder){
					return (ValueHolder)result;
				}
				else{
					return new BeanWrapper(result,type2);
				}
			}
		}
	}
	return null;
}

public  Object convertToJavaObject(ValueEval poiValue, Type type2){
		if(poiValue instanceof NumberEval)
		{
			if(type2.equals(Primary.DATE.getType())){ // Date - convert numeric to Calendar
				Date date = DateUtil.getJavaDate(((NumberEval)poiValue).getNumberValue(), is1904BaseDate);
				Calendar calendar = new GregorianCalendar();
				calendar.setTime(date);
		        return calendar;
			}
			return new Double(((NumberEval)poiValue).getNumberValue());
		}
		else if(poiValue instanceof BoolEval)
		{
			return new Boolean(((BoolEval)poiValue).getBooleanValue());
		}
		else if(poiValue instanceof StringEval)
		{
			return ((StringEval)poiValue).getStringValue();
		}	
		else 
		{
			String msg = " Returned object contains primitive value "+poiValue+ " with illegal type ";
			LOG.error(msg);
			throw new RuntimeException(msg);
		}
   }
   
   public static  ValueEval convertToEval(Object value){
		if(value instanceof Double)
		{
			return new NumberEval(((Double)value).doubleValue()); 
		}
		else if(value instanceof Boolean)
		{
			return ((Boolean)value)?BoolEval.TRUE:BoolEval.FALSE; 
		}
		else if(value instanceof String)
		{
			return new StringEval((String)value);
		}	
		else 
		{
			String msg = " Returned object contains primitive value "+value+ " with illegal type ";
			LOG.error(msg);
			throw new RuntimeException(msg);
		}
  }
   
	public   int findByKey(String name,Type type,ValueHolder bean,List<Item> keysValue){
		int result = (-1);
		if(keysValue.size()>0
//				&& result >0 // This string is for DEBUG ONLY.EXCLUDE IT!
				){
			int count = bean.size(name);
			// Set previous values in empty keys
			for(Item item:keysValue){
				if(item.getValue() instanceof BlankEval){
					item.setValue(getLastValue(type, bean, item)); 
				}
			}
			boolean equal = false;	
			for(int i=0;i<count;i++){
				ValueHolder elem = (ValueHolder) bean.getValue(name,i);
				Type elemType = elem.getModel();
				equal = true;
				for(Item item:keysValue){
					if(elem.getValue(item.getPropName())==null || !elem.getValue(item.getPropName()).equals(convertToJavaObject(item.getValue(),elemType))){
					equal = false;	
					}
				}
				if(equal){
					result = i;
					break;
				}
				
			}
			return result;
		}
		else {  // Type has no key set - so we did not found
			
			return -1;			
						
		}
	}
	protected static  ValueEval getLastValue(Type type, ValueHolder bean,Item item){
		
		String rest = item.getTitle().substring(type.getPath().length()+1);
		if(rest.equals(item.getPropName())) { // last
			try {
				return convertToEval(bean.getValue(rest, bean.size(rest)-1));
			} catch (IllegalArgumentException e) {
				return convertToEval(bean.getValue(rest));
			}
		}
		else {
			int indexdot = rest.indexOf(".");
			if(indexdot >0){
				String next = rest.substring(0,rest.indexOf("."));
				for(Property  child:type.getChilds()){
					if(child.getType().getPath().equals(type.getPath()+"."+next)){
						ValueHolder newBean;
						try {
							newBean = (ValueHolder)bean.getValue(next, bean.size(next)-1);
						} catch (IllegalArgumentException e) {
							newBean = (ValueHolder)bean.getValue(next);
						}
						return getLastValue(child.getType(),newBean,item);
					}
						
				}
			}	
			String msg = "Did not find path to  "+item.getTitle()+ " from "+type.getPath();
			LOG.error(msg);
			throw new RuntimeException(msg);
		}
			
	}
	protected boolean isAllNonBlankCollection(Type type,VirtualRow typeRow, String pathTo){
		for(Item item:typeRow.getContent()){
			if(! (item.isWasBlank())){
				boolean found = false;
				// Extract prop name from title (may be intermediate, so do not use getPropName )
				String title = item.getTitle();
				String propName = title.substring(pathTo.length()+1);
				int index = propName.indexOf(".");
				if(index !=(-1)){
					propName = propName.substring(0,index); //VIA
				}
				for(Property prop:type.getChilds()){
					if(prop.getName().equalsIgnoreCase(propName)){
						if(!prop.isCollection()){
							return false;
						}
						found = true;
						break;
					}
				}
				if(!found){
					return false;
				}
			}
		}
		return true;
	} 
}
