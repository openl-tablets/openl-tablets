package org.openl.rules.workspace.mock;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class MockArtefact implements ProjectArtefact, RulesRepositoryArtefact {

    private final static PropertyException PROPERTY_EXCEPTION = new PropertyException("", null);
    private String name;
    private MockFolder parent;

    private Date effectiveDate;
    private Date expirationDate;
    private String lineOfBusiness;
    
    private String attribute1;
    private String attribute2;
    private String attribute3;
    private String attribute4;
    private String attribute5;
    private Date attribute6;
    private Date attribute7;
    private Date attribute8;
    private Date attribute9;
    private Date attribute10;
    private Double attribute11;
    private Double attribute12;
    private Double attribute13;
    private Double attribute14;
    private Double attribute15;

    public MockArtefact(String name, MockFolder parent) {
        this.name = name;
        this.parent = parent;
    }

    public MockFolder up() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArtefactPath getArtefactPath() {
        return null;
    }

    public ProjectArtefact getArtefact(String name) throws ProjectException {
        return null;
    }

    public boolean hasArtefact(String name) {
        return false;
    }

    public boolean isFolder() {
        return false;
    }

    /**
     * Checks whether property with specified name exists in the container.
     *
     * @param name name of property
     * @return <code>true</code> if such property exists
     */
    public boolean hasProperty(String name) {
        return false;
    }

    /**
     * Returns property by name.
     *
     * @param name name of property
     * @return reference on named property
     * @throws org.openl.rules.workspace.props.PropertyException
     *          if no property with specified name
     */
    public Property getProperty(String name) throws PropertyException {
        throw PROPERTY_EXCEPTION;
    }

    /**
     * Gets list of all properties in the container.
     *
     * @return list of properties
     */
    public Collection<Property> getProperties() {
        return Collections.emptyList();
    }

    /**
     * Adds property into the container.
     *
     * @param property adding property
     * @throws org.openl.rules.workspace.props.PropertyException
     *          if property with the same name exists already and value cannot be updated.
     */
    public void addProperty(Property property) throws PropertyException {
    
    }

    /**
     * Removes property from the container.
     *
     * @param name name of property
     * @return removed property
     * @throws org.openl.rules.workspace.props.PropertyException
     *          if no property with specified name
     */
    public Property removeProperty(String name) throws PropertyException {
        throw PROPERTY_EXCEPTION;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getLineOfBusiness() {
        return lineOfBusiness;
    }

    public void setLineOfBusiness(String lineOfBusiness) {
        this.lineOfBusiness = lineOfBusiness;
    }

    public String getAttribute1() {
        return attribute1;
    }

    public void setAttribute1(String attribute1) {
        this.attribute1 = attribute1;
    }

    public String getAttribute2() {
        return attribute2;
    }

    public void setAttribute2(String attribute2) {
        this.attribute2 = attribute2;
    }

    public String getAttribute3() {
        return attribute3;
    }

    public void setAttribute3(String attribute3) {
        this.attribute3 = attribute3;
    }

    public String getAttribute4() {
        return attribute4;
    }

    public void setAttribute4(String attribute4) {
        this.attribute4 = attribute4;
    }

    public String getAttribute5() {
        return attribute5;
    }

    public void setAttribute5(String attribute5) {
        this.attribute5 = attribute5;
    }

    public Date getAttribute6() {
        return attribute6;
    }

    public void setAttribute6(Date attribute6) {
        this.attribute6 = attribute6;
    }

    public Date getAttribute7() {
        return attribute7;
    }

    public void setAttribute7(Date attribute7) {
        this.attribute7 = attribute7;
    }

    public Date getAttribute8() {
        return attribute8;
    }

    public void setAttribute8(Date attribute8) {
        this.attribute8 = attribute8;
    }

    public Date getAttribute9() {
        return attribute9;
    }

    public void setAttribute9(Date attribute9) {
        this.attribute9 = attribute9;
    }

    public Date getAttribute10() {
        return attribute10;
    }

    public void setAttribute10(Date attribute10) {
        this.attribute10 = attribute10;
    }

    public Double getAttribute11() {
        return attribute11;
    }

    public void setAttribute11(Double attribute11) {
        this.attribute11 = attribute11;
    }

    public Double getAttribute12() {
        return attribute12;
    }

    public void setAttribute12(Double attribute12) {
        this.attribute12 = attribute12;
    }

    public Double getAttribute13() {
        return attribute13;
    }

    public void setAttribute13(Double attribute13) {
        this.attribute13 = attribute13;
    }

    public Double getAttribute14() {
        return attribute14;
    }

    public void setAttribute14(Double attribute14) {
        this.attribute14 = attribute14;
    }

    public Double getAttribute15() {
        return attribute15;
    }

    public void setAttribute15(Double attribute15) {
        this.attribute15 = attribute15;
    }

}
