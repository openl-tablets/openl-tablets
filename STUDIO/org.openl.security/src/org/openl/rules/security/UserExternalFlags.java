package org.openl.rules.security;

public class UserExternalFlags {

    private final int features;

    private UserExternalFlags(int features) {
        this.features = features;
    }

    public boolean isFirstNameExternal() {
        return checkFeature(Feature.EXTERNAL_FIRST_NAME);
    }

    public boolean isLastNameExternal() {
        return checkFeature(Feature.EXTERNAL_LAST_NAME);
    }

    public boolean isEmailExternal() {
        return checkFeature(Feature.EXTERNAL_EMAIL);
    }

    public boolean isDisplayNameExternal() {
        return checkFeature(Feature.EXTERNAL_DISPLAY_NAME);
    }

    public boolean isSyncExternalGroups() {
        return checkFeature(Feature.SYNC_EXTERNAL_GROUPS);
    }

    boolean checkFeature(Feature f) {
        return f.enabled(features);
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum Feature {
        EXTERNAL_FIRST_NAME(false),
        EXTERNAL_LAST_NAME(false),
        EXTERNAL_EMAIL(false),
        EXTERNAL_DISPLAY_NAME(false),
        SYNC_EXTERNAL_GROUPS(false);

        private final int mask;
        private final boolean defaultState;

        Feature(boolean defaultState) {
            this.defaultState = defaultState;
            this.mask = (1 << ordinal());
        }

        public int getMask() {
            return mask;
        }

        public boolean getDefaultState() {
            return defaultState;
        }

        public boolean enabled(int flags) {
            return (flags & mask) != 0;
        }
    }

    public static class Builder {

        private int features;

        private Builder() {
            for (Feature f : Feature.values()) {
                applyFeature(f, f.getDefaultState());
            }
        }

        public Builder applyFeature(Feature f, boolean state) {
            if (state) {
                return withFeature(f);
            } else {
                return withoutFeature(f);
            }
        }

        public Builder withFeature(Feature f) {
            features |= f.getMask();
            return this;
        }

        public Builder withoutFeature(Feature f) {
            features &= ~f.getMask();
            return this;
        }

        public UserExternalFlags build() {
            return new UserExternalFlags(features);
        }
    }
}
