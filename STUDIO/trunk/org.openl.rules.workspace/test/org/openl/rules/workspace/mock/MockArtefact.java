//package org.openl.rules.workspace.mock;
//
//import org.openl.rules.workspace.abstracts.ArtefactPath;
//import org.openl.rules.workspace.abstracts.ProjectArtefact;
//import org.openl.rules.workspace.abstracts.ProjectException;
//import org.openl.rules.workspace.props.Property;
//import org.openl.rules.workspace.props.PropertyException;
//import org.openl.rules.workspace.repository.RulesRepositoryArtefact;
//
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Date;
//import java.util.Map;
//
//public class MockArtefact implements ProjectArtefact, RulesRepositoryArtefact {
//
//    private final static PropertyException PROPERTY_EXCEPTION = new PropertyException("", null);
//    private String name;
//    private MockFolder parent;
//
//    private Date effectiveDate;
//    private Date expirationDate;
//    private String lineOfBusiness;
//
//    private Map<String, Object> props;
//
//    public MockArtefact(String name, MockFolder parent) {
//        this.name = name;
//        this.parent = parent;
//    }
//
//    /**
//     * Adds property into the container.
//     *
//     * @param property adding property
//     * @throws org.openl.rules.workspace.props.PropertyException if property
//     *             with the same name exists already and value cannot be
//     *             updated.
//     */
//    public void addProperty(Property property) throws PropertyException {
//
//    }
//
//    public ProjectArtefact getArtefact(String name) throws ProjectException {
//        return null;
//    }
//
//    public ArtefactPath getArtefactPath() {
//        return null;
//    }
//
//    public Date getEffectiveDate() {
//        return effectiveDate;
//    }
//
//    public Date getExpirationDate() {
//        return expirationDate;
//    }
//
//    public String getLineOfBusiness() {
//        return lineOfBusiness;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    /**
//     * Gets list of all properties in the container.
//     *
//     * @return list of properties
//     */
//    public Collection<Property> getProperties() {
//        return Collections.emptyList();
//    }
//
//    /**
//     * Returns property by name.
//     *
//     * @param name name of property
//     * @return reference on named property
//     * @throws org.openl.rules.workspace.props.PropertyException if no property
//     *             with specified name
//     */
//    public Property getProperty(String name) throws PropertyException {
//        throw PROPERTY_EXCEPTION;
//    }
//
//    public Map<String, Object> getProps() {
//        return props;
//    }
//
//    public boolean hasArtefact(String name) {
//        return false;
//    }
//
//    /**
//     * Checks whether property with specified name exists in the container.
//     *
//     * @param name name of property
//     * @return <code>true</code> if such property exists
//     */
//    public boolean hasProperty(String name) {
//        return false;
//    }
//
//    public boolean isFolder() {
//        return false;
//    }
//
//    /**
//     * Removes property from the container.
//     *
//     * @param name name of property
//     * @return removed property
//     * @throws org.openl.rules.workspace.props.PropertyException if no property
//     *             with specified name
//     */
//    public Property removeProperty(String name) throws PropertyException {
//        throw PROPERTY_EXCEPTION;
//    }
//
//    public void setEffectiveDate(Date effectiveDate) {
//        this.effectiveDate = effectiveDate;
//    }
//
//    public void setExpirationDate(Date expirationDate) {
//        this.expirationDate = expirationDate;
//    }
//
//    public void setLineOfBusiness(String lineOfBusiness) {
//        this.lineOfBusiness = lineOfBusiness;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public void setProps(Map<String, Object> props) {
//        this.props = props;
//    }
//
//    public MockFolder up() {
//        return parent;
//    }
//
//}
