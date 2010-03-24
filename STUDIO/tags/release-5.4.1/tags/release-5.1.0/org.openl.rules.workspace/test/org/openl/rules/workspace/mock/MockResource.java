package org.openl.rules.workspace.mock;

import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.ProjectException;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Date;

public class MockResource extends MockArtefact implements ProjectResource{
    private static final InputStream NULL_STREAM = new ByteArrayInputStream(new byte[0]);

    private String resourceType = "unknown";
    private InputStream inputStream = NULL_STREAM;

    public MockResource(String name, MockFolder parent) {
        super(name, parent);
    }

    public InputStream getContent() throws ProjectException {
        return inputStream;
    }

    public MockResource setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public MockResource _setExpirationDate(Date expirationDate) {
        setExpirationDate(expirationDate);
        return this;
    }

    public MockResource _setLineOfBusiness(String lob) {
        setLineOfBusiness(lob);
        return this;
    }
    
    public MockResource _setAttribute1(String attribute1) {
        setAttribute1(attribute1);
        return this;
    }
    
    public MockResource _setAttribute2(String attribute2) {
        setAttribute2(attribute2);
        return this;
    }
    
    public MockResource _setAttribute3(String attribute3) {
        setAttribute3(attribute3);
        return this;
    }
    
    public MockResource _setAttribute4(String attribute4) {
        setAttribute4(attribute4);
        return this;
    }
    
    public MockResource _setAttribute5(String attribute5) {
        setAttribute5(attribute5);
        return this;
    }
    
    public MockResource _setAttribute6(Date attribute6) {
        setAttribute6(attribute6);
        return this;
    }
    
    public MockResource _setAttribute7(Date attribute7) {
        setAttribute7(attribute7);
        return this;
    }
    
    public MockResource _setAttribute8(Date attribute8) {
        setAttribute8(attribute8);
        return this;
    }
    
    public MockResource _setAttribute9(Date attribute9) {
        setAttribute9(attribute9);
        return this;
    }
    
    public MockResource _setAttribute10(Date attribute10) {
        setAttribute10(attribute10);
        return this;
    }
    
    public MockResource _setAttribute11(Double attribute11) {
        setAttribute11(attribute11);
        return this;
    }
    
    public MockResource _setAttribute12(Double attribute12) {
        setAttribute12(attribute12);
        return this;
    }
    
    public MockResource _setAttribute13(Double attribute13) {
        setAttribute13(attribute13);
        return this;
    }
    
    public MockResource _setAttribute14(Double attribute14) {
        setAttribute14(attribute14);
        return this;
    }
    
    public MockResource _setAttribute15(Double attribute15) {
        setAttribute15(attribute15);
        return this;
    }
}
