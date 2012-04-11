package org.openl.rules.repository.jcr;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RProperty;
import org.openl.rules.repository.RPropertyType;
import static org.openl.rules.repository.jcr.NodeUtil.isSame;
import static org.openl.rules.repository.jcr.NodeUtil.convertDate2Calendar;
import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Implementation of JCR Entity.
 * It is linked with node in JCR implementation, always.
 *
 * @author Aleh Bykhavets
 *
 */
public class JcrEntity extends JcrCommonArtefact implements REntity {
    private HashMap<String, RProperty> properties;

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

    public JcrEntity(Node node) throws RepositoryException {
        super(node);

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
            attribute6 = node.getProperty(JcrNT.PROP_ATTRIBUTE6).getDate().getTime();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE7)) {
            attribute7 = node.getProperty(JcrNT.PROP_ATTRIBUTE7).getDate().getTime();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE8)) {
            attribute8 = node.getProperty(JcrNT.PROP_ATTRIBUTE8).getDate().getTime();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE9)) {
            attribute9 = node.getProperty(JcrNT.PROP_ATTRIBUTE9).getDate().getTime();
        }
        if (node.hasProperty(JcrNT.PROP_ATTRIBUTE10)) {
            attribute10 = node.getProperty(JcrNT.PROP_ATTRIBUTE10).getDate().getTime();
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

        properties = new HashMap<String, RProperty>();
        initProperties();
    }

    // ------ protected methods ------

    public String getPath() throws RRepositoryException {
        StringBuilder sb = new StringBuilder(128);
        try {
            buildRelPath(sb, node());
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get relative path.", e);
        }

        return sb.toString();
    }

    public Collection<RProperty> getProperties() {
        return properties.values();
    }

    public void addProperty(String name, RPropertyType type, Object value) throws RRepositoryException {
        try {
            NodeUtil.smartCheckout(node(), false);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Internal error.", e);
        }

        if (hasProperty(name)) {
            removeProperty(name);
        }

        JcrProperty jp = new JcrProperty(this, name, type, value);
        properties.put(name, jp);
    }

    public void removeProperty(String name) throws RRepositoryException {
        RProperty rp = properties.get(name);

        if (rp == null) {
            throw new RRepositoryException("No such property ''{0}''.", null, name);
        }

        Node n = node();
        try {
            NodeUtil.smartCheckout(n, false);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Internal error.", e);
        }

        try {
            n.getProperty(name).remove();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot remove property ''{0}''.", e, name);
        }
        properties.remove(name);
    }

    public boolean hasProperty(String name) {
        return (properties.get(name) != null);
    }

    public RProperty getProperty(String name) throws RRepositoryException {
        RProperty rp = properties.get(name);

        if (rp == null) {
            throw new RRepositoryException("No such property ''{0}''.", null, name);
        }

        return rp;
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

    public void setEffectiveDate(Date date) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(effectiveDate, date)) return;
        
        Node n = node();
        Calendar c = convertDate2Calendar(date);

        try {
            NodeUtil.smartCheckout(n, false);
            n.setProperty(JcrNT.PROP_EFFECTIVE_DATE, c);
            effectiveDate = date;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set effectiveDate.", e);
        }
    }

    public void setExpirationDate(Date date) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(expirationDate, date)) return;

        Node n = node();
        Calendar c = convertDate2Calendar(date);

        try {
            NodeUtil.smartCheckout(n, false);
            n.setProperty(JcrNT.PROP_EXPIRATION_DATE, c);
            expirationDate = date;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set expirationDate.", e);
        }
    }

    public void setLineOfBusiness(String lineOfBusiness) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.lineOfBusiness, lineOfBusiness)) return;

        Node n = node();

        try {
            NodeUtil.smartCheckout(n, false);
            n.setProperty(JcrNT.PROP_LINE_OF_BUSINESS, lineOfBusiness);
            this.lineOfBusiness = lineOfBusiness;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set LOB.", e);
        }
    }

    private boolean setProperty(String propName, Object propValue)
        throws RRepositoryException {
        Node n = node();
        try {
            NodeUtil.smartCheckout(n, false);
            if (propValue instanceof Date) {
                n.setProperty(propName, ((Date) propValue).getTime());
            } else if (propValue instanceof Double) {
                n.setProperty(propName, (Double) propValue);
            } else {
                n.setProperty(propName, propValue.toString());
            }
            return true;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot set property "
                    + propName + ".", e);
        }
    }

    public void setAttribute1(String attribute1) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute1, attribute1)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE1, attribute1)) {
            this.attribute1 = attribute1;
        }
    }

    public void setAttribute2(String attribute2) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute2, attribute2)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE2, attribute2)) {
            this.attribute2 = attribute2;
        }
    }

    public void setAttribute3(String attribute3) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute3, attribute3)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE3, attribute3)) {
            this.attribute3 = attribute3;
        }
    }

    public void setAttribute4(String attribute4) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute4, attribute4)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE4, attribute4)) {
            this.attribute4 = attribute4;
        }
    }

    public void setAttribute5(String attribute5) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute5, attribute5)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE5, attribute5)) {
            this.attribute5 = attribute5;
        }
    }

    public void setAttribute6(Date attribute6) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute6, attribute6)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE6, attribute6)) {
            this.attribute6 = attribute6;
        }
    }

    public void setAttribute7(Date attribute7) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute7, attribute7)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE7, attribute7)) {
            this.attribute7 = attribute7;
        }
    }

    public void setAttribute8(Date attribute8) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute8, attribute8)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE8, attribute8)) {
            this.attribute8 = attribute8;
        }
    }

    public void setAttribute9(Date attribute9) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute9, attribute9)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE9, attribute9)) {
            this.attribute9 = attribute9;
        }
    }

    public void setAttribute10(Date attribute10) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute10, attribute10)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE10, attribute10)) {
            this.attribute10 = attribute10;
        }
    }

    public void setAttribute11(Double attribute11) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute11, attribute11)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE11, attribute11)) {
            this.attribute11 = attribute11;
        }
    }

    public void setAttribute12(Double attribute12) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute12, attribute12)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE12, attribute12)) {
            this.attribute12 = attribute12;
        }
    }

    public void setAttribute13(Double attribute13) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute13, attribute13)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE13, attribute13)) {
            this.attribute13 = attribute13;
        }
    }

    public void setAttribute14(Double attribute14) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute14, attribute14)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE14, attribute14)) {
            this.attribute14 = attribute14;
        }
    }

    public void setAttribute15(Double attribute15) throws RRepositoryException {
        // do not update JCR if property wasn't changed
        if (isSame(this.attribute15, attribute15)) return;
        if (setProperty(JcrNT.PROP_ATTRIBUTE15, attribute15)) {
            this.attribute15 = attribute15;
        }
    }

    // ------ private ------

    private void buildRelPath(StringBuilder sb, Node n) throws RepositoryException {
        if (!n.isNodeType(JcrNT.NT_PROJECT)) {
            buildRelPath(sb, n.getParent());
        }

        if (!n.isNodeType(JcrNT.NT_FILES)) {
            sb.append('/');
            sb.append(n.getName());
        }
    }

    private static final String[] ALLOWED_PROPS = {};
    private void initProperties() throws RepositoryException {
        properties.clear();

        Node n = node();
        for (String s : ALLOWED_PROPS) {
            if (n.hasProperty(s)) {
                Property p = n.getProperty(s);

                JcrProperty prop = new JcrProperty(this, p);
                properties.put(prop.getName(), prop);
            }
        }
    }
}
