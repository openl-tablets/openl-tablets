package org.openl.rules.lookup;

public interface ISingleLookupFactory {
    ISingleLookupModel makeModel(Object[] lookups, Object[] values);
}
