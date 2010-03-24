package org.openl.rules.workspace.dtr.impl;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

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
        this.name = rulesEntity.getName();
        this.path = path;

        reLoadVersions();
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
        throw new PropertyException("Not supported", null);
    }

    public Property removeProperty(String name) throws PropertyException {
        throw new PropertyException("Not supported", null);
    }

    // all for project, main for content
    public Collection<ProjectVersion> getVersions() {
        reLoadVersions();
        return versions;
    }

    public void update(ProjectArtefact srcArtefact) throws ProjectException {
        if (srcArtefact instanceof RulesRepositoryArtefact) {
            RulesRepositoryArtefact rra = (RulesRepositoryArtefact) srcArtefact;
            
            try {
                rulesEntity.setEffectiveDate(rra.getEffectiveDate());
                rulesEntity.setExpirationDate(rra.getExpirationDate());
                rulesEntity.setLineOfBusiness(rra.getLineOfBusiness());
                
                rulesEntity.setAttribute1(rra.getAttribute1());
                rulesEntity.setAttribute2(rra.getAttribute2());
                rulesEntity.setAttribute3(rra.getAttribute3());
                rulesEntity.setAttribute4(rra.getAttribute4());
                rulesEntity.setAttribute5(rra.getAttribute5());
                rulesEntity.setAttribute6(rra.getAttribute6());
                rulesEntity.setAttribute7(rra.getAttribute7());
                rulesEntity.setAttribute8(rra.getAttribute8());
                rulesEntity.setAttribute9(rra.getAttribute9());
                rulesEntity.setAttribute10(rra.getAttribute10());
                rulesEntity.setAttribute11(rra.getAttribute11());
                rulesEntity.setAttribute12(rra.getAttribute12());
                rulesEntity.setAttribute13(rra.getAttribute13());
                rulesEntity.setAttribute14(rra.getAttribute14());
                rulesEntity.setAttribute15(rra.getAttribute15());
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

    public Date getEffectiveDate() {
        return rulesEntity.getEffectiveDate();
    }

    public Date getExpirationDate() {
        return rulesEntity.getExpirationDate();
    }

    public String getLineOfBusiness() {
        return rulesEntity.getLineOfBusiness();
    }

    public String getAttribute1() {
        return rulesEntity.getAttribute1();
    }

    public String getAttribute2() {
        return rulesEntity.getAttribute2();
    }

    public String getAttribute3() {
        return rulesEntity.getAttribute3();
    }

    public String getAttribute4() {
        return rulesEntity.getAttribute4();
    }

    public String getAttribute5() {
        return rulesEntity.getAttribute5();
    }

    public Date getAttribute6() {
        return rulesEntity.getAttribute6();
    }

    public Date getAttribute7() {
        return rulesEntity.getAttribute7();
    }

    public Date getAttribute8() {
        return rulesEntity.getAttribute8();
    }

    public Date getAttribute9() {
        return rulesEntity.getAttribute9();
    }

    public Date getAttribute10() {
        return rulesEntity.getAttribute10();
    }

    public Double getAttribute11() {
        return rulesEntity.getAttribute11();
    }

    public Double getAttribute12() {
        return rulesEntity.getAttribute12();
    }

    public Double getAttribute13() {
        return rulesEntity.getAttribute13();
    }

    public Double getAttribute14() {
        return rulesEntity.getAttribute14();
    }

    public Double getAttribute15() {
        return rulesEntity.getAttribute15();
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
    
    public void setAttribute1(String attribute1) throws ProjectException {
        try {
            rulesEntity.setAttribute1(attribute1);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }
    
    public void setAttribute2(String attribute2) throws ProjectException {
        try {
            rulesEntity.setAttribute1(attribute2);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }
    
    public void setAttribute3(String attribute3) throws ProjectException {
        try {
            rulesEntity.setAttribute3(attribute3);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute4(String attribute4) throws ProjectException {
        try {
            rulesEntity.setAttribute4(attribute4);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute5(String attribute5) throws ProjectException {
        try {
            rulesEntity.setAttribute5(attribute5);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute6(Date attribute6) throws ProjectException {
        try {
            rulesEntity.setAttribute6(attribute6);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute7(Date attribute7) throws ProjectException {
        try {
            rulesEntity.setAttribute7(attribute7);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute8(Date attribute8) throws ProjectException {
        try {
            rulesEntity.setAttribute8(attribute8);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute9(Date attribute9) throws ProjectException {
        try {
            rulesEntity.setAttribute9(attribute9);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute10(Date attribute10) throws ProjectException {
        try {
            rulesEntity.setAttribute10(attribute10);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute11(Double attribute11) throws ProjectException {
        try {
            rulesEntity.setAttribute11(attribute11);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute12(Double attribute12) throws ProjectException {
        try {
            rulesEntity.setAttribute12(attribute12);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute13(Double attribute13) throws ProjectException {
        try {
            rulesEntity.setAttribute13(attribute13);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute14(Double attribute14) throws ProjectException {
        try {
            rulesEntity.setAttribute14(attribute14);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    public void setAttribute15(Double attribute15) throws ProjectException {
        try {
            rulesEntity.setAttribute15(attribute15);
        } catch (RRepositoryException e) {
            throw new ProjectException(null, e);
        }        
    }

    // --- protected

    protected void reLoadVersions() {
        LinkedList<ProjectVersion> vers = new LinkedList<ProjectVersion>();
        
        try {
            for (RVersion rv : rulesEntity.getVersionHistory()) {
                RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(rv.getCreated(), rv.getCreatedBy().getUserName());
                vers.add(new RepositoryProjectVersionImpl(rv, rvii));
            }
            
            versions = vers;
        } catch (RRepositoryException e) {
            log.error("Failed to get version history!", e);
        }
    }
}
