package org.openl.rules.ruleservice.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DeployStrategyTest {

    @Test
    public void testFromString() {
        assertEquals(DeployStrategy.IF_ABSENT, DeployStrategy.fromString("IF_ABSENT"));
        assertEquals(DeployStrategy.IF_ABSENT, DeployStrategy.fromString("true"));
        assertEquals(DeployStrategy.NEVER, DeployStrategy.fromString(null));
        assertEquals(DeployStrategy.NEVER, DeployStrategy.fromString("  "));
        assertEquals(DeployStrategy.NEVER, DeployStrategy.fromString("false"));
        assertEquals(DeployStrategy.NEVER, DeployStrategy.fromString("NEVER"));
        assertEquals(DeployStrategy.ALWAYS, DeployStrategy.fromString("ALWAYS"));
    }

}
