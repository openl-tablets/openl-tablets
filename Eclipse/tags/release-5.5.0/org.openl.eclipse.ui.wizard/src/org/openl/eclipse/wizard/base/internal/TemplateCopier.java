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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.openl.eclipse.wizard.base.INewProjectFromTemplateWizardCustomizer;
import org.openl.eclipse.wizard.base.INewProjectFromTemplateWizardCustomizerConstants;

/**
 * Copies files from a template into an eclipse projects.
 * <p>
 * Some files from the template can be ignored, some others can be processed
 * (replace '@key@' sequences with corresponding values).
 * </p>
 * Usage:
 *
 * <pre>
 * TemplateCopier copier = new TemplateCopier();
 *
 * copier.setProject(aProject);
 * copier.setTemplateLocation(whereTemplateIs);
 *
 * copier.copy(monitor);
 * </pre>
 *
 * @author Aleh Bykhavets
 *
 */
public class TemplateCopier implements INewProjectFromTemplateWizardCustomizerConstants {
    /**
     * An eclipse project
     */
    private IProject project;
    /**
     * Root folder of a template
     */
    private String templateLocation;
    /**
     * Replace map: '@key@' -> 'value'
     */
    private Map<String, String> replaces;
    /**
     * Rename map: old-file-name -> new-file-name
     */
    private Map<String, String> renames;

    private boolean ignoreManifect;

    public TemplateCopier(IProject project, INewProjectFromTemplateWizardCustomizer customizer) {
        replaces = new HashMap<String, String>();
        renames = new HashMap<String, String>();

        Properties properties = new Properties();
        customizer.setTemplateProperties(properties);

        String dstDir = project.getLocation().toOSString();
        String dstProjectName = project.getName();

        properties.setProperty(PROP_DST_DIR, dstDir);
        properties.setProperty(PROP_DST_PROJECT_NAME, dstProjectName);
        properties.setProperty(PROP_GEN_DIR, PROP_GEN_DIR_VALUE);
        properties.setProperty(PROP_JAVA_PKG, PROP_JAVA_PKG_VALUE);

        String templateLocation = properties.getProperty(PROP_SRC_DIR);

        setProject(project);
        setTemplateLocation(templateLocation);
        setReplaces(properties);

        addRename("Generate Template Wrapper.launch", "Generate " + dstProjectName + " Wrapper.launch");
    }

    /**
     * Adds rename record.
     *
     * @param templateFileName name of file in a template
     * @param projectFileName name of file in a project
     */
    public void addRename(String templateFileName, String projectFileName) {
        renames.put(templateFileName, projectFileName);
    }

    /**
     * Adds replace record.
     * <p>
     * While copying template into an eclipse project all replaceable files will
     * be processed. Any '@key@' sequences in them will be replaced with
     * corresponding values.
     * </p>
     * If key '@unknown-key@' wasn't added to replaces then it'll be left
     * untouched.
     *
     * @param key '@key@' sequence without '@' characters
     * @param value replace value
     */
    private void addReplace(String key, String value) {
        replaces.put("@" + key + "@", value);
    }

    /**
     * Copies content of a template into an eclipse project.
     * <p>
     * Before invoking this method <b>project</b> and <b>template location</b>
     * must be set. See {@link #setProject(IProject)} and
     * {@link #setTemplateLocation(String)} methods.
     *
     * @param monitor progress monitor
     * @throws CoreException if failed
     */
    public void copy(IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        monitor.beginTask("Copying...", 2);

        File templateRoot = new File(templateLocation);
        if (templateRoot.exists() && templateRoot.isDirectory()) {
            copyFiles(project, templateRoot, monitor);
            monitor.worked(1);
        } else {
            // invalid template folder
            throw new CoreException(new Status(IStatus.ERROR, null, "Invalid template folder: "
                    + templateRoot.getPath(), null));
        }

        monitor.done();
    }

    /**
     * Copies file from template into an eclipse project.
     *
     * @param destFile destination file (eclipse project)
     * @param srcFile source file (template)
     * @throws CoreException if failed
     */
    protected void copyFile(IFile destFile, File srcFile) throws CoreException {
        try {
            InputStream content;

            if (isReplaceable(destFile)) {
                // System.out.println("copy & replace: " + srcFile.getPath() + "
                // -> " + destFile.getLocation().toString());
                content = replaceContent(srcFile);
            } else {
                // System.out.println("copy: " + srcFile.getPath() + " -> " +
                // destFile.getLocation().toString());
                content = new FileInputStream(srcFile);
            }

            destFile.create(content, true, null);
        } catch (FileNotFoundException e) {
            throw new CoreException(new Status(IStatus.ERROR, null, "Cannot find file in template!", e));
        }
    }

    /**
     * Copies a template folder into an eclipse project recursively.
     *
     * @param dest destination folder (eclipse project)
     * @param srcFolder source folder (template)
     * @param monitor progress monitor
     * @throws CoreException if failed
     */
    protected void copyFiles(IContainer dest, File srcFolder, IProgressMonitor monitor) throws CoreException {
        File[] files = srcFolder.listFiles();
        if (files == null) {
            return;
        }

        SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
        subMonitor.beginTask(null, files.length + 1);

        for (File file : files) {
            if (monitor.isCanceled()) {
                throw new CancellationException();
            }
            if (!isInclude(file)) {
                continue;
            }

            String destName = file.getName();

            // check, whether file should be renamed
            String s = renames.get(destName);
            if (s != null) {
                destName = s;
            }

            Path destPath = new Path(destName);

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

    /**
     * Copies (and modifies) launch file.
     *
     * @param launchName name of launch file in an eclipse project
     * @param srcFile source file in a template
     * @throws CoreException if failed
     */
    protected void copyLaunch(String launchName, File srcFile) throws CoreException {
        if (srcFile.exists()) {
            Path destPath = new Path(launchName);
            IFile destFile = project.getFile(destPath);
            copyFile(destFile, srcFile);
        } else {
            // no such file
            throw new CoreException(new Status(IStatus.ERROR, null, "Cannot find template file: " + srcFile.getPath(),
                    null));
        }
    }

    /**
     * Checks whether a file or a folder should be copied from template into an
     * eclipse project.
     *
     * @param file template file
     * @return true -- include; false -- do not copy
     */
    protected boolean isInclude(File file) {
        String name = file.getName();

        if (".project".equalsIgnoreCase(name)) {
            return false;
        }
        if (".classpath".equalsIgnoreCase(name)) {
            return false;
        }
        if (".foo".equalsIgnoreCase(name)) {
            return false;
        }
        if (".info".equalsIgnoreCase(name)) {
            return false;
        }

        // for debug
        if ("CVS".equals(name)) {
            return false;
        }
        if (".svn".equals(name)) {
            return false;
        }
        if (".cvsignore".equals(name)) {
            return false;
        }

        if (ignoreManifect && "META-INF".equals(name)) {
            return false;
        }

        return true;
    }

    /**
     * Checks whether content of a file should be modified. I.e. whether a file
     * can contain '@key@' sequences.
     *
     * @param destFile file in an eclipse project
     * @return true if file should be processed on copy
     */
    protected boolean isReplaceable(IFile destFile) {
        String ext = destFile.getFileExtension();

        if ("properties".equalsIgnoreCase(ext)) {
            return true;
        }
        // in case of new project
        if ("launch".equalsIgnoreCase(ext)) {
            return true;
        }

        // TODO make them '@key@' free
        if ("xml".equalsIgnoreCase(ext)) {
            return true;
        }
        if ("MANIFEST.MF".equalsIgnoreCase(destFile.getName())) {
            return true;
        }

        return false;
    }

    /**
     * Creates InputStream from source file and modifies its content. Replaces
     * '@key@' sequences with corresponding values from {@link #replaces} map.
     * <p>
     * Note, it works for 'ASCII' like files. I.e. for files where each
     * character is represented by 1 byte.
     *
     * @param srcFile template source file
     * @return new input stream (byte array)
     * @throws CoreException if failed
     */
    protected InputStream replaceContent(File srcFile) throws CoreException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(srcFile));

            StringBuilder sb = new StringBuilder(1024);

            while (true) {
                String s = reader.readLine();
                if (s == null) {
                    break;
                }

                sb.append(s);
                sb.append('\n');
            }

            String s = sb.toString();
            // replace all '@key@' sequences with 'value'
            for (String key : replaces.keySet()) {
                String value = replaces.get(key);

                s = s.replace(key, value);
            }

            // WARNING: works with char=byte encodings only
            byte[] buffer = s.getBytes();
            return new ByteArrayInputStream(buffer);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, null, "Cannot update content of a template file!", e));
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public void setIgnoreManifect(boolean ignoreManifect) {
        this.ignoreManifect = ignoreManifect;
    }

    /**
     * Sets an eclipse project.
     *
     * @param project an existing eclipse project
     * @see #copy(IProgressMonitor)
     */
    private void setProject(IProject project) {
        this.project = project;
    }

    /**
     * Adds replace records.
     *
     * @param props set of properties
     * @see #addReplace(String, String)
     */
    private void setReplaces(Properties props) {
        for (Object k : props.keySet()) {
            String key = k.toString();
            String value = props.getProperty(key);

            addReplace(key, value);
        }
    }

    /**
     * Sets location of a template folder.
     *
     * @param templateLocation location where a template folder is
     * @see #copy(IProgressMonitor)
     */
    private void setTemplateLocation(String templateLocation) {
        this.templateLocation = templateLocation;
    }
}
