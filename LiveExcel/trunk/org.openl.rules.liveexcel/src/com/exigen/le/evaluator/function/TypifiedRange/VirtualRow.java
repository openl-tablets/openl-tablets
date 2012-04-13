/**
 * 
 */
package com.exigen.le.evaluator.function.TypifiedRange;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;

import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.Type;


/**
 * Row (or column) of table
 * @author vabramovs
 *
 */
public class VirtualRow {
	List<Item> content;
	String path;

	/**
	 * @return the content
	 */
	public List<Item> getContent() {
		return content;
	}

	/**
	 * @param content the content to set
	 */
	public void setContent(List<Item> content) {
		this.content = content;
	}
	/**
	 * Take away only elements for child and it's descendants 
	 * 
	 * @param childPath
	 * @return
	 */
	public VirtualRow getSubRow(String childPath){
		VirtualRow result = new VirtualRow();
		List<Item> newContent = new ArrayList<Item>();
		for(Item item:content){
			if(item.getTitle().startsWith(childPath)){
				newContent.add(item);
			}
		}
		result.setContent(newContent);
		return result;
	}
	/** Take away only elements for direct childs
	 * @param parentPath
	 * @return
	 */
	public VirtualRow getDirectChild(String parentPath){
		VirtualRow result = new VirtualRow();
		List<Item> newContent = new ArrayList<Item>();
		for(Item item:content){
			String title = item.getTitle();
			if(title.equals(parentPath)){
				return this;
			}
			else if(title.startsWith(parentPath)){
				if(!title.substring(parentPath.length()+1).contains(".")){
					newContent.add(item);
				}
			}
		}
		result.setContent(newContent);
		return result;
	}

	/**
	 *  Get key  element's set for type
	 * @param type
	 * @return
	 */
	public List<Item> getKeySet(Type type){
		List<Item> result = new ArrayList<Item>();
		for(String key:type.getKeyList()){
			for(Item item:content){
				String upKey = key;
				String title = item.getTitle();
				if(title.endsWith(upKey)){
					result.add(item);
				}
			}
		}
			
		return result;
	}
	/**
	 *  Is row blank (or was blank before substitution)
	 * @return
	 */
	public boolean isBlank() {
		for(Item item:content){
			if(! (item.getValue() instanceof BlankEval || item.wasBlank )){
				if(item.getValue() instanceof StringEval){
					if(((StringEval)item.getValue()).getStringValue().trim().length()==0){
							continue;
					}
				}
				return false;
			}
		}
		return true;
	}
}
