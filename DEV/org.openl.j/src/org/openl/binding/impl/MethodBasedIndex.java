package org.openl.binding.impl;

import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;

public class MethodBasedIndex implements IOpenIndex {

    private IMethodCaller reader;
    private IMethodCaller writer;

    public MethodBasedIndex(IMethodCaller reader, IMethodCaller writer) {
        this.reader = reader;
        this.writer = writer;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenIndex#getElementType()
     */
    @Override
    public IOpenClass getElementType() {
        return reader.getMethod().getType();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenIndex#getIndexType()
     */
    @Override
    public IOpenClass getIndexType() {
        int n = writer.getMethod().getSignature().getParameterTypes().length;

        return reader.getMethod().getSignature().getParameterTypes()[n - 1];
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenIndex#getValue(java.lang.Object, java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getValue(Object container, Object index) {

        int n = reader.getMethod().getSignature().getParameterTypes().length;

        if (n == 2) {
            return reader.invoke(null, new Object[] { container, index }, null);
        } else {
            return reader.invoke(container, new Object[] { index }, null);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenIndex#isWritable()
     */
    @Override
    public boolean isWritable() {
        return writer != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValue(Object container, Object index, Object value) {

        int n = writer.getMethod().getSignature().getParameterTypes().length;

        if (n == 3) {
            writer.invoke(null, new Object[] { container, index, value }, null);
        } else {
            writer.invoke(container, new Object[] { index, value }, null);
        }
    }
}
