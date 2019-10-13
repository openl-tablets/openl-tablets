package org.openl.rules.calculation.result.convertor2;

/*-
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2017 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public final class WhiteListRowFilter implements RowFilter {

    private Set<String> whiteList;

    private WhiteListRowFilter(Set<String> whiteList) {
        this.whiteList = whiteList;
    }

    @Override
    public boolean excludeRow(String rowName) {
        return !whiteList.contains(rowName);
    }

    public static WhiteListRowFilter buildWhiteListRowFilter(Set<String> whiteList) {
        Objects.requireNonNull(whiteList, "whiteList argument can't be null.");
        return new WhiteListRowFilter(Collections.unmodifiableSet(whiteList));
    }

}
