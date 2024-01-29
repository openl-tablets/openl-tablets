package org.openl.rules.project.abstraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.util.IOUtils;

public class ProjectDescriptorSerializerTest {

    private ProjectDescriptorSerializer serializer;

    @BeforeEach
    public void setUp() throws JAXBException {
        this.serializer = new ProjectDescriptorSerializer();
    }

    @Test
    public void serializeTest() throws IOException, JAXBException {
        assertXml(new FileInputStream("test-resources/xml/descriptor1.xml"), serializer.serialize(makeDescriptors()));
    }

    @Test
    public void serializeEmpty() throws IOException, JAXBException {
        assertXml(new FileInputStream("test-resources/xml/empty_descriptor.xml"), serializer.serialize(null));
        assertXml(new FileInputStream("test-resources/xml/empty_descriptor.xml"),
            serializer.serialize(Collections.emptyList()));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void deserialize() throws FileNotFoundException, JAXBException {
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
    public void checkCompatability() throws IOException, JAXBException {
        byte[] xml;
        try (var input = serializer.serialize(makeDescriptors())) {
            xml = input.readAllBytes();
        }
        List<ProjectDescriptor> result = serializer.deserialize(new ByteArrayInputStream(xml));
        try (var input = serializer.serialize(result)) {
            assertXml(xml, input.readAllBytes());
        }

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
    public void deserializeEmpty() throws FileNotFoundException, JAXBException {
        List<ProjectDescriptor> result = serializer
            .deserialize(new FileInputStream("test-resources/xml/empty_descriptor.xml"));
        assertEquals(0, result.size());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void deserializeBlack() throws JAXBException {
        List<ProjectDescriptor> result = serializer.deserialize(IOUtils.toInputStream(""));
        assertNull(result);
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
