package org.openl.rules.webstudio.web.trace.debug;

/**
 * The rule execution a debug session drives on its worker thread.
 *
 * <p>Implementations run the selected test case or method invocation. They run with the debug hook
 * installed on the current thread, so every traced invocation is bracketed by the engine.
 */
@FunctionalInterface
public interface DebugBody {

    void execute() throws Exception;
}
