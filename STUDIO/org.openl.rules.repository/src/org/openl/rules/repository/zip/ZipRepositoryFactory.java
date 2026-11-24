package org.openl.rules.repository.zip;

import java.util.function.UnaryOperator;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;
import org.openl.util.StringUtils;

/**
 * Zipped local file system repository factory.
 *
 * @author Yury Molchan
 */
public class ZipRepositoryFactory implements RepositoryFactory {
    private static final String ID = "repo-zip";

    @Override
    public boolean accept(String factoryID) {
        return factoryID.equals(ID);
    }

    @Override
    public String getRefID() {
        return ID;
    }

    @Override
    public Repository create(UnaryOperator<String> settings) {
        var uri = settings.apply("uri");
        var archives = settings.apply("archives");
        var repository = new ZippedLocalRepository(StringUtils.isBlank(uri) ? null : uri,
                archives == null ? new String[0] : archives.split(","));
        RepositoryInstatiator.setParams(repository, settings);
        return repository;
    }
}
