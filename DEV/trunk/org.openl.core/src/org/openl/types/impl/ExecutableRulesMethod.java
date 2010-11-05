package org.openl.types.impl;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;

/**
 * Default implementation for all executable OpenL table methods.
 * 
 * @author DLiauchuk
 *
 */
public abstract class ExecutableRulesMethod extends AMethod implements IMemberMetaInfo {

	public ExecutableRulesMethod(IOpenMethodHeader header) {
		super(header);		
	}	
	
	@Override
	public String toString() {	
		return getName();
	}
}
