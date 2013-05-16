package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.util.Date;

import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:openl-ruleservice-filesystemdatasource-beans.xml",
        "classpath:openl-ruleservice-filesystemdatasource-scheduler-beans.xml" })
public class FileSystemDataSourceListenerTest {
    private static final long SLEEP_TIME = 15000;
    private boolean flag = false;

    @Autowired
    private DataSource dataSource;

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Test
    public void fileSystemDataSourceListenerTest() {
        Assert.assertNotNull(dataSource);
        Assert.assertFalse(flag);
        DataSourceListener dataSourceListener = new DataSourceListener() {
            public void onDeploymentAdded() {
                flag = true;
            }
        };
        dataSource.addListener(dataSourceListener);
        String path = ((FileSystemDataSource) dataSource).getLoadDeploymentsFromDirectory();
        File loadDeploymentsFromFolder = new File(path);
        for (File file : loadDeploymentsFromFolder.listFiles()) {
            file.setLastModified(new Date().getTime());
        }
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
        }
        Assert.assertTrue(flag);
    }
}
