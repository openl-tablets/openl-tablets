package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.DependsOn;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:UnpackClasspathJarToDirectoryBeanTest.xml" })
@DependsOn(value = { "unpackClasspathJarToDirectoryBean" })
public class UnpackClasspathJarToDirectoryBeanTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private static final String UNPACK_CLASSPATH_JAR_TO_DIRECTORY_BEAN_TEST_DIRECTORY = "target/unpackClasspathJarToDirectoryBeanTest";

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Before
    public void before() {
        FolderHelper.clearFolder(new File(UNPACK_CLASSPATH_JAR_TO_DIRECTORY_BEAN_TEST_DIRECTORY));
    }

    private boolean isFolderHasElements(String folderPath) throws IOException {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            throw new IOException("Folder doesn't exist. Path: " + folderPath);
        }

        if (!folder.isDirectory()) {
            throw new IOException("Path isn't a directory on the file system. Path: " + folderPath);
        }

        return folder.listFiles().length > 0;
    }

    @Test
    public void test() throws Exception {
        Assert.assertNotNull(applicationContext);
        UnpackClasspathJarToDirectoryBean bean = applicationContext.getBean(UnpackClasspathJarToDirectoryBean.class);
        Assert.assertNotNull(bean);
        Assert.assertFalse(isFolderHasElements(UNPACK_CLASSPATH_JAR_TO_DIRECTORY_BEAN_TEST_DIRECTORY));
        bean.afterPropertiesSet();
        Assert.assertTrue(isFolderHasElements(UNPACK_CLASSPATH_JAR_TO_DIRECTORY_BEAN_TEST_DIRECTORY));
    }
}
