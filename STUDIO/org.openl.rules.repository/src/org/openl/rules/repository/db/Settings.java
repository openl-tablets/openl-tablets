package org.openl.rules.repository.db;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Settings {
    private final Logger log = LoggerFactory.getLogger(Settings.class);

    Settings(String databaseCode) throws IOException {
        TreeMap<String, String> queries = new TreeMap<String, String>();
        fillQueries(queries, "/openl-db-repository.properties");
        fillQueries(queries, "/openl-db-repository-" + databaseCode + ".properties");
        fillQueries(queries, "/openl-db-repository-ext.properties");

        timerPeriod = getIntValue(queries, "setting.timerPeriod", 10000);
        tableName = getRequired(queries, "query.tablename");

        selectAllMetainfo = getRequired(queries, "query.select-all-metainfo");
        selectAllHistoryMetainfo = getRequired(queries, "query.select-all-history-metainfo");
        insertFile = getRequired(queries, "query.insert-file");
        readActualFile = getRequired(queries, "query.read-actual-file");
        readActualFileMetainfo = getRequired(queries, "query.read-actual-file-metainfo");
        readHistoricFile = getRequired(queries, "query.read-historic-file");
        readHistoricFileMetainfo = getRequired(queries, "query.read-historic-file-metainfo");
        deleteAllHistory = getRequired(queries, "query.delete-all-history");
        deleteVersion = getRequired(queries, "query.delete-version");
        selectMaxId = getRequired(queries, "query.select-max-id");
        copyFile = getRequired(queries, "query.copy-file");
        copyHistory = getRequired(queries, "query.copy-history");

        initStatements = queries.subMap("init.", "init." + Character.MAX_VALUE).values();
    }

    int timerPeriod;
    String tableName;
    Collection<String> initStatements;

    String selectAllMetainfo;
    String selectAllHistoryMetainfo;
    String insertFile;
    String readActualFile;
    String readActualFileMetainfo;
    String readHistoricFile;
    String readHistoricFileMetainfo;
    String deleteAllHistory;
    String deleteVersion;
    String selectMaxId;
    String copyFile;
    String copyHistory;

    private void fillQueries(Map<String, String> queries, String propertiesFileName) throws IOException {
        URL resource = getClass().getResource(propertiesFileName);
        if (resource == null) {
            log.info("File [{}] not found.", propertiesFileName);
            return;
        }
        log.info("Load configuration from [{}].", resource);
        InputStream is = resource.openStream();
        try {
            Properties properties = new Properties();
            properties.load(is);
            for (String key : properties.stringPropertyNames()) {
                queries.put(key, properties.getProperty(key));
            }
            is.close();
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private int getIntValue(Map<String, String> queries, String prop, int defValue) {
        String stringValue = queries.get(prop);
        int value = defValue;
        if (stringValue != null) {
            try {
                value = Integer.parseInt(stringValue);
            } catch (Exception e) {
                log.warn("Cannot parse value from {} = {}! Default value is used.", prop, stringValue, e);
            }
        }
        return value;
    }

    private String getRequired(Map<String, String> queries, String prop) {
        String value = queries.get(prop);
        if (value == null) {
            throw new IllegalArgumentException("Cannot get value for " + prop + " property.");
        }
        return value;
    }
}
