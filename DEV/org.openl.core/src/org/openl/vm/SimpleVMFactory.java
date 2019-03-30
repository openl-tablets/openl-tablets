package org.openl.vm;

public final class SimpleVMFactory {

    private SimpleVMFactory() {
    }

    public static SimpleVM buildSimpleVM() {
        return new SimpleVM();
    }
}
