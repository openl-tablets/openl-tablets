package org.openl.rules.calc;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.OpenL;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.types.IOpenField;

// The containing spreadsheet describes field lookup; it does not change OpenL type identity.
@SuppressWarnings("java:S2160")
public class SpreadsheetOpenClass extends ComponentOpenClass {

    private final @Nullable SpreadsheetOpenClass spreadsheet;
    /** Returns the name of the spreadsheet containing this context. */
    @Getter
    private final String spreadsheetName;
    /** Returns the source location of the containing spreadsheet, when available. */
    @Getter
    private final @Nullable String spreadsheetSourceUrl;

    SpreadsheetOpenClass(String name, OpenL openl, String spreadsheetName, @Nullable String spreadsheetSourceUrl) {
        super(name, openl);
        this.spreadsheet = null;
        this.spreadsheetName = spreadsheetName;
        this.spreadsheetSourceUrl = spreadsheetSourceUrl;
    }

    SpreadsheetOpenClass(String name, OpenL openl, SpreadsheetOpenClass spreadsheet) {
        super(name, openl);
        this.spreadsheet = spreadsheet;
        this.spreadsheetName = spreadsheet.spreadsheetName;
        this.spreadsheetSourceUrl = spreadsheet.spreadsheetSourceUrl;
    }

    /** Resolves cell references absent from a row or column context through their spreadsheet. */
    @Override
    public IOpenField getField(String name, boolean strictMatch) {
        var field = super.getField(name, strictMatch);
        if (field == null && spreadsheet != null && name.startsWith("$")) {
            return spreadsheet.getField(name, strictMatch);
        }
        return field;
    }

    @Override
    public Class<?> getInstanceClass() {
        return SpreadsheetResult.class;
    }
}
