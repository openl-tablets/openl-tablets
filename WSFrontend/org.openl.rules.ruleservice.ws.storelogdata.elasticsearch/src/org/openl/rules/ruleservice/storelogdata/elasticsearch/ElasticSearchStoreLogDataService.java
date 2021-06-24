package org.openl.rules.ruleservice.storelogdata.elasticsearch;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.storelogdata.AbstractStoreLogDataService;
import org.openl.rules.ruleservice.storelogdata.Inject;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataException;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataMapper;
import org.openl.rules.ruleservice.storelogdata.annotation.AnnotationUtils;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.annotation.StoreLogDataToElasticsearch;
import org.openl.spring.config.ConditionalOnEnable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnEnable("ruleservice.store.logs.elasticsearch.enabled")
public class ElasticSearchStoreLogDataService extends AbstractStoreLogDataService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private final StoreLogDataMapper storeLogDataMapper = new StoreLogDataMapper();

    private volatile Collection<Inject<?>> supportedInjects;

    public ElasticsearchOperations getElasticsearchOperations() {
        return elasticsearchOperations;
    }

    public void setElasticsearchOperations(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public boolean isSync(StoreLogData storeLogData) {
        StoreLogDataToElasticsearch storeLogDataToElasticsearch = AnnotationUtils
            .getAnnotationInServiceClassOrServiceMethod(storeLogData, StoreLogDataToElasticsearch.class);
        if (storeLogDataToElasticsearch != null) {
            return storeLogDataToElasticsearch.sync();
        }
        return false;
    }

    @Override
    public Collection<Inject<?>> additionalInjects() {
        if (supportedInjects == null) {
            synchronized (this) {
                if (supportedInjects == null) {
                    Collection<Inject<?>> injects = new ArrayList<>();
                    injects.add(new Inject<>(InjectElasticsearchOperations.class, this::getElasticsearchOperations));
                    supportedInjects = Collections.unmodifiableCollection(injects);
                }
            }
        }
        return supportedInjects;
    }

    @Override
    protected void save(StoreLogData storeLogData, boolean sync) throws StoreLogDataException {
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
                        throw new StoreLogDataException(String.format(
                            "Failed to instantiate ElasticSearch index builder%s. Please, check that class '%s' is not abstract and has a default constructor.",
                            serviceMethod != null ? " for method '" + MethodUtil
                                .printQualifiedMethodName(serviceMethod) + "'" : StringUtils.EMPTY,
                            entityClass.getTypeName()), e);
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
                if (serviceMethod != null) {
                    throw new StoreLogDataException(
                        String.format("Failed to populate Elasticsearch index related to method '%s' in class '%s'.",
                            MethodUtil.printQualifiedMethodName(serviceMethod),
                            entity.getClass().getTypeName()),
                        e);
                } else {
                    throw new StoreLogDataException(
                        String.format("Failed to populate Elasticsearch index related to class '%s'.",
                            entity.getClass().getTypeName()),
                        e);
                }
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
                    throw new StoreLogDataException("Failed on ElasticSearch entity save operation.", e);
                }
            }
        }
    }

    private String extractId(Object entity) throws StoreLogDataException {
        String existingId = null;

        for (Field f : entity.getClass().getDeclaredFields()) {
            Id[] annotationsByType = f.getAnnotationsByType(Id.class);
            if (annotationsByType.length != 0) {
                try {
                    f.setAccessible(true);
                    existingId = (String) f.get(entity);
                } catch (IllegalAccessException e) {
                    throw new StoreLogDataException(
                        String.format("Failed on ElasticSearch entity '%s' extract ID operation.",
                            entity.getClass().getTypeName()),
                        e);
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
