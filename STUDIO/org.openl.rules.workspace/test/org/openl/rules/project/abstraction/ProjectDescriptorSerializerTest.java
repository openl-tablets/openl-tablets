package org.openl.rules.project.abstraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.util.IOUtils;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

public class ProjectDescriptorSerializerTest {

    private ProjectDescriptorSerializer serializer;

    @Before
    public void setUp() {
        this.serializer = new ProjectDescriptorSerializer();
    }

    @Test
    public void serializeTest() throws FileNotFoundException {
        assertXml(new FileInputStream("test-resources/xml/descriptor1.xml"), serializer.serialize(makeDescriptors()));
    }

    @Test
    public void serializeEmpty() throws FileNotFoundException {
        assertXml(new FileInputStream("test-resources/xml/empty_descriptor.xml"), serializer.serialize(null));
        assertXml(new FileInputStream("test-resources/xml/empty_descriptor.xml"),
            serializer.serialize(Collections.emptyList()));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void deserialize() throws FileNotFoundException {
        List<ProjectDescriptor> result = serializer
            .deserialize(new FileInputStream("test-resources/xml/descriptor1.xml"));
        List<ProjectDescriptor> expected = makeDescriptors();
        assertEquals(expected.size(), result.size());
        assertEquals(expected.get(0).getProjectName(), result.get(0).getProjectName());
        assertEquals(expected.get(0).getProjectVersion(), result.get(0).getProjectVersion());
        assertEquals(expected.get(1).getProjectName(), result.get(1).getProjectName());
        assertEquals(expected.get(1).getProjectVersion(), result.get(1).getProjectVersion());
        assertEquals(expected.get(2).getProjectName(), result.get(2).getProjectName());
        assertEquals(expected.get(2).getProjectVersion(), result.get(2).getProjectVersion());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void checkCompatability() throws IOException {
        String xml = IOUtils.toStringAndClose(serializer.serialize(makeDescriptors()));
        List<ProjectDescriptor> result = serializer.deserialize(IOUtils.toInputStream(xml));
        assertXml(xml, serializer.serialize(result));

        List<ProjectDescriptor> expected = makeDescriptors();
        assertEquals(expected.size(), result.size());
        assertEquals(expected.get(0).getProjectName(), result.get(0).getProjectName());
        assertEquals(expected.get(0).getProjectVersion(), result.get(0).getProjectVersion());
        assertEquals(expected.get(1).getProjectName(), result.get(1).getProjectName());
        assertEquals(expected.get(1).getProjectVersion(), result.get(1).getProjectVersion());
        assertEquals(expected.get(2).getProjectName(), result.get(2).getProjectName());
        assertEquals(expected.get(2).getProjectVersion(), result.get(2).getProjectVersion());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void deserializeEmpty() throws FileNotFoundException {
        List<ProjectDescriptor> result = serializer
            .deserialize(new FileInputStream("test-resources/xml/empty_descriptor.xml"));
        assertEquals(0, result.size());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void deserializeBlack() {
        List<ProjectDescriptor> result = serializer.deserialize(IOUtils.toInputStream(""));
        Assert.assertNull(result);
    }

    @SuppressWarnings("rawtypes")
    private static List<ProjectDescriptor> makeDescriptors() {
        ProjectDescriptor prj1 = new ProjectDescriptorImpl("design",
            "project1 & \\ <>",
            "project1 & \\ <>",
            "master",
            new CommonVersionImpl(3, 5, 11));
        ProjectDescriptor prj2 = new ProjectDescriptorImpl("design2",
            "project2",
            null,
            null,
            new CommonVersionImpl(17));
        ProjectDescriptor prj3 = new ProjectDescriptorImpl(null, "project0", null, null, new CommonVersionImpl(0));
        return new ArrayList<>(Arrays.asList(prj1, prj2, prj3));
    }

    private static void assertXml(Object expected, Object actual) {
        Iterator<Difference> differences = DiffBuilder.compare(expected)
            .withTest(actual)
            .ignoreWhitespace()
            .checkForSimilar()
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes, ElementSelectors.byName))
            .withDifferenceEvaluator(DifferenceEvaluators.Default)
            .build()
            .getDifferences()
            .iterator();
        if (differences.hasNext()) {
            fail("Difference\n\t" + differences.next());
        }
    }

}
