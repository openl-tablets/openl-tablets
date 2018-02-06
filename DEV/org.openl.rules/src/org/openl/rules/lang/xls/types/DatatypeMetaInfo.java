package org.openl.rules.lang.xls.types;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.openl.meta.IMetaInfo;

/**
 * Implementation of {@link IMetaInfo} for datatypes. 
 * First of all to handle the url to source.
 * Display name is implemented to return the same name for all modes. Should be updated if needed.
 * 
 * @author DLiauchuk
 * TODO: Replace with org.openl.meta.TableMetaInfo
 */
public class DatatypeMetaInfo implements IMetaInfo {
	
	private String displayName;
	private String sourceUrl;
	
	public DatatypeMetaInfo(String displayName, String sourceUrl) {
		this.displayName = displayName;
		this.sourceUrl = sourceUrl;
	}
		
	public String getDisplayName(int mode) {
		/* Default implementation.
		 *  Don`t know if we need any displayName for Datatype.
		 *  @author DLiauchuk
		 */
		return displayName;
	}
	
	public String getDisplayName() {
		return getDisplayName(0);
	}
	
	public String getSourceUrl() {
		return sourceUrl;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(displayName).append(sourceUrl).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DatatypeMetaInfo other = (DatatypeMetaInfo) obj;
		
		return new EqualsBuilder().append(displayName, other.getDisplayName()).append(sourceUrl, other.getSourceUrl()).isEquals();
	}
	
	

}
