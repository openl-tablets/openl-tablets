/**
 * Created Feb 12, 2007
 */
package org.openl.rules.validator.dt;

/**
 * @author snshor
 *
 */

public interface IDomainAdaptor
{
	//int size();
	int getIndex(Object value);
	Object getValue(int index);
	int getMin();
	int getMax();
	
	int getIntVarDomainType();
	
}
