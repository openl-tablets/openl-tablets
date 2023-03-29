package org.openl.rules.ui.tree.view;

public class Profile {
    private static final RulesTreeView typeView = new TypeView();
    private static final RulesTreeView fileView = new FileView();
    private static final RulesTreeView categoryView = new CategoryView();
    private static final RulesTreeView categoryDetailedView = new CategoryDetailedView();
    private static final RulesTreeView categoryInversedView = new CategoryInversedView();

    public static final RulesTreeView[] treeViews = { typeView,
            fileView,
            categoryView,
            categoryDetailedView,
            categoryInversedView };

    public static final RulesProfile[] profiles = { new TypeProfile(),
            new FileProfile(),
            new CategoryProfile(),
            new CategoryDetailedProfile(),
            new CategoryInversedProfile() };
}
