/**
 * Created Apr 6, 2007
 */
package org.openl.domain;

import org.openl.util.ISelector;

/**
 * @author snshor
 */

public interface IDomain extends ISelector
{
	boolean isFinite();
	IType getElementType();
}
