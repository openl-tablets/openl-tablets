package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties={"ruleservice.datasource.dir=test-resources/FileSystemDataSourceListenerTest"})
@ContextConfiguration({ "classpath:properties.xml", "classpath:openl-ruleservice-datasource-beans.xml", "classpath:openl-ruleservice-loader-beans.xml", "classpath:openl-ruleservice-filesystemdatasource-scheduler-beans.xml" })
public class FileSystemDataSourceListenerTest {
    private static final long SLEEP_TIME = 15000;
    private boolean flag = false;

    @Qualifier("datasource") @Autowired
    private DataSource dataSource;

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Test
    public void fileSystemDataSourceListenerTest() throws IOException {
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
            if (file.isDirectory() && file.getName().equals("non-openl-project")) {
                for (File f : file.listFiles()) {
                    f.setLastModified(new Date().getTime());
                }
            }
        }
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
        }
        Assert.assertTrue(flag);
        File createdFile = null;
        try {
            flag = false;
            for (File file : loadDeploymentsFromFolder.listFiles()) {
                if (file.isDirectory() && file.getName().equals("non-openl-project")) {
                    createdFile = new File(file, "test.test");
                    createdFile.createNewFile();
                }
            }
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
            }
            Assert.assertTrue(flag);
            flag = false;
            if (createdFile != null) {
                createdFile.delete();
                createdFile = null;
            }
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
            }
            Assert.assertTrue(flag);
        } finally {
            if (createdFile != null) {
                createdFile.delete();
            }
        }
        Assert.assertTrue(flag);
    }
}
