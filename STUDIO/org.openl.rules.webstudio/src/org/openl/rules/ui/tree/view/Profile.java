package org.openl.rules.ui.tree.view;

public class Profile {
    private static final RulesTreeView typeView = new TypeView();
    private static final RulesTreeView excelSheetView = new ExcelSheetView();
    private static final RulesTreeView categoryView = new CategoryView();
    private static final RulesTreeView categoryDetailedView = new CategoryDetailedView();
    private static final RulesTreeView categoryInversedView = new CategoryInversedView();

    public static final RulesTreeView[] treeViews = { typeView,
            excelSheetView,
            categoryView,
            categoryDetailedView,
            categoryInversedView };

    public static final RulesProfile[] profiles = { new TypeProfile(),
            new ExcelSheetProfile(),
            new CategoryProfile(),
            new CategoryDetailedProfile(),
            new CategoryInversedProfile() };
}
