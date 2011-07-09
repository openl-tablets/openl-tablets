package org.openl.ruleservice.publish.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class WebServicesExposingTest {
    private static final String TEST_REPOSITORY_PATH = "./test-resources/production-repository/";
    private static final String TEST_REPOSITORY_ZIP = "./test-resources/production-repository.zip";

    private static void before() throws Exception {
        unzipArchive(new File(TEST_REPOSITORY_ZIP), new File(TEST_REPOSITORY_PATH));
    }

    public static void main(String[] args) throws Exception {
        before();
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:test-ruleservice-beans.xml");
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        serviceManager.start();
        System.out.print("Press enter for server stop:");
        System.in.read();
        System.out.println("Server stoped");
        System.exit(0);
    }

    public static void unzipArchive(File archive, File outputDir) throws Exception {
        ZipFile zipfile = new ZipFile(archive);
        Enumeration<? extends ZipEntry> e = zipfile.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            unzipEntry(zipfile, entry, outputDir);
        }
    }

    private static void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir) throws IOException {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }
        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }

        BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

        try {
            IOUtils.copy(inputStream, outputStream);
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

    private static void createDir(File dir) {
        if (dir.exists()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (Exception e) {
                new RuntimeException("Can't delete dir " + dir);
            }
        }
        if (!dir.mkdirs())
            throw new RuntimeException("Can not create dir " + dir);
    }

}
