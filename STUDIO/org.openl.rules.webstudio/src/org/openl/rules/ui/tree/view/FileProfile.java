package org.openl.rules.ui.tree.view;

public class FileProfile implements RulesProfile {
    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getDisplayName() {
        return "File";
    }

    @Override
    public String getDescription() {
        return "Organize projects by physical location";
    }
}
