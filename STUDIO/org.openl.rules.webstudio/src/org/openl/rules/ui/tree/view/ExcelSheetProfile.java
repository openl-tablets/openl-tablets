package org.openl.rules.ui.tree.view;

public class ExcelSheetProfile implements RulesProfile {
    @Override
    public String getName() {
        return "excelSheet";
    }

    @Override
    public String getDisplayName() {
        return "Excel Sheet";
    }

    @Override
    public String getDescription() {
        return "Organize projects by physical file structure";
    }
}
