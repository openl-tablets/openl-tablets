package org.openl.rules.ruleservice.logging.elasticsearch;

import java.lang.reflect.Method;

import org.openl.binding.MethodUtil;
import org.openl.rules.ruleservice.logging.StoreLoggingData;
import org.openl.rules.ruleservice.logging.StoreLoggingService;
import org.openl.rules.ruleservice.logging.elasticsearch.annotation.ElasticsearchIndexBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

public class ElasticSearchStoreLoggingService implements StoreLoggingService {

    private final Logger log = LoggerFactory.getLogger(ElasticSearchStoreLoggingService.class);

    private ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchOperations getElasticsearchOperations() {
        return elasticsearchOperations;
    }

    public void setElasticsearchOperations(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public void store(StoreLoggingData storeLoggingData) {
        Method serviceMethod = storeLoggingData.getServiceMethod();
        if (serviceMethod == null) {
            log.error("Service method is not found! Please, see previous errors.");
            return;
        }

        IndexBuilder elasticsearchIndexBuilder = null;

        ElasticsearchIndexBuilder elasticsearchIndexBuilderAnnotation = serviceMethod
            .getAnnotation(ElasticsearchIndexBuilder.class);
        if (elasticsearchIndexBuilderAnnotation == null) {
            elasticsearchIndexBuilderAnnotation = serviceMethod.getDeclaringClass()
                .getAnnotation(ElasticsearchIndexBuilder.class);
        }
        if (elasticsearchIndexBuilderAnnotation == null) {
            elasticsearchIndexBuilder = new DefaultIndexBuilderImpl();
        } else {
            try {
                Class<? extends IndexBuilder> elasticSearchIndexBuilderClass = elasticsearchIndexBuilderAnnotation
                    .value();
                elasticsearchIndexBuilder = elasticSearchIndexBuilderClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                elasticsearchIndexBuilder = new DefaultIndexBuilderImpl();
                if (log.isErrorEnabled()) {
                    log.error(String.format(
                        "Failed to instantiate ElasticSearch index builder for method '%s'. Please, check that '%s' class is not abstact and has a default constructor. Default index builder is used instead.",
                        MethodUtil.printQualifiedMethodName(serviceMethod),
                        elasticsearchIndexBuilderAnnotation.value().getTypeName()), e);
                }
            }
        }

        IndexQuery indexQuery = new IndexQueryBuilder()
            .withIndexName(elasticsearchIndexBuilder.withIndexName(storeLoggingData))
            .withType(elasticsearchIndexBuilder.withType(storeLoggingData))
            .withId(elasticsearchIndexBuilder.withId(storeLoggingData))
            .withObject(elasticsearchIndexBuilder.withObject(storeLoggingData))
            .withVersion(elasticsearchIndexBuilder.withVersion(storeLoggingData))
            .withSource(elasticsearchIndexBuilder.withSource(storeLoggingData))
            .withParentId(elasticsearchIndexBuilder.withParentId(storeLoggingData))
            .build();

        elasticsearchOperations.index(indexQuery);

    }

}
