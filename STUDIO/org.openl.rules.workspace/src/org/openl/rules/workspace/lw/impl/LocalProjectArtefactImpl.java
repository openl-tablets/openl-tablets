package org.openl.rules.workspace.lw.impl;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.impl.PropertiesContainerImpl;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;

import java.util.Collection;
import java.util.Date;
import java.io.File;

public abstract class LocalProjectArtefactImpl implements LocalProjectArtefact {
    private String name;
    private ArtefactPath path;
    private File location;

    private Date effectiveDate;
    private Date expirationDate;
    private String lineOfBusiness;

    private PropertiesContainerImpl properties;

    private boolean isNew;
    private boolean isChanged;

    public LocalProjectArtefactImpl(String name, ArtefactPath path, File location) {
        this.name = name;
        this.path = path;
        this.location = location;

        properties = new PropertiesContainerImpl();
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

    public void addProperty(Property property) throws PropertyException {
        properties.addProperty(property);
    }

    public Property removeProperty(String name) throws PropertyException {
        return properties.removeProperty(name);
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isChanged() {
        return isChanged;
    }

    public void remove() {
        File f = getLocation();
        if (!f.exists()) {
            // TODO log -- nothing to remove
            ;
        } else if (!f.delete()) {
            // TODO log -- cannot delete file
            ;
        }
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }
    
    public Date getExpirationDate() {
        return expirationDate;
    }
    
    public String getLineOfBusiness() {
        return lineOfBusiness;
    }
    
    public void setEffectiveDate(Date date) throws ProjectException {
        effectiveDate = date;
    }
    
    public void setExpirationDate(Date date) throws ProjectException {
        expirationDate = date;
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        this.lineOfBusiness = lineOfBusiness;
    }
    
    // --- protected

    protected void setNew(boolean aNew) {
        isNew = aNew;
    }

    protected void setChanged(boolean changed) {
        isChanged = changed;
    }

    protected File getLocation() {
        return location;
    }
    
    protected void downloadArtefact(ProjectArtefact artefact) throws ProjectException {
        if (artefact instanceof RulesRepositoryArtefact) {
            RulesRepositoryArtefact rulesArtefact = (RulesRepositoryArtefact) artefact;
            
            effectiveDate = rulesArtefact.getEffectiveDate();
            expirationDate = rulesArtefact.getExpirationDate();
            lineOfBusiness = rulesArtefact.getLineOfBusiness();
        }
    }
}
