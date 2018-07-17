package org.openl.rules.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.util.IOUtils;

public class ProjectDescriptorHelperTest {
    private static String XML = "<descriptors>\n" +
            "  <descriptor>\n" +
            "    <projectName>project1</projectName>\n" +
            "    <projectVersion>3.5.11</projectVersion>\n" +
            "  </descriptor>\n" +
            "  <descriptor>\n" +
            "    <projectName>project2</projectName>\n" +
            "    <projectVersion>17</projectVersion>\n" +
            "  </descriptor>\n" +
            "  <descriptor>\n" +
            "    <projectName>project0</projectName>\n" +
            "    <projectVersion>0</projectVersion>\n" +
            "  </descriptor>\n" +
            "</descriptors>\n";

    @Test
    public void serialize() throws IOException {
        List<ProjectDescriptor> descriptors = makeDescriptors();
        InputStream stream = ProjectDescriptorHelper.serialize(descriptors);
        String xml = IOUtils.toStringAndClose(stream);
        Assert.assertEquals(XML, xml);

    }

    private static List<ProjectDescriptor> makeDescriptors() {
        ProjectDescriptor prj1 = new ProjectDescriptorImpl("project1", new CommonVersionImpl(3, 5, 11));
        ProjectDescriptor prj2 = new ProjectDescriptorImpl("project2", new CommonVersionImpl(17));
        ProjectDescriptor prj3 = new ProjectDescriptorImpl("project0", new CommonVersionImpl(0));
        return new ArrayList<>(Arrays.asList(prj1, prj2, prj3));
    }

    @Test
    public void deserialize() {
        InputStream stream = IOUtils.toInputStream(XML);
        List<ProjectDescriptor> result = ProjectDescriptorHelper.deserialize(stream);
        List<ProjectDescriptor> expected = makeDescriptors();
        Assert.assertEquals(expected.size(), result.size());
        Assert.assertEquals(expected.get(0).getProjectName(), result.get(0).getProjectName());
        Assert.assertEquals(expected.get(0).getProjectVersion(), result.get(0).getProjectVersion());
        Assert.assertEquals(expected.get(1).getProjectName(), result.get(1).getProjectName());
        Assert.assertEquals(expected.get(1).getProjectVersion(), result.get(1).getProjectVersion());
        Assert.assertEquals(expected.get(2).getProjectName(), result.get(2).getProjectName());
        Assert.assertEquals(expected.get(2).getProjectVersion(), result.get(2).getProjectVersion());
    }
}