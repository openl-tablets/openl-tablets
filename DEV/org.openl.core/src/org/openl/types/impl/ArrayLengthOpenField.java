/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.impl;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public abstract class ArrayLengthOpenField implements IOpenField
{
			public String getDisplayName(int mode)
	{
		return getName();
	}

		/* (non-Javadoc)
		 * @see org.openl.types.IOpenField#get(java.lang.Object)
		 */
		public Object get(Object target, IRuntimeEnv env)
		{
			return new Integer(getLength(target));
		}
		
		public abstract int getLength(Object target);
		/* (non-Javadoc)
		 * @see org.openl.types.IOpenField#isConst()
		 */
		public boolean isConst()
		{
			return true;
		}

		/* (non-Javadoc)
		 * @see org.openl.types.IOpenField#isReadable()
		 */
		public boolean isReadable()
		{
			return true;
		}

		public boolean isWritable()
		{
			return false;
		}

		public void set(Object target, Object value, IRuntimeEnv env)
		{
			throw new UnsupportedOperationException();
		}

		/* (non-Javadoc)
		 * @see org.openl.types.IOpenMember#getDeclaringClass()
		 */
		public IOpenClass getDeclaringClass()
		{
			return NullOpenClass.the;
		}

		public IMemberMetaInfo getInfo()
		{
			return null;
		}

		/* (non-Javadoc)
		 * @see org.openl.types.IOpenMember#getType()
		 */
		public IOpenClass getType()
		{
			return JavaOpenClass.INT;
		}

		/* (non-Javadoc)
		 * @see org.openl.types.IOpenMember#isStatic()
		 */
		public boolean isStatic()
		{
			return false;
		}

		/* (non-Javadoc)
		 * @see org.openl.base.INamedThing#getName()
		 */
		public String getName()
		{
			return "length";
		}

}
