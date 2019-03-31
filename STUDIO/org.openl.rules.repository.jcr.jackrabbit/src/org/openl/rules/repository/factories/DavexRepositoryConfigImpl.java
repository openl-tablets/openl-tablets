package org.openl.rules.repository.factories;

import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi2davex.RepositoryServiceImpl;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;

/**
 * from jackarabbit sandbox
 */
public class DavexRepositoryConfigImpl implements RepositoryConfig {

    public static final int DEFAULT_ITEM_CACHE_SIZE = 5000;
    private static final int POLL_TIMEOUT = 1000; // ms

    private final CacheBehaviour cacheBehaviour;
    private final int itemCacheSize;

    private final RepositoryService service;

    public DavexRepositoryConfigImpl(String uri) throws RepositoryException {
        this(uri, CacheBehaviour.INVALIDATE, DEFAULT_ITEM_CACHE_SIZE);
    }

    public DavexRepositoryConfigImpl(String uri, CacheBehaviour cacheBehaviour, int itemCacheSize)
            throws RepositoryException {
        this.cacheBehaviour = cacheBehaviour;
        this.itemCacheSize = itemCacheSize;
        service = createService(uri);
    }

    @Override
    public CacheBehaviour getCacheBehaviour() {
        return cacheBehaviour;
    }

    @Override
    public int getItemCacheSize() {
        return itemCacheSize;
    }

    @Override
    public int getPollTimeout() {
        return POLL_TIMEOUT;
    }

    private static RepositoryService createService(String uri) throws RepositoryException {
        BatchReadConfig brc = new BatchReadConfig() {
            @Override
            public int getDepth(Path path, PathResolver pathResolver) throws NamespaceException {
                return 4;
            }
        };
        return new RepositoryServiceImpl(uri, brc);
    }

    @Override
    public RepositoryService getRepositoryService() throws RepositoryException {
        return service;
    }
}