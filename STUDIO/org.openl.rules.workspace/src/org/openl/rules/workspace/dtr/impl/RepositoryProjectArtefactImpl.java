package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RProperty;
import org.openl.rules.repository.RPropertyType;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.RulesRepositoryFactory;
import org.openl.rules.repository.exceptions.RDeleteException;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.props.PropertiesContainer;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.PropertyTypeException;
import org.openl.rules.workspace.props.impl.PropertiesContainerImpl;
import org.openl.rules.workspace.props.impl.PropertyImpl;
import org.openl.util.Log;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

public abstract class RepositoryProjectArtefactImpl implements RepositoryProjectArtefact {
    private String name;
    private ArtefactPath path;
    private PropertiesContainer properties;
    private LinkedList<ProjectVersion> versions;
    private REntity rulesEntity;

    protected RepositoryProjectArtefactImpl(REntity rulesEntity, ArtefactPath path) {
        this.rulesEntity = rulesEntity;
        this.name = rulesEntity.getName();
        this.path = path;

        versions = new LinkedList<ProjectVersion>();
        properties = new PropertiesContainerImpl();

        reLoad();
    }

    public String getName() {
        return name;
    }

    public ArtefactPath getArtefactPath() {
        return path;
    }

    public boolean hasProperty(String name) {
        return properties.hasProperty(name);
    }

    public Property getProperty(String name) throws PropertyException {
        return properties.getProperty(name);
    }

    public Collection<Property> getProperties() {
        return properties.getProperties();
    }

    public void addProperty(Property property) throws PropertyTypeException {
        throw new PropertyTypeException("Not supported", null);
    }

    public Property removeProperty(String name) throws PropertyException {
        throw new PropertyException("Not supported", null);
    }

    // all for project, main for content
    public Collection<ProjectVersion> getVersions() {
        // TODO use updated each time
        return versions;
    }

    public void update(ProjectArtefact srcArtefact) throws ProjectException {
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
                } catch (RDeleteException e) {
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

    // --- protected

    protected void reLoad() {
        LinkedList<ProjectVersion> vers = new LinkedList<ProjectVersion>();
        
        try {
            for (RVersion rv : rulesEntity.getVersionHistory()) {
                RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy().getName());
                vers.add(new RepositoryProjectVersionImpl(rv.getMajor(), rv.getMinor(), rv.getRevision(), rvii));
            }
            
            versions = vers;
        } catch (RRepositoryException e) {
            Log.error("Failed to get version history", e);
        }

        PropertiesContainer props = new PropertiesContainerImpl();
        
        for (RProperty rp : rulesEntity.getProperties()) {
            String name = rp.getName();
            Object value = rp.getValue();
            
            Property prop;
            switch (rp.getType()) {
            case DATE:
                prop = new PropertyImpl(name, (Date) value);
                break;
            default:
                prop = new PropertyImpl(name, value.toString());
            }
            
            try {
                props.addProperty(prop);
            } catch (PropertyTypeException e) {
                // ignore -- must never happen
            }            
        }
        
        properties = props;
    }
}
