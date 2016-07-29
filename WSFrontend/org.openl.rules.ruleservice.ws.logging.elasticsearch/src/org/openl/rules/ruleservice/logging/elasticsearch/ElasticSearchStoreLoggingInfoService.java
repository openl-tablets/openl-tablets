package org.openl.rules.ruleservice.logging.elasticsearch;

import java.lang.reflect.Method;

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.openl.rules.ruleservice.logging.LoggingInfo;
import org.openl.rules.ruleservice.logging.StoreLoggingInfoService;
import org.openl.rules.ruleservice.logging.elasticsearch.annotation.UseElasticSearchIndexBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

public class ElasticSearchStoreLoggingInfoService implements StoreLoggingInfoService {

    private final Logger log = LoggerFactory.getLogger(ElasticSearchStoreLoggingInfoService.class);

    private ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchOperations getElasticSearchOperations() {
        return elasticsearchOperations;
    }

    public ElasticsearchOperations getElasticsearchOperations() {
        return elasticsearchOperations;
    }

    public void setElasticsearchOperations(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public void store(LoggingInfo loggingInfo) {
        OperationResourceInfo operationResourceInfo = loggingInfo.getOperationResourceInfo();
        if (operationResourceInfo == null) {
            log.error("Operation wasn't found. Logging skipped! Please, see previous errors.");
            return;
        }
        Method annotatedMethod = operationResourceInfo.getAnnotatedMethod();
        ElasticSearchIndexBuilder elasticSearchIndexBuilder = null;

        UseElasticSearchIndexBuilder useElasticSearchIndexBuilderAnnotation = annotatedMethod
            .getAnnotation(UseElasticSearchIndexBuilder.class);
        if (useElasticSearchIndexBuilderAnnotation == null) {
            useElasticSearchIndexBuilderAnnotation = annotatedMethod.getDeclaringClass()
                .getAnnotation(UseElasticSearchIndexBuilder.class);
        }
        if (useElasticSearchIndexBuilderAnnotation == null) {
            elasticSearchIndexBuilder = new DefaultElasticSearchIndexBuilderImpl();
        } else {
            try {
                Class<? extends ElasticSearchIndexBuilder> elasticSearchIndexBuilderClass = useElasticSearchIndexBuilderAnnotation
                    .value();
                elasticSearchIndexBuilder = elasticSearchIndexBuilderClass.newInstance();
            } catch (InstantiationException e) {
                elasticSearchIndexBuilder = new DefaultElasticSearchIndexBuilderImpl();
                if (log.isErrorEnabled()) {
                    log.error(
                        "Loading CustomLoggingElasticSearchIndexBuilder annotation was failed for method " + annotatedMethod
                            .getName() + ". Used default implementation instead!");
                }
            } catch (IllegalAccessException e) {
                elasticSearchIndexBuilder = new DefaultElasticSearchIndexBuilderImpl();
                if (log.isErrorEnabled()) {
                    log.error(
                        "Loading CustomLoggingElasticSearchIndexBuilder annotation was failed for method " + annotatedMethod
                            .getName() + ". Used default implementation instead!");
                }
            }
        }

        IndexQuery indexQuery = new IndexQueryBuilder()
            .withIndexName(elasticSearchIndexBuilder.withIndexName(loggingInfo))
            .withType(elasticSearchIndexBuilder.withType(loggingInfo))
            .withId(elasticSearchIndexBuilder.withId(loggingInfo))
            .withObject(elasticSearchIndexBuilder.withObject(loggingInfo))
            .withVersion(elasticSearchIndexBuilder.withVersion(loggingInfo))
            .withSource(elasticSearchIndexBuilder.withSource(loggingInfo))
            .withParentId(elasticSearchIndexBuilder.withParentId(loggingInfo))
            .build();

        elasticsearchOperations.index(indexQuery);
        
    }

}
