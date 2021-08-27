package org.openl.rules.repository.db;

import org.junit.Test;
import org.openl.rules.repository.RepositoryVerifier;

public class JndiDBRepositoryTest {

    @Test
    public void test() throws Exception {
        DatasourceDBRepository repository = new DatasourceDBRepository();
        repository.setUri("java:/comp/env/datasource/ds");
        repository.initialize();
        RepositoryVerifier.testRepo(repository);
    }
}
