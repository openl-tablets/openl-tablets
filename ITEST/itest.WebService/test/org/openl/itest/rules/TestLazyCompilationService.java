package org.openl.itest.rules;

import org.openl.rules.context.IRulesRuntimeContext;

public interface TestLazyCompilationService {

    String throwCompilationError(IRulesRuntimeContext context);

}
