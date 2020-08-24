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

public final class LowerCaseSpreadsheetResultFieldNameResolver implements SpreadsheetResultFieldNameResolver {
    @Override
    public String resolveName(String name, String columnName, String rowName) {
        return name != null ? name.toLowerCase() : null;
    }
}
