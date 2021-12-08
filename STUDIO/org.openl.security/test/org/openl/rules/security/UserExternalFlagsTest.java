package org.openl.rules.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.junit.Test;
import org.openl.rules.security.UserExternalFlags.Builder;
import org.openl.rules.security.UserExternalFlags.Feature;

public class UserExternalFlagsTest {

    @Test
    public void testBuilderDefaults() {
        UserExternalFlags flags = UserExternalFlags.builder().build();
        assertFalse(flags.isFirstNameExternal());
        assertFalse(flags.isLastNameExternal());
        assertFalse(flags.isDisplayNameExternal());
        assertFalse(flags.isSyncExternalGroups());
        assertFalse(flags.isEmailExternal());
        assertFalse(flags.isEmailVerified());

        for (Feature f : Feature.values()) {
            assertFalse(flags.checkFeature(f));
        }
    }

    @Test
    public void testBuilder() {
        UserExternalFlags flags = UserExternalFlags.builder().applyFeature(Feature.EXTERNAL_FIRST_NAME, true).build();
        assertTrue(flags.isFirstNameExternal());
        assertFalse(flags.isLastNameExternal());
        assertFalse(flags.isDisplayNameExternal());
        assertFalse(flags.isSyncExternalGroups());
        assertFalse(flags.isEmailExternal());
        assertFalse(flags.isEmailVerified());

        flags = UserExternalFlags.builder().applyFeature(Feature.EXTERNAL_LAST_NAME, true).build();
        assertFalse(flags.isFirstNameExternal());
        assertTrue(flags.isLastNameExternal());
        assertFalse(flags.isDisplayNameExternal());
        assertFalse(flags.isSyncExternalGroups());
        assertFalse(flags.isEmailExternal());
        assertFalse(flags.isEmailVerified());

        flags = UserExternalFlags.builder().applyFeature(Feature.EXTERNAL_DISPLAY_NAME, true).build();
        assertFalse(flags.isFirstNameExternal());
        assertFalse(flags.isLastNameExternal());
        assertTrue(flags.isDisplayNameExternal());
        assertFalse(flags.isSyncExternalGroups());
        assertFalse(flags.isEmailExternal());
        assertFalse(flags.isEmailVerified());

        flags = UserExternalFlags.builder().applyFeature(Feature.SYNC_EXTERNAL_GROUPS, true).build();
        assertFalse(flags.isFirstNameExternal());
        assertFalse(flags.isLastNameExternal());
        assertFalse(flags.isDisplayNameExternal());
        assertTrue(flags.isSyncExternalGroups());
        assertFalse(flags.isEmailExternal());
        assertFalse(flags.isEmailVerified());

        flags = UserExternalFlags.builder().applyFeature(Feature.EXTERNAL_EMAIL, true).build();
        assertFalse(flags.isFirstNameExternal());
        assertFalse(flags.isLastNameExternal());
        assertFalse(flags.isDisplayNameExternal());
        assertFalse(flags.isSyncExternalGroups());
        assertTrue(flags.isEmailExternal());
        assertFalse(flags.isEmailVerified());

        flags = UserExternalFlags.builder().applyFeature(Feature.EMAIL_VERIFIED, true).build();
        assertFalse(flags.isFirstNameExternal());
        assertFalse(flags.isLastNameExternal());
        assertFalse(flags.isDisplayNameExternal());
        assertFalse(flags.isSyncExternalGroups());
        assertFalse(flags.isEmailExternal());
        assertTrue(flags.isEmailVerified());
    }

    @Test
    public void testAllTrue() {
        UserExternalFlags flags = UserExternalFlags.builder()
            .applyFeature(Feature.EXTERNAL_DISPLAY_NAME, true)
            .applyFeature(Feature.SYNC_EXTERNAL_GROUPS, true)
            .applyFeature(Feature.EXTERNAL_LAST_NAME, true)
            .applyFeature(Feature.EXTERNAL_FIRST_NAME, true)
            .applyFeature(Feature.EXTERNAL_EMAIL, true)
            .applyFeature(Feature.EMAIL_VERIFIED, true)
            .build();
        assertTrue(flags.isFirstNameExternal());
        assertTrue(flags.isLastNameExternal());
        assertTrue(flags.isDisplayNameExternal());
        assertTrue(flags.isSyncExternalGroups());
        assertTrue(flags.isEmailExternal());
        assertTrue(flags.isEmailVerified());

        Builder builder = UserExternalFlags.builder();
        Stream.of(Feature.values()).forEach(builder::withFeature);
        flags = builder.build();
        for (Feature f : Feature.values()) {
            assertTrue(flags.checkFeature(f));
        }
    }

    @Test
    public void testGeneric() {
        for (Feature f : Feature.values()) {
            UserExternalFlags flags = UserExternalFlags.builder().withFeature(f).build();
            for (Feature f2 : Feature.values()) {
                if (f2 == f) {
                    assertTrue(flags.checkFeature(f2));
                } else {
                    assertFalse(flags.checkFeature(f2));
                }
            }
        }
    }

}
