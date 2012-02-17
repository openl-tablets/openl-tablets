package org.openl.rules.repository.jcr;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class JcrOldFile extends JcrOldEntity implements RFile {
    private static final Log log = LogFactory.getLog(JcrOldFile.class);

    public JcrOldFile(JcrOldEntity parent, String name, Node node) throws RepositoryException {
        super(parent, name, node);
        checkNodeType(JcrNT.NT_FILE);
    }

    public InputStream getContent() throws RRepositoryException {
        try {
            Node n = node();
            Node resNode = n.getNode("jcr:content");
            InputStream result = resNode.getProperty("jcr:data").getStream();

            return result;
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Content.", e);
        }
    }

    public InputStream getContent4Version(CommonVersion version) throws RRepositoryException {
        throw new RRepositoryException("In versioned mode can work with one version only!", null);
    }

    public String getMimeType() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getSize() {
        long result;

        try {
            Node n = node();
            Node resNode = n.getNode("jcr:content");
            result = resNode.getProperty("jcr:data").getLength();
        } catch (RepositoryException e) {
            log.info("getSize", e);
            result = -1;
        }

        return result;
    }

    public void revertToVersion(String versionName) throws RRepositoryException {
        notSupported();
    }

    public void setContent(InputStream inputStream) throws RRepositoryException {
        notSupported();
    }

}
