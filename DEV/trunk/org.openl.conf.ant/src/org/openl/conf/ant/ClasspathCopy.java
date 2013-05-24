package org.openl.conf.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.openl.util.FileTool;
import org.openl.util.Log;
import org.openl.util.StringTool;

public class ClasspathCopy extends Task {

    static abstract class CopyExecutor {
        public abstract void copyDir(String targetDir, File wspaceDir, File relativeFile);

        public abstract void copyFile(String targetDir, File wspaceDir, File relativeFile);
    }

    class CopyWebapp extends CopyExecutor {
        @Override
        public void copyDir(String webappTargetDir, File wspaceSrcDir, File relativeFile) {
            Copy cpy = getCopyTask();
            cpy.setTodir(new File(webappTargetDir + "/WEB-INF/classes"));
            FileSet fset = new FileSet();
            fset.setDir(new File(wspaceSrcDir + "/" + relativeFile.getPath()));
            cpy.addFileset(fset);
            cpy.setVerbose(true);
            cpy.execute();
        }

        @Override
        public void copyFile(String wspaceTargetDir, File wspaceSrcDir, File relativeFile) {
            Copy cpy = getCopyTask();
            cpy.setTodir(new File(wspaceTargetDir + "/WEB-INF/lib"));
            cpy.setFile(new File(wspaceSrcDir + "/" + relativeFile.getPath()));
            cpy.setVerbose(true);
            cpy.execute();
        }
    }

    class CopyWorkspace extends CopyExecutor {

        @Override
        public void copyDir(String wspaceTargetDir, File wspaceSrcDir, File relativeFile) {
            Copy cpy = getCopyTask();
            cpy.setTodir(new File(wspaceTargetDir + "/" + relativeFile.getPath()));
            FileSet fset = new FileSet();
            fset.setDir(new File(wspaceSrcDir + "/" + relativeFile.getPath()));
            cpy.addFileset(fset);
            cpy.setVerbose(true);
            cpy.execute();

        }

        @Override
        public void copyFile(String wspaceTargetDir, File wspaceSrcDir, File relativeFile) {
            Copy cpy = getCopyTask();
            cpy.setTofile(new File(wspaceTargetDir + "/" + relativeFile.getPath()));
            cpy.setFile(new File(wspaceSrcDir + "/" + relativeFile.getPath()));
            cpy.setVerbose(true);
            cpy.execute();
        }

    }

    public static final String JAVA_CLASSPATH_PROPERTY = "java.class.path";

    public static final String COPY_WORKSPACE_MODE = "copy.workspace";

    public static final String COPY_WEBAPP_MODE = "copy.webapp";

    public static final String[] ALL_MODES = { COPY_WORKSPACE_MODE, COPY_WEBAPP_MODE };

    protected String startupFileName;

    private boolean copyTomcat = false;

    String copyMode = null;

    String targetDir = null;

    String defaultExclude = ".*apache.ant.*|.*javacc.*|.*junit.*";

    boolean validateThisDir;

    String projectDir = ".";

    String classpathExclude = null;

    String classpathInclude = null;

    String pathSeparator = File.pathSeparator;

    String classpath = null;

    public static String findApacheTomcatHome() throws IOException {
        String cpath = System.getProperty(JAVA_CLASSPATH_PROPERTY);

        String[] pathElements = StringTool.tokenize(cpath, File.pathSeparator);

        for (int i = 0; i < pathElements.length; i++) {
            if (pathElements[i].endsWith("bootstrap.jar")) {
                File tomcatHome = new File(pathElements[i]).getCanonicalFile().getParentFile().getParentFile()
                        .getParentFile();
                return tomcatHome.toString();
            }
        }

        throw new RuntimeException("Could not find bootstrap.jar in " + JAVA_CLASSPATH_PROPERTY);
    }

    static String printModes() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ALL_MODES.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(ALL_MODES[i]);
        }
        return sb.toString();
    }

    @Override
    public void execute() throws BuildException {

        if (targetDir == null) {
            throw new BuildException("Attribute targetDir must be defined for ClasspathCopy");
        }

        if (copyMode == null) {
            throw new BuildException("Attribute copyMode must be defined for ClasspathCopy. Allowed modes: "
                    + printModes());
        }

        File projectFile = new File(projectDir);
        File wspaceDir;
        try {
            wspaceDir = projectFile.getCanonicalFile().getParentFile();
        } catch (IOException e1) {
            throw new BuildException(e1);
        }

        String[] classpathElements = StringTool.tokenize(getClasspath(), pathSeparator);

        String excludePattern = defaultExclude;
        if (classpathExclude != null) {
            excludePattern += "|" + classpathExclude;
        }

        CopyExecutor cp = makeCopyExecutor();
        List<File> cpath = new ArrayList<File>();

        for (int i = 0; i < classpathElements.length; i++) {
            String element = classpathElements[i];

            if (element.matches(excludePattern)) {
                Log.info("Exclude " + element);
                continue;

            }

            try {

                File sourceFile = new File(element);

                File relativeFile = FileTool.buildRelativePath(wspaceDir, sourceFile);

                if (sourceFile.isDirectory()) {
                    cp.copyDir(targetDir, wspaceDir, relativeFile);
                } else {
                    cp.copyFile(targetDir, wspaceDir, relativeFile);
                }

                if (copyMode.equals(COPY_WORKSPACE_MODE)) {
                    File relativePath = FileTool.buildRelativePath(projectFile, sourceFile);
                    cpath.add(relativePath);

                }
            } catch (IOException e) {
                throw new BuildException(e);
            }

        }

        if (copyTomcat) {

            File sourceFile, relativeFile;
            try {
                sourceFile = new File(findApacheTomcatHome());
                relativeFile = FileTool.buildRelativePath(wspaceDir, sourceFile);
            } catch (IOException e) {
                throw new BuildException(e);
            }

            cp.copyDir(targetDir, wspaceDir, relativeFile);

        }

        if (startupFileName != null) {
            try {
                generateStartupFile(cpath);
            } catch (IOException e) {
                throw new BuildException("Error creating startup file: ", e);
            }
        }

    }

    private void generateStartupBatFile(List<File> path) throws IOException {
        String batFile = startupFileName;
        if (!batFile.endsWith(".bat") || !batFile.endsWith(".cmd")) {
            batFile += ".bat";
        }

        File startupFile = new File(targetDir + "/" + batFile);

        PrintWriter w = new PrintWriter(new FileWriter(startupFile));

        w.println("cd " + getProjectDirName());
        w.println("set CP=");

        for (Iterator<File> iterator = path.iterator(); iterator.hasNext();) {
            File element = iterator.next();
            w.println("set CP=%CP%" + element.getPath() + ";");
        }

        w.println();
        w.println("java -Xms256M -Xmx1024M -cp %CP% org.openl.rules.webstudio.util.StartTomcat");
        w.println("pause");

        w.close();

    }

    private void generateStartupFile(List<File> path) throws IOException {
        String targetPlatform = System.getProperty("os.name");

        if (targetPlatform.contains("Windows")) {
            generateStartupBatFile(path);
        } else {
            generateStartupUnixShellFile();
        }
    }

    private void generateStartupUnixShellFile() {

    }

    public String getClasspath() {
        if (classpath == null) {
            classpath = System.getProperty(JAVA_CLASSPATH_PROPERTY);
        }
        return classpath;
    }

    public String getClasspathExclude() {
        return classpathExclude;
    }

    public String getClasspathInclude() {
        return classpathInclude;
    }

    public String getCopyMode() {
        return copyMode;
    }

    Copy getCopyTask() {
        Copy cpy = new Copy();
        cpy.setProject(getProject());
        cpy.setPreserveLastModified(true);
        return cpy;

    }

    public String getDefaultExclude() {
        return defaultExclude;
    }

    public String getPathSeparator() {
        return pathSeparator;
    }

    public String getProjectDir() {
        return projectDir;
    }

    private String getProjectDirName() throws IOException {
        return new File(projectDir).getCanonicalFile().getName();
    }

    public String getStartupFileName() {
        return startupFileName;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public boolean isCopyTomcat() {
        return copyTomcat;
    }

    public boolean isValidateThisDir() {
        return validateThisDir;
    }

    private CopyExecutor makeCopyExecutor() {
        if (copyMode.equals(COPY_WORKSPACE_MODE)) {
            return new CopyWorkspace();
        }

        if (copyMode.equals(COPY_WEBAPP_MODE)) {
            return new CopyWebapp();
        }

        throw new BuildException("Invalid Copy Mode: " + copyMode + ". Allowed modes: " + printModes());
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public void setClasspathExclude(String classpathExclude) {
        this.classpathExclude = classpathExclude;
    }

    public void setClasspathInclude(String classpathInclude) {
        this.classpathInclude = classpathInclude;
    }

    public void setCopyMode(String copyMode) {
        this.copyMode = copyMode;
    }

    public void setCopyTomcat(boolean copyTomcatProject) {
        copyTomcat = copyTomcatProject;
    }

    public void setDefaultExclude(String defaultExclude) {
        this.defaultExclude = defaultExclude;
    }

    public void setPathSeparator(String pathSeparator) {
        this.pathSeparator = pathSeparator;
    }

    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }

    public void setStartupFileName(String startupFile) {
        startupFileName = startupFile;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public void setValidateThisDir(boolean validateThisDir) {
        this.validateThisDir = validateThisDir;
    }

}
