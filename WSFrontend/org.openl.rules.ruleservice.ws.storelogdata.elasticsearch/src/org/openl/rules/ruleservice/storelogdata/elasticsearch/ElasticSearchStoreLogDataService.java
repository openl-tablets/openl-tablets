package org.openl.rules.ruleservice.storelogdata.elasticsearch;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataService;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.annotation.StoreLogDataToElasticsearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

public class ElasticSearchStoreLogDataService implements StoreLogDataService {

    private final Logger log = LoggerFactory.getLogger(ElasticSearchStoreLogDataService.class);

    private boolean enabled = true;

    private ElasticsearchOperations elasticsearchOperations;

    private StoreLogDataMapper storeLogDataMapper = new StoreLogDataMapper();

    public ElasticsearchOperations getElasticsearchOperations() {
        return elasticsearchOperations;
    }

    public void setElasticsearchOperations(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void save(StoreLogData storeLogData) {
        Object[] entities;

        StoreLogDataToElasticsearch storeLogDataToElasticsearchAnnotation = storeLogData.getServiceClass()
            .getAnnotation(StoreLogDataToElasticsearch.class);

        Method serviceMethod = storeLogData.getServiceMethod();
        if (serviceMethod != null && serviceMethod.isAnnotationPresent(StoreLogDataToElasticsearch.class)) {
            storeLogDataToElasticsearchAnnotation = serviceMethod.getAnnotation(StoreLogDataToElasticsearch.class);
        }
        if (storeLogDataToElasticsearchAnnotation == null) {
            return;
        }

        if (storeLogDataToElasticsearchAnnotation.value().length == 0) {
            entities = new DefaultElasticEntity[] { new DefaultElasticEntity() };
        } else {
            entities = new Object[storeLogDataToElasticsearchAnnotation.value().length];
            int i = 0;
            for (Class<?> entityClass : storeLogDataToElasticsearchAnnotation.value()) {
                if (StoreLogDataToElasticsearch.DEFAULT.class == entityClass) {
                    entities[i] = new DefaultElasticEntity();
                } else {
                    try {
                        entities[i] = entityClass.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        if (log.isErrorEnabled()) {
                            log.error(String.format(
                                "Failed to instantiate ElasticSearch index builder%s. Please, check that class '%s' is not abstract and has a default constructor.",
                                serviceMethod != null ? " for method '" + MethodUtil
                                    .printQualifiedMethodName(serviceMethod) + "'" : StringUtils.EMPTY,
                                entityClass.getTypeName()), e);
                        }
                        return;
                    }
                }
                i++;
            }
        }

        IndexQuery[] indexQueries = new IndexQuery[entities.length];
        int i = 0;

        for (Object entity : entities) {
            try {
                storeLogDataMapper.map(storeLogData, entity);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    if (serviceMethod != null) {
                        log.error(String.format("Failed to map '%s' Elasticsearch index for method '%s'.",
                            entity.getClass().getTypeName(),
                            MethodUtil.printQualifiedMethodName(serviceMethod)), e);
                    } else {
                        log.error(String.format("Failed to map '%s'.", entity.getClass().getTypeName()), e);
                    }
                }
                return;
            }
        }

        for (Object entity : entities) {
            Class<?> clazz = entity.getClass();
            IndexQuery indexQuery = new IndexQueryBuilder().withIndexName(extractIndexName(clazz))
                .withType(null)
                .withId(extractId(entity))
                .withObject(entity)
                .withVersion(null)
                .withSource(null)
                .withParentId(null)
                .build();
            indexQueries[i++] = indexQuery;
        }
        for (IndexQuery indexQuery : indexQueries) {
            if (indexQuery != null) {
                try {
                    elasticsearchOperations.index(indexQuery);
                    elasticsearchOperations.refresh(indexQuery.getIndexName());
                } catch (Exception e) {
                    // Continue the loop if exception occurs
                    log.error("Failed on ElasticSearch entity save operation.", e);
                }
            }
        }
    }

    private String extractId(Object entity) {
        String existingId = null;

        for (Field f : entity.getClass().getDeclaredFields()) {
            Id[] annotationsByType = f.getAnnotationsByType(Id.class);
            if (annotationsByType != null && annotationsByType.length != 0) {
                try {
                    f.setAccessible(true);
                    existingId = (String) f.get(entity);
                } catch (IllegalAccessException e) {
                    log.error("Failed on ElasticSearch entity extract ID operation.", e);
                }
            }
        }
        if (existingId == null) {
            existingId = UUID.randomUUID().toString();
        }
        return existingId;
    }

    private String extractIndexName(Class<?> clazz) {
        String indexName = clazz.getAnnotation(Document.class).indexName();
        try {
            return URLEncoder.encode(indexName, "UTF-8").toLowerCase();
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

}
