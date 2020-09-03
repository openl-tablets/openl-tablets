package org.openl.rules.serialization;

/*-
 * #%L
 * OpenL - STUDIO - Jackson
 * %%
 * Copyright (C) 2016 - 2020 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import org.openl.util.StringUtils;

public class UncapitalizeSpreadsheetResultFieldNameResolver implements SpreadsheetResultFieldNameResolver {
    @Override
    public String resolveName(String name, String columnName, String rowName) {
        return name != null ? StringUtils.uncapitalize(name) : null;
    }
}
