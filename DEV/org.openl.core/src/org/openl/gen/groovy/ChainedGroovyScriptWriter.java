package org.openl.gen.groovy;

import java.util.Set;

public abstract class ChainedGroovyScriptWriter implements GroovyWriter {

    private final GroovyWriter next;

    public ChainedGroovyScriptWriter(GroovyWriter next) {
        this.next = next == null ? EmptyWriter.getInstance() : next;
    }

    @Override
    public void write(StringBuilder s, boolean isAbstract, Set<String> imports) {
        writeInternal(s, isAbstract, imports);
        next.write(s, isAbstract, imports);
    }

    protected abstract void writeInternal(StringBuilder s, boolean isAbstract, Set<String> imports);

    private static class EmptyWriter implements GroovyWriter {

        private static class Holder {
            private static final EmptyWriter INSTANCE = new EmptyWriter();
        }

        private EmptyWriter() {
        }

        @Override
        public void write(StringBuilder sb, boolean isAbstract, Set<String> imports) {
            // do nothing
        }

        static EmptyWriter getInstance() {
            return EmptyWriter.Holder.INSTANCE;
        }
    }
}
