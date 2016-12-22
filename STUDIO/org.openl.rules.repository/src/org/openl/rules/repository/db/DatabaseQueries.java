package org.openl.rules.repository.db;

final class DatabaseQueries {
    static final String REPOSITORY_NAME = "openl_repository";
    static final String INIT_PREFIX = "init.";

    static final String SELECT_ALL_METAINFO = "query.select-all-metainfo";
    static final String SELECT_ALL_HISTORY_METAINFO = "query.select-all-history-metainfo";
    static final String INSERT_FILE = "query.insert-file";
    static final String READ_ACTUAL_FILE = "query.read-actual-file";
    static final String READ_ACTUAL_FILE_METAINFO = "query.read-actual-file-metainfo";
    static final String READ_HISTORIC_FILE = "query.read-historic-file";
    static final String READ_HISTORIC_FILE_METAINFO = "query.read-historic-file-metainfo";
    static final String DELETE_ALL_HISTORY = "query.delete-all-history";
    static final String DELETE_VERSION = "query.delete-version";
    static final String SELECT_MAX_ID = "query.select-max-id";
    static final String COPY_FILE = "query.copy-file";
    static final String COPY_HISTORY = "query.copy-history";
}
