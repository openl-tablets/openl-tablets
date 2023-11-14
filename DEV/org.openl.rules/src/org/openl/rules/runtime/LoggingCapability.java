package org.openl.rules.runtime;

import java.util.function.Function;

public interface LoggingCapability {

    boolean loggingEnabled();

    Function<Object, String> serializer();
}
