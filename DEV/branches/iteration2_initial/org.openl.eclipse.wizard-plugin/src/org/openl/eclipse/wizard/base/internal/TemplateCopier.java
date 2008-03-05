package org.openl.eclipse.wizard.base.internal;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CancellationException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

public class TemplateCopier {
    private IProject project;
    private String templateLocation;
    private Map<String, String> replaces;

    public void setProject(IProject project) {
        this.project = project;
    }

    public void setTemplateLocation(String templateLocation) {
        this.templateLocation = templateLocation;

        replaces = new HashMap<String, String>();
    }

    public void setReplaces(Properties props) {
        for (Object k : props.keySet()) {
            String key = k.toString();
            String value = props.getProperty(key);

            addReplace(key, value);
        }
    }

    public void addReplace(String key, String value) {
        replaces.put("@" + key + "@", value);
    }

    public void copy(IProgressMonitor monitor) throws CoreException {
        monitor.beginTask("Copying...", 3);

        File templateRoot = new File(templateLocation);
        if (templateRoot.exists() && templateRoot.isDirectory()) {
            copyFiles(project, templateRoot, monitor);
            monitor.worked(1);

            String projectName = project.getName();
            // copy "Generate Template Wrapper.launch" -> "Generate @project.name@ Wrapper.launch"
            copyLaunch("Generate " + projectName + " Wrapper.launch", new File(templateRoot,
                    "Generate Template Wrapper.launch"));
            monitor.worked(1);

            // copy "WebStudio Starter.launch" -> *.*
            copyLaunch("WebStudio Starter.launch", new File(templateRoot, "WebStudio Starter.launch"));
            monitor.worked(1);
        } else {
            // invalid template folder
            System.out.println("ERROR: invalid template folder: " + templateRoot.getPath());
        }

        monitor.done();
    }

    protected void copyFiles(IContainer dest, File srcFolder, IProgressMonitor monitor) throws CoreException {
        File[] files = srcFolder.listFiles();
        if (files == null) return;

        SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
        subMonitor.beginTask(null, files.length + 1);

        for (File file : files) {
            if (monitor.isCanceled()) throw new CancellationException();
            if (!isInclude(file)) continue;

            Path destPath = new Path(file.getName());

            if (file.isDirectory()) {
                // folder
                IFolder destFolder = dest.getFolder(destPath);

                if (!destFolder.exists()) {
                    destFolder.create(true, true, null);
                }

                copyFiles(destFolder, file, monitor);
            } else {
                // file
                IFile destFile = dest.getFile(destPath);

                copyFile(destFile, file);
            }

            subMonitor.worked(1);
        }

        subMonitor.done();
    }

    protected void copyLaunch(String launchName, File srcFile) throws CoreException {
        if (srcFile.exists()) {
            Path destPath = new Path(launchName);
            IFile destFile = project.getFile(destPath);
            copyFile(destFile, srcFile);
        } else {
            // no such file
            System.out.println("ERROR: Cannot find template file: " + srcFile.getPath());
        }
    }

    protected void copyFile(IFile destFile, File srcFile) throws CoreException {
        try {
            InputStream content;

            if (isReplaceable(destFile)) {
                System.out.println("copy & replace: " + srcFile.getPath() + " -> " + destFile.getLocation().toString());
                content = replaceContent(srcFile);
            } else {
                System.out.println("copy: " + srcFile.getPath() + " -> " + destFile.getLocation().toString());
                content = new FileInputStream(srcFile);
            }

            destFile.create(content, false, null);
        } catch (FileNotFoundException e) {
            throw new CoreException(new Status(IStatus.ERROR, null, "Cannot find file in template!", e));
        }
    }

    protected InputStream replaceContent(File srcFile) throws CoreException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(srcFile));

            StringBuilder sb = new StringBuilder(1024);

            while (true) {
                String s = reader.readLine();
                if (s == null) break;

                sb.append(s);
                sb.append('\n');
            }

            String s = sb.toString();
            // replace all '@key@' sequences with 'value'
            for (String key : replaces.keySet()) {
                String value = replaces.get(key);

                s = s.replace(key, value);
            }

            byte[] buffer = s.getBytes();
            return new ByteArrayInputStream(buffer);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, null, "Cannot update content of a template file!", e));
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    protected boolean isReplaceable(IFile destFile) {
        String ext = destFile.getFileExtension();

        if ("properties".equalsIgnoreCase(ext)) return true;
        if ("xml".equalsIgnoreCase(ext)) return true;
        if ("launch".equalsIgnoreCase(ext)) return true;
        if ("MANIFEST.MF".equalsIgnoreCase(destFile.getName())) return true;

        return false;
    }

    protected boolean isInclude(File file) {
        String name = file.getName();

        if (".project".equalsIgnoreCase(name)) return false;
        if (".classpath".equalsIgnoreCase(name)) return false;
        if (".foo".equalsIgnoreCase(name)) return false;

        if (name.endsWith(".launch")) return false;

        // for debug
        if ("CVS".equals(name)) return false;
        if (".svn".equals(name)) return false;
        if (".cvsignore".equals(name)) return false;

        return true;
    }
}
