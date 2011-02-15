/**
 * 
 */
package com.exigen.le.project;

import java.util.Comparator;
import java.util.Date;

/**
 * Version Description
 * @author vabramovs
 *
 */
public class VersionDesc {
	private String revisionExcel;
	private String revisionXML;
	private String version;
	private Date date;

	/**
	 * @param revision
	 */
	public VersionDesc(String version,Date date) {
		this.version = version;
		this.date = date;
		this.revisionExcel="";
		this.revisionXML="";
	}
	public VersionDesc(String version) {
		this(version,new Date());
	}
	
	public VersionDesc(){
		this("");
	}

	/**
	 * @return the revision of Excel file
	 */
	public String getRevisionExcel() {
		return revisionExcel;
	}
	
	/**
	 * Set revision of Excel file
	 * @param revision
	 */
	public void setRevisionExcel(String revision){
		this.revisionExcel = revision;
	}
	
	/**
	 * @return the revision of XML file
	 */
	public String getRevisionXML() {
		return revisionXML;
	}
	
	/**
	 * Set revision of XML file
	 * @param revision
	 */
	public void setRevisionXML(String revision){
		this.revisionXML = revision;
	}
	public boolean equals(Object obj){
		if(obj instanceof VersionDesc)
			return version.equals(((VersionDesc)obj).getVersion());
		else 
			return false;
	}
	
	public int hashCode(){
		return version.hashCode();
	}
	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}
	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}
	/**
	 *  Comparator - newest first 
	 */
   static public final Comparator<VersionDesc> NEWEST_DATE_ORDER =
	        new Comparator<VersionDesc>() {
	    		public int compare(VersionDesc v1, VersionDesc v2) {
	    			return v2.getDate().compareTo(v1.getDate());
	    		}
	    };

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

}
