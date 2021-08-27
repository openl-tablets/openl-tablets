package org.openl.rules.repository.db;

import org.junit.Test;
import org.openl.rules.repository.RepositoryVerifier;


public class JdbcDBRepositoryTest {

    @Test
    public void test() throws Exception {
        JdbcDBRepository repository = new JdbcDBRepository();
        repository.setUri("jdbc:h2:mem:repo1;DB_CLOSE_DELAY=-1");
        repository.initialize();
        RepositoryVerifier.testRepo(repository);
    }
}
