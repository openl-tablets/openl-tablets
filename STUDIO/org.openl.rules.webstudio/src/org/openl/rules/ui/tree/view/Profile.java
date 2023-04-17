package org.openl.rules.ui.tree.view;

public class Profile {
    private static final RulesTreeView TYPE_VIEW = new TypeView();
    private static final RulesTreeView EXCEL_SHEET_VIEW = new ExcelSheetView();
    private static final RulesTreeView CATEGORY_VIEW = new CategoryView();
    private static final RulesTreeView CATEGORY_DETAILED_VIEW = new CategoryDetailedView();
    private static final RulesTreeView CATEGORY_INVERSE_VIEW = new CategoryInversedView();

    public static final RulesTreeView[] TREE_VIEWS = {TYPE_VIEW,
            EXCEL_SHEET_VIEW,
            CATEGORY_VIEW,
            CATEGORY_DETAILED_VIEW,
            CATEGORY_INVERSE_VIEW};

    public static final RulesProfile[] PROFILES = { new TypeProfile(),
            new ExcelSheetProfile(),
            new CategoryProfile(),
            new CategoryDetailedProfile(),
            new CategoryInversedProfile() };
}
