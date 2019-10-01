package org.openl.rules.ruleservice.storelogdata.elasticsearch;

import java.lang.reflect.Method;

import org.openl.binding.MethodUtil;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.storelogdata.StoreLogData;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataService;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.IndexBuilder;
import org.openl.rules.ruleservice.storelogdata.elasticsearch.annotation.ElasticsearchIndexBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

public class ElasticSearchStoreLogDataService implements StoreLogDataService {

    private final Logger log = LoggerFactory.getLogger(ElasticSearchStoreLogDataService.class);

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
    public void save(StoreLogData storeLogData) {
        if (!storeLogData.getLoggingStorages().contains(RulesDeploy.LogStorageType.ELASTICSEARCH.toString())) {
            return;
        }

        Method serviceMethod = storeLogData.getServiceMethod();

        IndexBuilder[] elasticsearchIndexBuilders = null;
        ElasticsearchIndexBuilder elasticsearchIndexBuilderAnnotation = null;
        if (serviceMethod != null) {
            elasticsearchIndexBuilderAnnotation = serviceMethod.getAnnotation(ElasticsearchIndexBuilder.class);
            if (elasticsearchIndexBuilderAnnotation == null) {
                elasticsearchIndexBuilderAnnotation = serviceMethod.getDeclaringClass()
                    .getAnnotation(ElasticsearchIndexBuilder.class);
            }
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
            IndexQuery indexQuery = new IndexQueryBuilder().withIndexName(indexBuilder.withIndexName(storeLogData))
                .withType(indexBuilder.withType(storeLogData))
                .withId(indexBuilder.withId(storeLogData))
                .withObject(indexBuilder.withObject(storeLogData))
                .withVersion(indexBuilder.withVersion(storeLogData))
                .withSource(indexBuilder.withSource(storeLogData))
                .withParentId(indexBuilder.withParentId(storeLogData))
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
