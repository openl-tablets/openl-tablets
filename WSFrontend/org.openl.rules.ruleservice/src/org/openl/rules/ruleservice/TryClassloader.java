package org.openl.rules.ruleservice;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class TryClassloader {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        File base = new File("/tmp");
        String path = new File(base, "sample\\f\\").getCanonicalPath();

        URL testUrl = new URL("file", null, path);
        System.out.println(testUrl);


        URL url = new URL("file:E:\\Projects\\SVN\\STUDIO\\org.openl.rules.ruleservice\\sample\\f/");

        URLClassLoader loader = new URLClassLoader(new URL[]{url});
        Class<?> aClass = loader.loadClass("Bla");
        System.out.println(aClass.getName());

    }
}
