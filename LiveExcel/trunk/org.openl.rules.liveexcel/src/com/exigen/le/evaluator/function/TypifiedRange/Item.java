/**
 * 
 */
package com.exigen.le.evaluator.function.TypifiedRange;

import org.apache.poi.hssf.record.formula.eval.ValueEval;

/**
 * @author vabramovs
 *
 */
public class Item {
	String title;
	ValueEval value;
	protected boolean wasBlank;
	/**
	 * @param title
	 * @param value
	 */
	public Item(String rootName,String title, ValueEval value) {
		String parentName = rootName.toUpperCase();
		title = title.toUpperCase();
		if(!title.startsWith(parentName)){
			title = parentName+"."+title;
		}
		this.title = title.replaceAll(" ", "_");
		this.value = value;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @return name of property (last segment in title)
	 */
	public String getPropName(){
		int i=title.lastIndexOf(".");
		if(i!=(-1))
			return title.substring(i+1);
		else
			return title;
	}
	/**
	 * @return the value
	 */
	public ValueEval getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(ValueEval value) {
		this.value = value;
	}
	/**
	 * @return the wasBlank
	 */
	public boolean isWasBlank() {
		return wasBlank;
	}
	/**
	 * @param wasBlank the wasBlank to set
	 */
	public void setWasBlank(boolean wasBlank) {
		this.wasBlank = wasBlank;
	}

}
