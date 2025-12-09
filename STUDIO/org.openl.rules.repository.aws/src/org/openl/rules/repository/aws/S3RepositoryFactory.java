package org.openl.rules.repository.aws;

import java.util.function.Consumer;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.api.Repository;

/**
 * AWS S3 repository factory.
 *
 * @author Yury Molchan
 */
public class S3RepositoryFactory implements RepositoryFactory {
    private static final String ID = "repo-aws-s3";
    private static final String OLD_ID = "org.openl.rules.repository.aws.S3Repository";

    @Override
    public boolean accept(String factoryID) {
        return factoryID.equals(ID) || factoryID.equals(OLD_ID);
    }

    @Override
    public String getRefID() {
        return ID;
    }

    @Override
    public Repository create(Consumer<Repository> initParamsCallback) {
        S3Repository repository = new S3Repository();
        initParamsCallback.accept(repository);
        repository.initialize();
        return repository;
    }
}
