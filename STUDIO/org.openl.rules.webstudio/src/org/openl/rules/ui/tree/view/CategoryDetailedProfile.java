package org.openl.rules.ui.tree.view;

public class CategoryDetailedProfile implements RulesProfile {
    @Override
    public String getName() {
        return "categoryDetailed";
    }

    @Override
    public String getDisplayName() {
        return "By Category Detailed";
    }

    @Override
    public String getDescription() {
        return "Provides more detailed categorized view";
    }
}
