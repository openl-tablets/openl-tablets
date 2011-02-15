/**
 * 
 */
package com.exigen.le.repository;

import java.util.Date;
import java.util.Map;


/**
 * @author vabramovs
 *
 */
public class RepositoryNodeDesc implements Comparable<RepositoryNodeDesc>{
	String name;
	String version;
	String author;
	String comment;
	Map<String, Object> properties;
	Date date;
	Date activationDate;
	
	/**
	 * @param version
	 * @param comment
	 * @param tags
	 * @param date
	 */
	public RepositoryNodeDesc(String name,String version,String author, String comment, Map<String, Object> properties, Date date) {
		this.name = name;
		this.version = version;
		this.comment = comment;
		this.author  = author;
		this.properties = properties;
		this.date = date;
		this.activationDate = null;
	}

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

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the tags
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}

	/**
	 * @param tags the tags to set
	 */
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
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
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	public int compareTo(RepositoryNodeDesc o) {
		if(name.compareTo(o.getName())!= 0) // Compare by name
				return name.compareTo(o.getName());
		else{ // Compare by version
			int len = Math.max(name.length(), o.getName().length());
			String our = "             ".substring(0,len-name.length())+name;
			String other = "             ".substring(0,len-o.getName().length())+o.getName();
			return our.compareTo(other);	
			
		}   
	}
	public boolean equals(RepositoryNodeDesc o){
		return  name.equals(o.getName())&&  version.equals(o.getVersion());
	}
	public int hashCode(){
		return name.hashCode()+version.hashCode();
	}
	/**
	 * @return the activationDate
	 */
	public Date getActivationDate() {
		return activationDate;
	}

	/**
	 * @param activationDate the activationDate to set
	 */
	public void setActivationDate(Date activationDate) {
		this.activationDate = activationDate;
	}
}
