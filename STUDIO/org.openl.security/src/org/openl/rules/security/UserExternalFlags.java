package org.openl.rules.security;

/**
 * This class handles flags of user external feature. Here can be added as many flags as they can fit in {@code int}.
 * Spoiler: 32 features can fit in {@code int} types.
 *
 * @author Vladyslav Pikus
 */
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

    /**
     * Initialize feature builder with default states
     *
     * @return builder object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Represent amount of supported features
     */
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

        /**
         * Return mask for given feature. The value is always 2 power N. Where 0 <= N < 32
         * 
         * @return feature mask
         */
        public int getMask() {
            return mask;
        }

        /**
         * Default feature state. {@code true} if enabled
         * 
         * @return {@code true} or {@code false}
         */
        public boolean getDefaultState() {
            return defaultState;
        }

        /**
         * Check if current feature is enabled in flags or not.
         * 
         * @param flags flags of features
         * @return {@code true} if enabled otherwise {@code false}
         */
        public boolean enabled(int flags) {
            return (flags & mask) != 0;
        }
    }

    /**
     * Builder for user external flags
     *
     * @author Vladyslav Pikus
     */
    public static class Builder {

        private int features;

        private Builder() {
            for (Feature f : Feature.values()) {
                applyFeature(f, f.getDefaultState());
            }
        }

        /**
         * Apply feature state
         *
         * @param f feature to apply
         * @param state {@code true} if enabled otherwise {@code false}
         * @return the same builder instance
         */
        public Builder applyFeature(Feature f, boolean state) {
            if (state) {
                return withFeature(f);
            } else {
                return withoutFeature(f);
            }
        }

        /**
         * Enable feature
         *
         * @param f feature to enadle
         * @return the same builder instance
         */
        public Builder withFeature(Feature f) {
            features |= f.getMask();
            return this;
        }

        /**
         * Disable feature
         *
         * @param f feature to disable
         * @return the same builder instance
         */
        public Builder withoutFeature(Feature f) {
            features &= ~f.getMask();
            return this;
        }

        /**
         * Make new instance of {@link UserExternalFlags}
         * 
         * @return new instance of {@link UserExternalFlags}
         */
        public UserExternalFlags build() {
            return new UserExternalFlags(features);
        }
    }
}
