package org.openl.rules.context.properties;

import org.openl.rules.context.IRulesContext;
import org.openl.rules.table.properties.ITableProperties;

public interface IMatcher {

	boolean isMatch(IRulesContext context, ITableProperties tableProperties);
}
