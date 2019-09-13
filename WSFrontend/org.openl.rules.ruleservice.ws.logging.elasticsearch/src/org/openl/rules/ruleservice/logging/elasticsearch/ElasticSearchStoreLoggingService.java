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

    private boolean enabled = true;

    private ElasticsearchOperations elasticsearchOperations;

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
    public void save(StoreLoggingData storeLoggingData) {
        Method serviceMethod = storeLoggingData.getServiceMethod();
        if (serviceMethod == null) {
            log.error("Service method is not found! Please, see previous errors.");
            return;
        }

        IndexBuilder[] elasticsearchIndexBuilders = null;

        ElasticsearchIndexBuilder elasticsearchIndexBuilderAnnotation = serviceMethod
            .getAnnotation(ElasticsearchIndexBuilder.class);
        if (elasticsearchIndexBuilderAnnotation == null) {
            elasticsearchIndexBuilderAnnotation = serviceMethod.getDeclaringClass()
                .getAnnotation(ElasticsearchIndexBuilder.class);
        }
        if (elasticsearchIndexBuilderAnnotation == null || elasticsearchIndexBuilderAnnotation.value().length == 0) {
            elasticsearchIndexBuilders = new IndexBuilder[] { new DefaultIndexBuilderImpl() };
        } else {
            Class<? extends IndexBuilder>[] elasticSearchIndexBuilderClasses = elasticsearchIndexBuilderAnnotation
                .value();
            elasticsearchIndexBuilders = new IndexBuilder[elasticSearchIndexBuilderClasses.length];
            int i = 0;
            for (Class<? extends IndexBuilder> elasticSearchIndexBuilderClass : elasticSearchIndexBuilderClasses) {
                try {
                    elasticsearchIndexBuilders[i] = elasticSearchIndexBuilderClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format(
                            "Failed to instantiate ElasticSearch index builder for method '%s'. Please, check that '%s' class is not abstact and has a default constructor.",
                            MethodUtil.printQualifiedMethodName(serviceMethod),
                            elasticSearchIndexBuilderClass.getTypeName()), e);
                    }
                    return;
                }
                i++;
            }
        }

        IndexQuery[] indexQueries = new IndexQuery[elasticsearchIndexBuilders.length];
        int i = 0;
        for (IndexBuilder indexBuilder : elasticsearchIndexBuilders) {
            IndexQuery indexQuery = new IndexQueryBuilder().withIndexName(indexBuilder.withIndexName(storeLoggingData))
                .withType(indexBuilder.withType(storeLoggingData))
                .withId(indexBuilder.withId(storeLoggingData))
                .withObject(indexBuilder.withObject(storeLoggingData))
                .withVersion(indexBuilder.withVersion(storeLoggingData))
                .withSource(indexBuilder.withSource(storeLoggingData))
                .withParentId(indexBuilder.withParentId(storeLoggingData))
                .build();
            indexQueries[i++] = indexQuery;
        }
        for (IndexQuery indexQuery : indexQueries) {
            if (indexQuery != null) {
                try {
                    elasticsearchOperations.index(indexQuery);
                } catch (Exception e) {
                    // Continue the loop if exception occurs
                    log.error("Failed on ElasticSearch entity save operation.", e);
                }
            }
        }
    }

}
