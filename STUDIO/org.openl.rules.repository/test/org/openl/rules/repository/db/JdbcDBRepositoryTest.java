package org.openl.rules.repository.db;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.repository.RepositoryVerifier;
import org.openl.rules.repository.api.Repository;
import org.openl.spring.env.PropertySourcesLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {"repoID=jdbc", "openl.config.location=file:test-resources/application.properties"})
@ContextConfiguration(initializers = PropertySourcesLoader.class, locations = {"classpath:application.xml"})
public class JdbcDBRepositoryTest {

    @Autowired
    Repository repository;

    @Test
    public void test() throws Exception {
        RepositoryVerifier.testRepo(repository);
    }
}
