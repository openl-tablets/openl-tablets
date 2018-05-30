package org.openl.rules.repository.db;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Settings {
    private final Logger log = LoggerFactory.getLogger(Settings.class);
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
    String selectLastChange;
    String copyFile;
    String copyHistory;

    Settings(String databaseCode, int major, int minor) throws IOException {
        TreeMap<String, String> queries = new TreeMap<String, String>();
        fillQueries(queries, "/openl-db-repository");
        fillQueries(queries, "/openl-db-repository-" + databaseCode);
        fillQueries(queries, "/openl-db-repository-" + databaseCode + "-v" + major);
        fillQueries(queries, "/openl-db-repository-" + databaseCode + "-v" + major + "." + minor);
        fillQueries(queries, "/openl-db-repository-ext"); // For customization purposes
        resolve(queries);

        timerPeriod = getIntValue(queries, "setting.timerPeriod", 10000);
        tableName = getRequired(queries, "setting.tablename");

        insertFile = getRequired(queries, "query.insert-new-file");
        copyFile = getRequired(queries, "query.copy-last-file");
        copyHistory = getRequired(queries, "query.copy-exact-file");
        deleteVersion = getRequired(queries, "query.delete-exact-file");
        deleteAllHistory = getRequired(queries, "query.delete-all-history");
        readActualFile = getRequired(queries, "query.read-last-file");
        readHistoricFile = getRequired(queries, "query.read-exact-file");
        readActualFileMetainfo = getRequired(queries, "query.read-last-metainfo");
        readHistoricFileMetainfo = getRequired(queries, "query.read-exact-metainfo");
        selectAllMetainfo = getRequired(queries, "query.list-last-metainfo");
        selectAllHistoryMetainfo = getRequired(queries, "query.list-all-metainfo");
        selectLastChange = getRequired(queries, "query.select-last-change");

        initStatements = queries.subMap("init.", "init." + Character.MAX_VALUE).values();
    }

    private void fillQueries(Map<String, String> queries, String propertiesFileName) throws IOException {
        URL resource = getClass().getResource(propertiesFileName + ".properties");
        if (resource == null) {
            log.info("Configuration file '{}.properties' is absent, so skipped.", propertiesFileName);
            return;
        }
        log.info("Load configuration from '{}'.", resource);
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

    private void resolve(TreeMap<String, String> queries) {
        Set<String> keys = queries.keySet();
        for (String key : keys) {
            String value = queries.get(key);
            if (value != null) {
                value = new StrSubstitutor(queries).replace(value);
                queries.put(key, value);
            }
        }
    }
}
