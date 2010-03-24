package org.openl.rules.workspace.dtr.impl;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RProperty;
import org.openl.rules.repository.RPropertyType;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.props.PropertiesContainer;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.impl.PropertiesContainerImpl;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;

public abstract class RepositoryProjectArtefactImpl implements RepositoryProjectArtefact {
    private static final Log log = LogFactory.getLog(RepositoryProjectArtefactImpl.class);

    private String name;
    private ArtefactPath path;
    private PropertiesContainer properties;
    private LinkedList<ProjectVersion> versions;
    private REntity rulesEntity;

    protected RepositoryProjectArtefactImpl(REntity rulesEntity, ArtefactPath path) {
        this.rulesEntity = rulesEntity;
        name = rulesEntity.getName();
        this.path = path;

        reLoadVersions();
        properties = new PropertiesContainerImpl();
    }

    public void addProperty(Property property) throws PropertyException {
        throw new PropertyException("Not supported", null);
    }

    public ArtefactPath getArtefactPath() {
        return path;
    }

    public Date getEffectiveDate() {
        return rulesEntity.getEffectiveDate();
    }

    public Date getExpirationDate() {
        return rulesEntity.getExpirationDate();
    }

    public String getLineOfBusiness() {
        return rulesEntity.getLineOfBusiness();
    }

    public String getName() {
        return name;
    }

    public Collection<Property> getProperties() {
        return properties.getProperties();
    }

    public Property getProperty(String name) throws PropertyException {
        return properties.getProperty(name);
    }

    public Map<String, Object> getProps() {
        return rulesEntity.getProps();
    }

    // all for project, main for content
    public Collection<ProjectVersion> getVersions() {
        reLoadVersions();
        return versions;
    }

    public boolean hasProperty(String name) {
        return properties.hasProperty(name);
    }

    protected void reLoadVersions() {
        LinkedList<ProjectVersion> vers = new LinkedList<ProjectVersion>();

        try {
            for (RVersion rv : rulesEntity.getVersionHistory()) {
                RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy()
                        .getUserName());
                vers.add(new RepositoryProjectVersionImpl(rv, rvii));
            }

            versions = vers;
        } catch (RRepositoryException e) {
            log.error("Failed to get version history!", e);
        }
    }

    public Property removeProperty(String name) throws PropertyException {
        throw new PropertyException("Not supported", null);
    }

    public void setEffectiveDate(Date date) throws ProjectException {
        try {
            rulesEntity.setEffectiveDate(date);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }
    }

    public void setExpirationDate(Date date) throws ProjectException {
        try {
            rulesEntity.setExpirationDate(date);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        try {
            rulesEntity.setLineOfBusiness(lineOfBusiness);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }
    }

    public void setProps(Map<String, Object> props) throws ProjectException {
        try {
            rulesEntity.setProps(props);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }
    }

    // --- protected

    public void update(ProjectArtefact srcArtefact) throws ProjectException {
        if (srcArtefact instanceof RulesRepositoryArtefact) {
            RulesRepositoryArtefact rra = (RulesRepositoryArtefact) srcArtefact;

            try {
                rulesEntity.setEffectiveDate(rra.getEffectiveDate());
                rulesEntity.setExpirationDate(rra.getExpirationDate());
                rulesEntity.setLineOfBusiness(rra.getLineOfBusiness());

                rulesEntity.setProps(rra.getProps());
            } catch (RRepositoryException e) {
                throw new ProjectException("Failed to update rules properties", e);
            }
        }

        HashMap<String, Property> srcProps = new HashMap<String, Property>();
        for (Property p : srcArtefact.getProperties()) {
            srcProps.put(p.getName(), p);
        }

        HashMap<String, RProperty> rulesProps = new HashMap<String, RProperty>();
        for (RProperty rp : rulesEntity.getProperties()) {
            rulesProps.put(rp.getName(), rp);
        }

        // remove & update in RulesRepository
        for (RProperty rp : rulesProps.values()) {
            String name = rp.getName();
            Property srcProp = srcProps.get(name);

            if (srcProp == null) {
                try {
                    // remove
                    rulesEntity.removeProperty(name);
                    rulesProps.remove(name);
                } catch (RRepositoryException e) {
                    throw new ProjectException("Cannot remove property {0}", e, name);
                }
            } else {
                // update
                Object rulesValue = rp.getValue();
                Object srcValue = srcProp.getValue();

                if (!srcValue.equals(rulesValue)) {
                    try {
                        rp.setValue(srcValue);
                    } catch (RRepositoryException e) {
                        throw new ProjectException("Cannot update property {0}", e, name);
                    }
                }
            }
        }

        // add new
        for (Property p : srcProps.values()) {
            String name = p.getName();
            RProperty rp = rulesProps.get(name);

            if (rp == null) {
                RPropertyType type;
                switch (p.getType()) {
                    case DATE:
                        type = RPropertyType.DATE;
                        break;
                    default:
                        // STRING
                        type = RPropertyType.STRING;
                }

                try {
                    rulesEntity.addProperty(name, type, p.getValue());
                } catch (RRepositoryException e) {
                    throw new ProjectException("Cannot add property {0}", e, name);
                }
            }
        }
    }
}
