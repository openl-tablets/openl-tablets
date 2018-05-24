package org.openl.rules.project.resolving;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.project.resolving.DefaultPropertiesFileNameProcessor.PatternModel;

public class DefaultPropertyFileNameProcessorTest {

    @Test
    public void unknownPropertyTest() throws Exception {
        try {
            new PatternModel("%unknownProperty%");
        } catch (InvalidFileNamePatternException e) {
            Assert.assertEquals("Wrong file name pattern! Unknown property: unknownProperty", e.getMessage());
            return;
        }
        Assert.fail();
    }

}
