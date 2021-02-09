package org.openl.gen.writers;

import org.objectweb.asm.ClassWriter;

/**
 * Abstract chained byte code writer
 *
 * @author Vladyslav Pikus
 */
public abstract class ChainedBeanByteCodeWriter implements BeanByteCodeWriter {

    private final BeanByteCodeWriter next;

    /**
     * Initialize chained byte code writer with given parameter
     * 
     * @param next link to the next writer
     */
    public ChainedBeanByteCodeWriter(ChainedBeanByteCodeWriter next) {
        this.next = next == null ? EmptyWriter.getInstance() : next;
    }

    /**
     * Write bytecode and call next writer
     *
     * @param cw target class writer
     */
    @Override
    public final void write(ClassWriter cw) {
        writeInternal(cw);
        next.write(cw);
    }

    protected abstract void writeInternal(ClassWriter cw);

    /**
     * Singleton empty writer stub.
     *
     * @author Vladyslav Pikus
     */
    private static class EmptyWriter implements BeanByteCodeWriter {

        private static class Holder {
            private static final EmptyWriter INSTANCE = new EmptyWriter();
        }

        private EmptyWriter() {
        }

        /**
         * Just do nothing
         *
         * @param cw target class writer
         */
        @Override
        public void write(ClassWriter cw) {
            // do nothing
        }

        static EmptyWriter getInstance() {
            return Holder.INSTANCE;
        }
    }
}
