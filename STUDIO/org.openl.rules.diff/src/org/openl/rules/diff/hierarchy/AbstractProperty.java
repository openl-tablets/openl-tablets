package org.openl.rules.diff.hierarchy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AbstractProperty implements ProjectionProperty {
    private final String name;
    private final Object rawValue;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getRawValue() {
        return rawValue;
    }
}
