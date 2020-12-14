package org.openl.gen.writers;

import org.objectweb.asm.ClassWriter;

/**
 * Abstract chained byte code writter
 *
 * @author Vladyslav Pikus
 */
public abstract class ChainedBeanByteCodeWriter implements BeanByteCodeWriter {

    private final BeanByteCodeWriter next;

    /**
     * Initialize chained byte code writter with given parameter
     * @param next link to the next writter
     */
    public ChainedBeanByteCodeWriter(ChainedBeanByteCodeWriter next) {
        this.next = next == null ? EmptyWritter.getInstance() : next;
    }

    /**
     * Write bytecode and call next writter
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
     * Singleton empty writter stub.
     *
     * @author Vladyslav Pikus
     */
    private static class EmptyWritter implements BeanByteCodeWriter {

        private static class Holder {
            private static final EmptyWritter INSTANCE = new EmptyWritter();
        }

        private EmptyWritter() {
        }

        /**
         * Just do nothing
         *
         * @param cw  target class writer
         */
        @Override
        public void write(ClassWriter cw) {
            //do nothing
        }

        static EmptyWritter getInstance() {
            return Holder.INSTANCE;
        }
    }
}
