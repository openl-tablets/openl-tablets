package org.openl.rules.ui.tree.view;

/** The ways a module's tables can be ordered, as they are offered to a user. */
public class Profile {

    private Profile() {
        // Utility class
    }

    public static final RulesProfile[] PROFILES = {new TypeProfile(),
            new ExcelSheetProfile(),
            new CategoryProfile(),
            new CategoryDetailedProfile(),
            new CategoryInversedProfile()};
}
