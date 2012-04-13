/**
 * 
 */
package com.exigen.le.smodel;

import java.util.LinkedList;
import java.util.List;

/**
 * Table Descriptor
 * @author vabramovs
 *
 */
public class TableDesc {
	TableDesc(){ // For JAXB
	}
	
	String name;
	// I assume that basic params go first
	List<ColumnDesc> paramDescs = new LinkedList<ColumnDesc>();
	ColumnDesc valueDesc;
	// initial data for table
	String fileName;  // 
	//default value - if nothing found - this value is returned
	String defaultValue = null;
	
	
	public void setDefaultValue(String s){
		defaultValue = s;
	}
	
	public String getDefaultValue(){
		return defaultValue;
	}
	
    
    /**
     * @return the declared name
     */
    public String getDeclaredName() {
        return name;
    }
    
    
    /**
     * @return the name in upper case
     */
    public String getName() {
        return name.toUpperCase();
    }
    
	/*
	 * @return column description for the parameters.  
	 */
	public List<ColumnDesc> getParamDescs(){
		return paramDescs;
	}
	
	/*
	 * @return column description for the value.  
	 */
	public ColumnDesc getValueDesc(){
		return valueDesc;
	}
	
	public TableDesc(String name, List<ColumnDesc> params, ColumnDesc value){
		this.name = name;
		this.paramDescs = params;
		this.valueDesc = value;
	}
	
	/**
	 * @param name Table name
	 * @param value Type of return values
	 * @param params Types of params (param assumed non-intervals)
	 */
	public TableDesc(String name, DataType value, DataType...params){
		this.name = name;
		this.valueDesc = new ColumnDesc(value);
		if (params.length == 0){
			throw new RuntimeException("No params provided");
		}
		for( DataType dt: params){
			paramDescs.add(new ColumnDesc(dt));
		}
	}
	
	/**
	 * @param name Table name
	 * @param defValue default value
	 * @param value Type of return values
	 * @param params Types of params (param assumed non-intervals)
	 */
	public TableDesc(String name, String defValue, DataType value, DataType...params){
		this(name, value, params);
		this.defaultValue = defValue;
	}
	
	
	/*
	 * @return number of basic (non additional params)
	 */
	public int getBasicParametersCount(){
		int i = 0;
		for (;i<paramDescs.size(); i++){
			if (paramDescs.get(i).isAdditional()){
				break;
			}
		}
		return i;
	}
	
	public List<String> getAdditionalParameters(){
		List<String> result = new LinkedList<String>();
		for (ColumnDesc cd: paramDescs){
			if (cd.isAdditional){
				result.add(cd.additionalName);
			}	
		}
		return result;
	}
	

	public static enum DataType {
		BOOLEAN(1, "BOOLEAN"),
		DOUBLE(2, "DOUBLE"),
		STRING(3, "STRING"),
		DATE(4, "DATE");
		
		private int id;
		private String name;
		
		DataType(int id, String name){
			this.id = id;
			this.name = name;
		}
		int getId(){
			return id;
		}
		
		@Override 
		public String toString(){
			return name;
		}
		
	}
	
	
	
	
	public static class ColumnDesc {
		// could be interval - not applicable for values;
		boolean isInterval =  false;
		// data type - default - string
		DataType type = DataType.STRING;
		// applicable for datatype string
		int maxLength =50;
		// applicable for datatype string, when concrete cell data (and table parameter ) may be double
		int fractalLen =0;
		
		/**
		 * @return the fractalLen
		 */
		public int getFractalLen() {
			return fractalLen;
		}

		/**
		 * @param fractalLen the fractalLen to set
		 */
		public void setFractalLen(int fractalLen) {
			this.fractalLen = fractalLen;
		}

		// true if this is "context parameter" - effective date etc.
		boolean isAdditional=false; 
		
		// valid for additional only - name for additional param in excel 
		String additionalName;
		
		// if left end of interval includes
		boolean isLeftIncluded=true;
		boolean isRightIncluded=true;
		
		public ColumnDesc(){ // For JAXB
			this.type = DataType.STRING;
		}
		
		public ColumnDesc(DataType type){
			this.type = type;
		}
		
		public ColumnDesc(DataType type, boolean isInterval){
			this.type = type;
			this.isInterval = isInterval;
		}
		
		public ColumnDesc(int maxLength){
			type = DataType.STRING;
			this.maxLength = maxLength; 
		}
		
		public ColumnDesc(int maxLength, boolean isInterval){
			type = DataType.STRING;
			this.maxLength = maxLength; 
			this.isInterval = isInterval;
		}

		/**
		 * @return the isInterval
		 */
		public boolean isInterval() {
			return isInterval;
		}

		/**
		 * @return the type
		 */
		public DataType getType() {
			return type;
		}

		/**
		 * @return the maxLength
		 */
		public int getMaxLength() {
			return maxLength;
		}

		/**
		 * @return the isAdditional
		 */
		public boolean isAdditional() {
			return isAdditional;
		}

		/**
		 * @return the additionalName
		 */
		public String getAdditionalName() {
			return additionalName;
		}

		/**
		 * @param additionalName the additionalName to set
		 */
		public void setAdditionalName(String additionalName) {
			this.additionalName = additionalName;
			isAdditional = true;
		}

		/**
		 * @param isInterval the isInterval to set
		 */
		public void setInterval(boolean isInterval) {
			this.isInterval = isInterval;
		}

		/**
		 * @param type the type to set
		 */
		public void setType(DataType type) {
			this.type = type;
		}

		/**
		 * @param maxLength the maxLength to set
		 */
		public void setMaxLength(int maxLength) {
			this.maxLength = maxLength;
		}

		/**
		 * @param isAdditional the isAdditional to set
		 */
		public void setAdditional(boolean isAdditional) {
			this.isAdditional = isAdditional;
		}

		/**
		 * @return the isLeftIncluded
		 */
		public boolean isLeftIncluded() {
			return isLeftIncluded;
		}

		/**
		 * @param isLeftIncluded the isLeftIncluded to set
		 */
		public void setLeftIncluded(boolean isLeftIncluded) {
			this.isLeftIncluded = isLeftIncluded;
		}

		/**
		 * @return the isRightIncluded
		 */
		public boolean isRightIncluded() {
			return isRightIncluded;
		}

		/**
		 * @param isRightIncluded the isRightIncluded to set
		 */
		public void setRightIncluded(boolean isRightIncluded) {
			this.isRightIncluded = isRightIncluded;
		}
		
		@Override 
		public String toString(){
			return  "Data type: " + type.toString() +   " Interval: "+ isInterval + 
							" MaxLength: " + maxLength + " Additional" + isAdditional +
							" AdditionalName: " + additionalName + 
							" LeftIncluded: " + isLeftIncluded + " RightIncluded: " + isRightIncluded;
		}
		
	}




	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param paramDescs the paramDescs to set
	 */
	public void setParamDescs(List<ColumnDesc> paramDescs) {
		this.paramDescs = paramDescs;
	}

	/**
	 * @param valueDesc the valueDesc to set
	 */
	public void setValueDesc(ColumnDesc valueDesc) {
		this.valueDesc = valueDesc;
	}




	/**
	 * @return the fileName
	 */
	public String obtainFileName() {
		return fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	@Override 
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("--Table: ").append(name).append("\n").append("----Parameters:\n");
		for (ColumnDesc cd : paramDescs){
			sb.append(cd.toString()).append("\n");
		}
		sb.append("----Value:\n").append(valueDesc.toString()).append("\n");
		return sb.toString();
	}
	
}
