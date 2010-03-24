package org.openl.rules.workspace.uw.impl;

import static org.openl.rules.security.Privileges.PRIVILEGE_EDIT;
import static org.openl.rules.security.SecurityUtils.isGranted;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.props.Property;
import org.openl.rules.workspace.props.PropertyException;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;

public abstract class UserWorkspaceProjectArtefactImpl implements UserWorkspaceProjectArtefact {
    private UserWorkspaceProjectImpl project;

    private LocalProjectArtefact localArtefact;
    private RepositoryProjectArtefact dtrArtefact;

    protected UserWorkspaceProjectArtefactImpl(UserWorkspaceProjectImpl project, LocalProjectArtefact localArtefact, RepositoryProjectArtefact dtrArtefact) {
        this.project = project;

        updateArtefact(localArtefact, dtrArtefact);
    }

    public ArtefactPath getArtefactPath() {
        return getArtefact().getArtefactPath();
    }

    public String getName() {
        return getArtefact().getName();
    }

    public void addProperty(Property property) throws PropertyException {
        // TODO check whether it can edit
        getArtefact().addProperty(property);
    }

    public Collection<Property> getProperties() {
        return getArtefact().getProperties();
    }

    public Property getProperty(String name) throws PropertyException {
        return getArtefact().getProperty(name);
    }

    public boolean hasProperty(String name) {
        return getArtefact().hasProperty(name);
    }

    public Property removeProperty(String name) throws PropertyException {
        // TODO check whether it can edit
        return getArtefact().removeProperty(name);
    }

    public Collection<ProjectVersion> getVersions() {
        if (dtrArtefact != null) {
            return dtrArtefact.getVersions();
        } else {
            // no versions
            return new LinkedList<ProjectVersion>();
        }
    }

    public Date getEffectiveDate() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getEffectiveDate();
    }

    public Date getExpirationDate() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getExpirationDate();
    }

    public String getLineOfBusiness() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getLineOfBusiness();
    }

    public String getAttribute1() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute1();
    }
   
    public String getAttribute2() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute2();
    }
    
    public String getAttribute3() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute3();
    }
    
    public String getAttribute4() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute4();
    }
    
    public String getAttribute5() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute5();
    }
    
    public Date getAttribute6() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute6();
    }
    
    public Date getAttribute7() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute7();
    }
    
    public Date getAttribute8() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute8();
    }
    
    public Date getAttribute9() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute9();
    }
    
    public Date getAttribute10() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute10();
    }
    
    public Double getAttribute11() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute11();
    }
    
    public Double getAttribute12() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute12();
    }
    
    public Double getAttribute13() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute13();
    }
        
    public Double getAttribute14() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute14();
    }

    public Double getAttribute15() {
        RulesRepositoryArtefact rra = (RulesRepositoryArtefact) getArtefact();
        return rra.getAttribute15();
    }
    
    public boolean getCanModify() {
        return (!isReadOnly() && isGranted(PRIVILEGE_EDIT));
    }

    public void setEffectiveDate(Date date) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set effectiveDate in read mode");
        } else {
            localArtefact.setEffectiveDate(date);
        }
    }

    public void setExpirationDate(Date date) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set expirationDate in read mode");
        } else {
            localArtefact.setExpirationDate(date);
        }
    }

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set LOB in read mode");
        } else {
            localArtefact.setLineOfBusiness(lineOfBusiness);
        }
    }

    public void setAttribute1(String attribute1) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute1 in read mode");
        } else {
            localArtefact.setAttribute1(attribute1);
        }
    }
    
    public void setAttribute2(String attribute2) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute2 in read mode");
        } else {
            localArtefact.setAttribute2(attribute2);
        }
    }
    
    public void setAttribute3(String attribute3) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute3 in read mode");
        } else {
            localArtefact.setAttribute3(attribute3);
        }
    }
    
    public void setAttribute4(String attribute4) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute4 in read mode");
        } else {
            localArtefact.setAttribute4(attribute4);
        }
    }
    
    public void setAttribute5(String attribute5) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute5 in read mode");
        } else {
            localArtefact.setAttribute5(attribute5);
        }
    }
    
    public void setAttribute6(Date attribute6) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute6 in read mode");
        } else {
            localArtefact.setAttribute6(attribute6);
        }
    }
    
    public void setAttribute7(Date attribute7) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute7 in read mode");
        } else {
            localArtefact.setAttribute7(attribute7);
        }
    }
    
    public void setAttribute8(Date attribute8) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute8 in read mode");
        } else {
            localArtefact.setAttribute8(attribute8);
        }
    }
    
    public void setAttribute9(Date attribute9) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute9 in read mode");
        } else {
            localArtefact.setAttribute9(attribute9);
        }
    }
    
    public void setAttribute10(Date attribute10) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute10 in read mode");
        } else {
            localArtefact.setAttribute10(attribute10);
        }
    }
    
    public void setAttribute11(Double attribute11) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute11 in read mode");
        } else {
            localArtefact.setAttribute11(attribute11);
        }
    }
    
    public void setAttribute12(Double attribute12) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute12 in read mode");
        } else {
            localArtefact.setAttribute12(attribute12);
        }
    }
    
    public void setAttribute13(Double attribute13) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute13 in read mode");
        } else {
            localArtefact.setAttribute13(attribute13);
        }
    }
    
    public void setAttribute14(Double attribute14) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute14 in read mode");
        } else {
            localArtefact.setAttribute14(attribute14);
        }
    }
    
    public void setAttribute15(Double attribute15) throws ProjectException {
        if (isReadOnly()) {
            throw new ProjectException("Cannot set attribute15 in read mode");
        } else {
            localArtefact.setAttribute15(attribute15);
        }
    }
    
    // --- protected

    protected void updateArtefact(LocalProjectArtefact localArtefact, RepositoryProjectArtefact dtrArtefact) {
        this.localArtefact = localArtefact;
        this.dtrArtefact = dtrArtefact;
    }

    protected ProjectArtefact getArtefact() {
        return (isLocal()) ? localArtefact : dtrArtefact;
    }

    protected boolean isLocal() {
        return project.isLocal();
    }

    public boolean isReadOnly() {
        return project.isReadOnly();
    }

    public UserWorkspaceProjectImpl getProject() {
        return project;
    }

    // can be used in constructor of UserWorkspaceProjectImpl only
    protected void setProject(UserWorkspaceProjectImpl project) {
        this.project = project;
    }
}
