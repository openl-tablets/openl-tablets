package org.openl.rules.repository.factories;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.RProductionRepository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.jcr.JcrProductionRepository;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class LocalJackrabbitProductionRepositoryFactory extends LocalJackrabbitRepositoryFactory {
    private static Log LOG = LogFactory.getLog(LocalJackrabbitProductionRepositoryFactory.class);
    /**
     * {@inheritDoc}
     */
    @Override
    public RProductionRepository getRepositoryInstance() throws RRepositoryException {
        try {
            if(convert){
                convert();
                convert = false;
            }
            // FIXME: do not hardcode credential info
            Session session = createSession("user", "pass");

            return new JcrProductionRepository(repositoryName, session);
        } catch (RepositoryException e) {
            throw new RRepositoryException("Failed to get Repository Instance", e);
        }
    }

    protected void convert() throws RRepositoryException{
        RProductionRepository repositoryInstance = null;
        String tempRepoHome = "/temp/prod_repo/";
        try {
            // FIXME: do not hardcode credential info
            Session session = createSession("user", "pass");

            repositoryInstance = new JcrProductionRepository(repositoryName, session);
            //FIXME
            ProductionRepositoryConvertor repositoryConvertor = new ProductionRepositoryConvertor(tempRepoHome);
            LOG.info("Converting production repository. Please, be patient.");
            repositoryConvertor.convert(repositoryInstance);
        } catch (Exception e) {
            throw new RRepositoryException("Failed to convert repository.", e);
        } finally {
            if (repositoryInstance != null) {
                repositoryInstance.release();
            }
        }
        File repoHome = new File(repHome);
        File tmpRepoHome = new File(tempRepoHome);
        try {
            FileUtils.deleteDirectory(repoHome);
            FileUtils.copyDirectory(tmpRepoHome, repoHome);
        } catch (IOException e) {
            throw new RRepositoryException("Failed to convert repository.", e);
        } finally {
            FileUtils.deleteQuietly(tmpRepoHome);
        }
        return;
        
    }
}
