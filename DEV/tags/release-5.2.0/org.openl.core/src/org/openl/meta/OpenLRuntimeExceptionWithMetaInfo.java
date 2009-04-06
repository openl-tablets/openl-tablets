/**
 * Created Apr 16, 2007
 */
package org.openl.meta;

/**
 * @author snshor
 *
 */
public class OpenLRuntimeExceptionWithMetaInfo extends RuntimeException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4838530560261666278L;
	IMetaHolder[] holders;
	
	public OpenLRuntimeExceptionWithMetaInfo(String reason, IMetaHolder[] holders)
	{
		super(reason);
		this.holders = holders;
	}

	public IMetaHolder[] getHolders()
	{
		return this.holders;
	}
	
	public String[] optionalDescriptions()
	{
		return null;
	}
	
}
