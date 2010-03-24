package org.openl.rules.repository.jcr;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RProperty;
import org.openl.rules.repository.RPropertyType;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrOldEntity implements REntity {
    private Node node;
    private String name;
    
    private JcrVersion version;
    private JcrOldEntity parent;
    
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

    public JcrOldEntity(JcrOldEntity parent, String name, Node node) throws RepositoryException {
        this.parent = parent;
        this.node = node;
        this.name = name;

        this.version = new JcrVersion(node);
        
        if (node.hasProperty(JcrNT.PROP_EFFECTIVE_DATE)) {
            effectiveDate = node.getProperty(JcrNT.PROP_EFFECTIVE_DATE).getDate().getTime();
        }
        if (node.hasProperty(JcrNT.PROP_EXPIRATION_DATE)) {
            expirationDate = node.getProperty(JcrNT.PROP_EXPIRATION_DATE).getDate().getTime();
        }
        if (node.hasProperty(JcrNT.PROP_LINE_OF_BUSINESS)) {
            lineOfBusiness = node.getProperty(JcrNT.PROP_LINE_OF_BUSINESS).getString();
        }
        
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE1)) {
            attribute1 = node.getProperty(JcrNT.PROP_ATTRIBUTE1).getString();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE2)) {
            attribute2 = node.getProperty(JcrNT.PROP_ATTRIBUTE2).getString();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE3)) {
            attribute3 = node.getProperty(JcrNT.PROP_ATTRIBUTE3).getString();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE4)) {
            attribute4 = node.getProperty(JcrNT.PROP_ATTRIBUTE4).getString();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE5)) {
            attribute5 = node.getProperty(JcrNT.PROP_ATTRIBUTE5).getString();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE6)) {
            attribute6 = new Date(node.getProperty(JcrNT.PROP_ATTRIBUTE6).getLong());
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE7)) {
            attribute7 = new Date(node.getProperty(JcrNT.PROP_ATTRIBUTE7).getLong());
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE8)) {
            attribute8 = new Date(node.getProperty(JcrNT.PROP_ATTRIBUTE8).getLong());
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE9)) {
            attribute9 = new Date(node.getProperty(JcrNT.PROP_ATTRIBUTE9).getLong());
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE10)) {
            attribute10 = new Date(node.getProperty(JcrNT.PROP_ATTRIBUTE10).getLong());
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE11)) {
            attribute11 = node.getProperty(JcrNT.PROP_ATTRIBUTE11).getDouble();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE12)) {
            attribute12 = node.getProperty(JcrNT.PROP_ATTRIBUTE12).getDouble();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE13)) {
            attribute13 = node.getProperty(JcrNT.PROP_ATTRIBUTE13).getDouble();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE14)) {
            attribute14 = node.getProperty(JcrNT.PROP_ATTRIBUTE14).getDouble();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE15)) {
            attribute15 = node.getProperty(JcrNT.PROP_ATTRIBUTE15).getDouble();
        }
    }
    
    public void addProperty(String name, RPropertyType type, Object value) throws RRepositoryException {
        notSupported();
    }

    public void delete() throws RRepositoryException {
        notSupported();
    }

    public RVersion getActiveVersion() {
        return version;
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

    public String getName() {
        return name;
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

    public String getPath() throws RRepositoryException {
        JcrOldEntity curr = this;
        StringBuilder sb = new StringBuilder(128);
        
        while (curr != null) {
            String s = curr.getName();
            if (s != null) {
                sb.insert(0, s);
                sb.insert(0, '/');
            }
            
            curr = curr.parent;
        }
        
        return sb.toString();
    }

    public Collection<RProperty> getProperties() {
        // not supported
        return null;
    }

    public RProperty getProperty(String name) throws RRepositoryException {
        throw new RRepositoryException("Cannot find property" ,null);
    }

    public List<RVersion> getVersionHistory() throws RRepositoryException {
        LinkedList<RVersion> result = new LinkedList<RVersion>();
        
        // only current version
        result.add(version);
        return result;
    }

    public boolean hasProperty(String name) {
        // not supported
        return false;
    }

    public void removeProperty(String name) throws RRepositoryException {
        notSupported();
    }

    public void setEffectiveDate(Date date) throws RRepositoryException {
        notSupported();
    }

    public void setExpirationDate(Date date) throws RRepositoryException {
        notSupported();
    }

    public void setLineOfBusiness(String lineOfBusiness) throws RRepositoryException {
        notSupported();
    }

    public void setAttribute1(String attribute1) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute2(String attribute2) throws RRepositoryException {
        notSupported();
    }

    public void setAttribute3(String attribute3) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute4(String attribute4) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute5(String attribute5) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute6(Date attribute6) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute7(Date attribute7) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute8(Date attribute8) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute9(Date attribute9) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute10(Date attribute10) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute11(Double attribute11) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute12(Double attribute12) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute13(Double attribute13) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute14(Double attribute14) throws RRepositoryException {
        notSupported();
    }
    
    public void setAttribute15(Double attribute15) throws RRepositoryException {
        notSupported();
    }

    // --- protected
    
    protected Node node() {
        return node;
    }
    
    protected void notSupported() throws RRepositoryException {
        throw new RRepositoryException("Cannot modify artefact version!", null);
    }

    /**
     * Checks whether type of the JCR node is correct.
     * Checks FROZEN Type
     *
     * @param frozenNodeType expected node type
     * @throws RepositoryException if failed
     */
    protected void checkNodeType(String frozenNodeType) throws RepositoryException {
        if (!node.isNodeType(JcrNT.NT_FROZEN_NODE)) {
            throw new RepositoryException("Not a frozen node!");
        }
        
        String actualFrozenNodeType = node.getProperty("jcr:frozenPrimaryType").getString();
        if (JcrNT.NT_FILES.equals(actualFrozenNodeType) && JcrNT.NT_FOLDER.equals(frozenNodeType)) {
            // openl:files -- openl:folder
            return;
        }
        
        if (!frozenNodeType.equals(actualFrozenNodeType)) {
            throw new RepositoryException("Invalid NodeType '" + actualFrozenNodeType + "'. Expects '" + frozenNodeType + "'.");
        }
    }
}
