package org.openl.rules.project;

import java.io.ByteArrayInputStream;

import junit.framework.Assert;

import org.junit.Test;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;

public class XmlProjectDescriptorSerializerTest {

	@Test
	public void test() {
		XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
		ProjectDescriptor source = getProjectDescriptor();
		String value = serializer.serialize(source);
		ProjectDescriptor result = serializer
				.deserialize(new ByteArrayInputStream(value.getBytes()));
		String resultValue = result.getConfiguration().getPropertyValue(
				"propertyName");
		Assert.assertEquals("propertyValue", resultValue);
	}

	private ProjectDescriptor getProjectDescriptor() {
		ProjectDescriptor projectDescriptor = new ProjectDescriptor();
		projectDescriptor.getConfiguration().addProperty("propertyName",
				"propertyValue");
		return projectDescriptor;
	}
}
