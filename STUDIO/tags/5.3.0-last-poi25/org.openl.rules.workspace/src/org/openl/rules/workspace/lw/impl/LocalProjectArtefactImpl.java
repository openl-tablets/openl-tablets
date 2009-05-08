package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.props.impl.PropertiesContainerImpl;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;
import org.openl.util.MsgHelper;

public abstract class LocalProjectArtefactImpl implements LocalProjectArtefact {
    private static class ArtefactStateHolder implements StateHolder {
        private static final long serialVersionUID = 1049629652852513808L;

        boolean isNew;
        boolean isChanged;

        Date effectiveDate;
        Date expirationDate;
        String LOB;

        Map<String, Object> props;

        Collection<Property> properties;
    }

    private static final Log log = LogFactory.getLog(LocalProjectArtefactImpl.class);
    private String name;
    private ArtefactPath path;

    private File location;
    private Date effectiveDate;
    private Date expirationDate;

    private String lineOfBusiness;

    private Map<String, Object> props;

    private PropertiesContainerImpl properties;
    private boolean isNew;

    private boolean isChanged;

    public LocalProjectArtefactImpl(String name, ArtefactPath path, File location) {
        this.name = name;
        this.path = path;
        this.location = location;

        properties = new PropertiesContainerImpl();
    }

    public void addProperty(Property property) throws PropertyException {
        properties.addProperty(property);
    }

    protected void downloadArtefact(ProjectArtefact artefact) throws ProjectException {
        if (artefact instanceof RulesRepositoryArtefact) {
            RulesRepositoryArtefact rulesArtefact = (RulesRepositoryArtefact) artefact;

            effectiveDate = rulesArtefact.getEffectiveDate();
            expirationDate = rulesArtefact.getExpirationDate();
            lineOfBusiness = rulesArtefact.getLineOfBusiness();

            props = rulesArtefact.getProps();
        }
    }

    public ArtefactPath getArtefactPath() {
        return path;
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

    protected File getLocation() {
        return location;
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
        return props;
    }

    public StateHolder getState() {
        ArtefactStateHolder state = new ArtefactStateHolder();

        state.isNew = isNew;
        state.isChanged = isChanged;

        state.effectiveDate = effectiveDate;
        state.expirationDate = expirationDate;
        state.LOB = lineOfBusiness;

        state.props = props;

        state.properties = new ArrayList<Property>(properties.getProperties());

        return state;
    }

    public boolean hasProperty(String name) {
        return properties.hasProperty(name);
    }

    public boolean isChanged() {
        return isChanged;
    }

    public boolean isNew() {
        return isNew;
    }

    public void remove() {
        File f = getLocation();
        if (!f.exists()) {
            String msg = MsgHelper.format("No file ''{0}'', nothing to remove.", f.getAbsolutePath());
            log.debug(msg);
        } else if (!f.delete()) {
            String msg = MsgHelper.format("Failed to remove file ''{0}''!", f.getAbsolutePath());
            log.warn(msg);
        }
    }

    public Property removeProperty(String name) throws PropertyException {
        return properties.removeProperty(name);
    }

    // --- protected

    protected void resetNewAndChanged() {
        setNew(false);
        setChanged(false);
    }

    protected void setChanged(boolean changed) {
        isChanged = changed;
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

    protected void setNew(boolean aNew) {
        isNew = aNew;
    }

    public void setProps(Map<String, Object> props) throws ProjectException {
        this.props = props;
    }

    public void setState(StateHolder aState) throws PropertyException {
        ArtefactStateHolder state = (ArtefactStateHolder) aState;

        effectiveDate = state.effectiveDate;
        expirationDate = state.expirationDate;
        lineOfBusiness = state.LOB;

        props = state.props;

        properties.removeAll();
        for (Property prop : state.properties) {
            properties.addProperty(prop);
        }

        isNew = state.isNew;
        isChanged = state.isChanged;
    }
}
