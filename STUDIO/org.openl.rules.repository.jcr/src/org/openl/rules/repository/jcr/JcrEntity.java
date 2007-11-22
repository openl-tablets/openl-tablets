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

        properties = new HashMap<String, RProperty>();
        initProperties();
    }

    // ------ protected methods ------

    public String getPath() throws RRepositoryException {
        StringBuilder sb = new StringBuilder(128);
        try {
            buildRelPath(sb, node());
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get relative path", e);
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
            throw new RRepositoryException("Internal error", e);
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
            throw new RRepositoryException("No such property {0}", null, name);
        }

        Node n = node();
        try {
            NodeUtil.smartCheckout(n, false);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Internal error", e);
        }

        try {
            n.getProperty(name).remove();
        } catch (RepositoryException e) {
            throw new RRepositoryException("Cannot remove property {0}", e, name);
        }
        properties.remove(name);
    }

    public boolean hasProperty(String name) {
        return (properties.get(name) != null);
    }

    public RProperty getProperty(String name) throws RRepositoryException {
        RProperty rp = properties.get(name);

        if (rp == null) {
            throw new RRepositoryException("No such property {0}", null, name);
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
            throw new RRepositoryException("Cannot set effectiveDate", e);
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
            throw new RRepositoryException("Cannot set expirationDate", e);
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
            throw new RRepositoryException("Cannot set LOB", e);
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

    private Calendar convertDate2Calendar(Date date) {
        if (date == null) return null;
        
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c;
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
    
    private boolean isSame(Object o1, Object o2) {
        // both are null (the same)
        if (o1 == null && o2 == null) return true;
        // at least one is null (other is not)
        if (o1 == null || o2 == null) return false;
        // equals or not?
        return o1.equals(o2);
    }
}
