package org.openl.rules.ui.tree.view;

public class TypeProfile implements RulesProfile {
    @Override
    public String getName() {
        return "type";
    }

    @Override
    public String getDisplayName() {
        return "By Type";
    }

    @Override
    public String getDescription() {
        return "Organize projects by component type";
    }
}
