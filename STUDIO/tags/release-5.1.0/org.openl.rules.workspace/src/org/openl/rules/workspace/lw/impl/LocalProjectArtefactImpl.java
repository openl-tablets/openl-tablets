package org.openl.rules.workspace.lw.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

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
    private static final Log log = LogFactory.getLog(LocalProjectArtefactImpl.class);

    private String name;
    private ArtefactPath path;
    private File location;

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
            String msg = MsgHelper.format("No file ''{0}'', nothing to remove.", f.getAbsolutePath());
            log.debug(msg);
        } else if (!f.delete()) {
            String msg = MsgHelper.format("Failed to remove file ''{0}''!", f.getAbsolutePath());
            log.warn(msg);
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
    
    public String getAttribute1() {
         return attribute1;
    }
    
    public String getAttribute2() {
        return attribute2;
    }
    
    public String getAttribute3() {
        return attribute3;
    }

    public String getAttribute4() {
        return attribute4;
    }

    public String getAttribute5() {
        return attribute5;
    }

    public Date getAttribute6() {
        return attribute6;
    }

    public Date getAttribute7() {
        return attribute7;
    }

    public Date getAttribute8() {
        return attribute8;
    }

    public Date getAttribute9() {
        return attribute9;
    }

    public Date getAttribute10() {
        return attribute10;
    }

    public Double getAttribute11() {
        return attribute11;
    }

    public Double getAttribute12() {
        return attribute12;
    }

    public Double getAttribute13() {
        return attribute13;
    }

    public Double getAttribute14() {
        return attribute14;
    }

    public Double getAttribute15() {
        return attribute15;
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
    
    public void setAttribute1(String attribute1) throws ProjectException {
        this.attribute1 = attribute1;
    }
    
    public void setAttribute2(String attribute2) throws ProjectException {
        this.attribute2 = attribute2;
    }
    
    public void setAttribute3(String attribute3) throws ProjectException {
        this.attribute3 = attribute3;
    }
    
    public void setAttribute4(String attribute4) throws ProjectException {
        this.attribute4 = attribute4;
    }
    
    public void setAttribute5(String attribute5) throws ProjectException {
        this.attribute5 = attribute5;
    }
    
    public void setAttribute6(Date attribute6) throws ProjectException {
        this.attribute6 = attribute6;
    }
    
    public void setAttribute7(Date attribute7) throws ProjectException {
        this.attribute7 = attribute7;
    }
    
    public void setAttribute8(Date attribute8) throws ProjectException {
        this.attribute8 = attribute8;
    }
    
    public void setAttribute9(Date attribute9) throws ProjectException {
        this.attribute9 = attribute9;
    }
    
    public void setAttribute10(Date attribute10) throws ProjectException {
        this.attribute10 = attribute10;
    }
    
    public void setAttribute11(Double attribute11) throws ProjectException {
        this.attribute11 = attribute11;
    }
    
    public void setAttribute12(Double attribute12) throws ProjectException {
        this.attribute12 = attribute12;
    }
    
    public void setAttribute13(Double attribute13) throws ProjectException {
        this.attribute13 = attribute13;
    }
    
    public void setAttribute14(Double attribute14) throws ProjectException {
        this.attribute14 = attribute14;
    }

    public void setAttribute15(Double attribute15) throws ProjectException {
        this.attribute15 = attribute15;
    }

    // --- protected

    protected void setNew(boolean aNew) {
        isNew = aNew;
    }

    protected void setChanged(boolean changed) {
        isChanged = changed;
    }
    
    protected void resetNewAndChanged() {
	setNew(false);
	setChanged(false);
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
            
            attribute1 = rulesArtefact.getAttribute1();
            attribute2 = rulesArtefact.getAttribute2();
            attribute3 = rulesArtefact.getAttribute3();
            attribute4 = rulesArtefact.getAttribute4();
            attribute5 = rulesArtefact.getAttribute5();
            attribute6 = rulesArtefact.getAttribute6();
            attribute7 = rulesArtefact.getAttribute7();
            attribute8 = rulesArtefact.getAttribute8();
            attribute9 = rulesArtefact.getAttribute9();
            attribute10 = rulesArtefact.getAttribute10();
            attribute11 = rulesArtefact.getAttribute11();
            attribute12 = rulesArtefact.getAttribute12();
            attribute13 = rulesArtefact.getAttribute13();
            attribute14 = rulesArtefact.getAttribute14();
            attribute15 = rulesArtefact.getAttribute15();
        }
    }
    
    public StateHolder getState() {
        ArtefactStateHolder state = new ArtefactStateHolder();
        
        state.isNew = isNew;
        state.isChanged = isChanged;
        
        state.effectiveDate = effectiveDate;
        state.expirationDate = expirationDate;
        state.LOB = lineOfBusiness;
        
        state.attribute1 = attribute1;
        state.attribute2 = attribute2;
        state.attribute3 = attribute3;
        state.attribute4 = attribute4;
        state.attribute5 = attribute5;
        state.attribute6 = attribute6;
        state.attribute7 = attribute7;
        state.attribute8 = attribute8;
        state.attribute9 = attribute9;
        state.attribute10 = attribute10;
        state.attribute11 = attribute11;
        state.attribute12 = attribute12;
        state.attribute13 = attribute13;
        state.attribute14 = attribute14;
        state.attribute15 = attribute15;
        
        state.properties = new ArrayList<Property>(properties.getProperties());
        
        return state;
    }
    
    public void setState(StateHolder aState) throws PropertyException {
        ArtefactStateHolder state = (ArtefactStateHolder) aState;
        
        effectiveDate = state.effectiveDate;
        expirationDate = state.expirationDate;
        lineOfBusiness = state.LOB;
        
        attribute1 = state.attribute1;
        attribute2 = state.attribute2;
        attribute3 = state.attribute3;
        attribute4 = state.attribute4;
        attribute5 = state.attribute5;
        attribute6 = state.attribute6;
        attribute7 = state.attribute7;
        attribute8 = state.attribute8;
        attribute9 = state.attribute9;
        attribute10 = state.attribute10;
        attribute11 = state.attribute11;
        attribute12 = state.attribute12;
        attribute13 = state.attribute13;
        attribute14 = state.attribute14;
        attribute15 = state.attribute15;
        
        properties.removeAll();
        for (Property prop : state.properties) {
            properties.addProperty(prop);
        }

        isNew = state.isNew;
        isChanged = state.isChanged;
    }

    private static class ArtefactStateHolder implements StateHolder {
        private static final long serialVersionUID = 1049629652852513808L;

        boolean isNew;
        boolean isChanged;
        
        Date effectiveDate;
        Date expirationDate;
        String LOB;
        
        String attribute1;
        String attribute2;
        String attribute3;
        String attribute4;
        String attribute5;
        Date attribute6;
        Date attribute7;
        Date attribute8;
        Date attribute9;
        Date attribute10;
        Double attribute11;
        Double attribute12;
        Double attribute13;
        Double attribute14;
        Double attribute15;
        
        Collection<Property> properties;
    }
}
