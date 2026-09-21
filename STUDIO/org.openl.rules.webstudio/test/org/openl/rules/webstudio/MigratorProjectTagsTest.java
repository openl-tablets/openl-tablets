package org.openl.rules.webstudio;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Tests of the version that decides whether the project tags are still to be migrated from the database.
 *
 * @author Yury Molchan
 */
class MigratorProjectTagsTest {

    @Test
    void anInstallationOf6_0_0IsNotTouched() {
        var applicationContext = mock(ApplicationContext.class);

        Migrator.migrateAfterContentInitialized(applicationContext, "6.0.0");

        verifyNoInteractions(applicationContext);
    }

    @Test
    void aNewerInstallationIsNotTouched() {
        var applicationContext = mock(ApplicationContext.class);

        Migrator.migrateAfterContentInitialized(applicationContext, "6.5.0");

        verifyNoInteractions(applicationContext);
    }

    @Test
    void anOlderInstallationIsMigrated() {
        var applicationContext = mock(ApplicationContext.class);
        // A context without a database ends the migration right after it has started.
        when(applicationContext.containsBean("openlDataSource")).thenReturn(false);

        Migrator.migrateAfterContentInitialized(applicationContext, "5.27.0");

        verify(applicationContext).containsBean("openlDataSource");
        verifyNoMoreInteractions(applicationContext);
    }
}
