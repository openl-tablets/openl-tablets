/**
 * Interactive, Java-debugger-style trace engine.
 *
 * <p>Runs a rule on a dedicated worker thread and suspends real execution at breakpoints and step
 * points. Because OpenL evaluation is a synchronous recursive call chain that funnels through
 * {@link org.openl.vm.Tracer}, a parked worker thread holds the whole computation as a live
 * continuation. The engine exposes the live execution stack (root to current point) instead of a
 * full trace tree, so memory is bounded by stack depth rather than total executed steps.
 */
@NullMarked
package org.openl.rules.webstudio.web.trace.debug;

import org.jspecify.annotations.NullMarked;
