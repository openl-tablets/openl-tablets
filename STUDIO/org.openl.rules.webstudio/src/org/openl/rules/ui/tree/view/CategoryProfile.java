package org.openl.rules.ui.tree.view;

public class CategoryProfile implements RulesProfile {
    @Override
    public String getName() {
        return "category";
    }

    @Override
    public String getDisplayName() {
        return "By Category";
    }

    @Override
    public String getDescription() {
        return "Provides categorized view";
    }

}
