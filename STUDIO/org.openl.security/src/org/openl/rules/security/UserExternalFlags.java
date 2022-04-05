package org.openl.rules.security;

import io.swagger.v3.oas.annotations.media.Schema;

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

    @Schema(description = "Is first name pooled from external authentication system")
    public boolean isFirstNameExternal() {
        return checkFeature(Feature.EXTERNAL_FIRST_NAME);
    }

    @Schema(description = "Is last name pooled from external authentication system")
    public boolean isLastNameExternal() {
        return checkFeature(Feature.EXTERNAL_LAST_NAME);
    }

    @Schema(description = "Is e-mail pooled from external authentication system")
    public boolean isEmailExternal() {
        return checkFeature(Feature.EXTERNAL_EMAIL);
    }

    @Schema(description = "Is display name pooled from external authentication system")
    public boolean isDisplayNameExternal() {
        return checkFeature(Feature.EXTERNAL_DISPLAY_NAME);
    }

    @Schema(description = "Is e-mail verified")
    public boolean isEmailVerified() {
        return checkFeature(Feature.EMAIL_VERIFIED);
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
     * Initialize feature builder using custom feature flags
     * 
     * @param features feature flags
     * @return builder object
     */
    public static Builder builder(int features) {
        return new Builder(features);
    }

    public static Builder builder(UserExternalFlags externalFlags) {
        return new Builder(externalFlags.features);
    }

    /**
     * Represent amount of supported features
     */
    public enum Feature {
        EXTERNAL_FIRST_NAME(false, 0),
        EXTERNAL_LAST_NAME(false, 1),
        EXTERNAL_EMAIL(false, 2),
        EXTERNAL_DISPLAY_NAME(false, 3),
        EMAIL_VERIFIED(false, 4);

        private final int mask;
        private final boolean defaultState;

        /**
         *
         * @param defaultState default feature state
         * @param pos 0 <= bit number < 32.
         */
        Feature(boolean defaultState, int pos) {
            this.defaultState = defaultState;
            this.mask = (1 << pos);
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

        private Builder(int features) {
            this.features = features;
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

        public int getRawFeatures() {
            return features;
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
