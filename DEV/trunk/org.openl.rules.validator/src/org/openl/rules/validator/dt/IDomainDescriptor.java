/**
 * Created Feb 12, 2007
 */
package org.openl.rules.validator.dt;

/**
 * @author snshor
 *
 */

public interface IDomainDescriptor
{
	int size();
	int getIndex(Object value);
	Object getValue(int index);
	int getMin();
	int getMax();
	
}
