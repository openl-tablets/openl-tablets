/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

/**
 * @author snshor
 *
 */
public interface IMethodSignature
{

	public static final IMethodSignature VOID = new VoidSignature();

	public IOpenClass[] getParameterTypes();

	public String getParameterName(int i);

	public int getParameterDirection(int i);
	public int getNumberOfArguments();

	static final class VoidSignature implements IMethodSignature
	{
		public int getParameterDirection(int i)
		{
			throw new IndexOutOfBoundsException();
		}

		public String getParameterName(int i)
		{
			throw new IndexOutOfBoundsException();
		}

		public IOpenClass[] getParameterTypes()
		{
			return IOpenClass.EMPTY;
		}

		public int getNumberOfArguments()
		{
			return 0;
		}

	}

}
