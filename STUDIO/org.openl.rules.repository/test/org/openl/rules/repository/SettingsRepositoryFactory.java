package org.openl.rules.repository;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.openl.rules.repository.api.Repository;

/**
 * A repository type for tests only, which remembers the settings of the files kept apart that its repository is
 * given.
 *
 * @author Yury Molchan
 */
public class SettingsRepositoryFactory implements RepositoryFactory {

    static final Map<String, String> GIVEN = new HashMap<>();

    @Override
    public boolean accept(String factoryID) {
        return getRefID().equals(factoryID);
    }

    @Override
    public String getRefID() {
        return "repo-settings-test-only";
    }

    @Override
    public Repository create(UnaryOperator<String> settings) {
        GIVEN.clear();
        for (var key : new String[]{"ignored-paths", "protected-files"}) {
            GIVEN.put(key, settings.apply(key));
        }
        return mock(Repository.class);
    }
}
