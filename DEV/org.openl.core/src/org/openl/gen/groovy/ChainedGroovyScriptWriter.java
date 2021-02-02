package org.openl.gen.groovy;

public abstract class ChainedGroovyScriptWriter implements GroovyWriter {

    private final GroovyWriter next;

    public ChainedGroovyScriptWriter(GroovyWriter next) {
        this.next = next == null ? EmptyWriter.getInstance() : next;
    }

    @Override
    public void write(StringBuilder s, boolean isAbstract) {
        writeInternal(s, isAbstract);
        next.write(s, isAbstract);
    }

    protected abstract void writeInternal(StringBuilder s, boolean isAbstract);

    private static class EmptyWriter implements GroovyWriter {

        private static class Holder {
            private static final EmptyWriter INSTANCE = new EmptyWriter();
        }

        private EmptyWriter() {
        }

        @Override
        public void write(StringBuilder sb, boolean isAbstract) {
            // do nothing
        }

        static EmptyWriter getInstance() {
            return EmptyWriter.Holder.INSTANCE;
        }
    }
}
