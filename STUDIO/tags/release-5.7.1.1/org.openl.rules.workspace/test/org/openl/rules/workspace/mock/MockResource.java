package org.openl.rules.workspace.mock;

import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.ProjectException;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Map;

public class MockResource extends MockArtefact implements ProjectResource {
    private static final InputStream NULL_STREAM = new ByteArrayInputStream(new byte[0]);

    private String resourceType = "unknown";
    private InputStream inputStream = NULL_STREAM;

    public MockResource(String name, MockFolder parent) {
        super(name, parent);
    }

    public MockResource _setExpirationDate(Date expirationDate) {
        setExpirationDate(expirationDate);
        return this;
    }

    public MockResource _setLineOfBusiness(String lob) {
        setLineOfBusiness(lob);
        return this;
    }

    public MockResource _setProps(Map<String, Object> props) {
        setProps(props);
        return this;
    }

    public InputStream getContent() throws ProjectException {
        return inputStream;
    }

    public String getResourceType() {
        return resourceType;
    }

    public MockResource setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
