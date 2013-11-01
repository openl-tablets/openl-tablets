package org.openl.rules.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openl.OpenL;
import org.openl.conf.ant.JavaAntTask;
import org.openl.conf.ant.JavaInterfaceAntTask;

/**
 * Generate OpenL interface, domain classes and project descriptor
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends BaseOpenLMojo {
    /**
     * Tasks that will generate classes. By default the type is
     * {@link JavaInterfaceAntTask}.
     * <p>
     * <b>Object Properties</b>
     * <table border="1">
     * <tr>
     * <th>Name</th>
     * <th>Type</th>
     * <th>Required</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>srcFile</td>
     * <td>String</td>
     * <td>true</td>
     * <td>Reference to the Excel file for which an interface class must be
     * generated.</td>
     * </tr>
     * <tr>
     * <td>targetClass</td>
     * <td>String</td>
     * <td>true</td>
     * <td>Full name of the interface class to be generated. OpenL Tablets
     * WebStudio recognizes modules in projects by interface classes and uses
     * their names in the user interface. If there are multiple wrappers with
     * identical names, only one of them is recognized as a module in OpenL
     * Tablets WebStudio.</td>
     * </tr>
     * <tr>
     * <td>displayName</td>
     * <td>String</td>
     * <td>false</td>
     * <td>End user oriented title of the file that appears in OpenL Tablets
     * WebStudio. Default value is Excel file name without extension.</td>
     * </tr>
     * <tr>
     * <td>targetSrcDir</td>
     * <td>String</td>
     * <td>false</td>
     * <td>Folder where the generated interface class must be placed. For
     * example: "src/main/java". Default value is:
     * "${project.build.sourceDirectory}"</td>
     * </tr>
     * <tr>
     * <td>openlName</td>
     * <td>String</td>
     * <td>false</td>
     * <td>OpenL configuration to be used. For OpenL Tablets, the following
     * value must always be used: org.openl.xls. Default value is:
     * "org.openl.xls"</td>
     * </tr>
     * <tr>
     * <td>userHome</td>
     * <td>String</td>
     * <td>false</td>
     * <td>Location of user-defined resources relative to the current OpenL
     * Tablets project. Default value is: "."</td>
     * </tr>
     * <tr>
     * <td>userClassPath</td>
     * <td>String</td>
     * <td>false</td>
     * <td>Reference to the folder with additional compiled classes that will be
     * imported by the module when the interface is generated. Default value is:
     * null.</td>
     * </tr>
     * <tr>
     * <td>ignoreTestMethods</td>
     * <td>boolean</td>
     * <td>false</td>
     * <td>If true - test methods will not be added to interface class. Used
     * only in JavaInterfaceAntTask. Default value is: false.</td>
     * </tr>
     * </table>
     * <p>
     */
    @Parameter(required = true)
    private JavaAntTask[] generateInterfaces;

    /**
     * If true - rules.xml will be regenerated from scratch on each run. If
     * false, rules.xml will not be generated. Default value is "true".
     */
    @Parameter(defaultValue = "true")
    private boolean createProjectDescriptor;

    /**
     * Default project id in rules.xml. If omitted - the name of a first module
     * in the project is used. Used only if createProjectDescriptor == true.
     */
    @Parameter
    private String projectId;

    /**
     * Default project name in rules.xml. If omitted - the name of a first
     * module in the project is used. Used only if createProjectDescriptor ==
     * true.
     */
    @Parameter
    private String projectName;

    /**
     * Default classpath entries in rules.xml. Default value is {"."} Used only
     * if createProjectDescriptor == true.
     */
    @Parameter
    private String[] classpaths = { "." };

    @Override
    public void execute() throws MojoExecutionException {
        if (getLog().isInfoEnabled()) {
            getLog().info("Running OpenL GenerateMojo...");
        }
        for (JavaAntTask task : generateInterfaces) {
            if (getLog().isInfoEnabled()) {
                getLog().info(String.format("Generating classes for module '%s'", task.getSrcFile()));
            }
            initDefaultValues(task);
            task.execute();
        }
    }

    private void initDefaultValues(JavaAntTask task) {
        if (task.getOpenlName() == null) {
            task.setOpenlName(OpenL.OPENL_JAVA_RULE_NAME);
        }
        if (task.getTargetSrcDir() == null) {
            task.setTargetSrcDir(project.getBuild().getSourceDirectory());
        }

        if (task.getDisplayName() == null) {
            task.setDisplayName(FilenameUtils.getBaseName(task.getSrcFile()));
        }

        initResourcePath(task);

        if (task instanceof JavaInterfaceAntTask) {
            JavaInterfaceAntTask interfaceTask = (JavaInterfaceAntTask) task;
            interfaceTask.setCreateProjectDescriptor(createProjectDescriptor);
            interfaceTask.setDefaultProjectId(projectId);
            interfaceTask.setDefaultProjectName(projectName);
            interfaceTask.setDefaultClasspaths(classpaths);
        }
    }

    private void initResourcePath(JavaAntTask task) {
        String srcFile = task.getSrcFile().replace("\\", "/");
        String baseDir = project.getBasedir().getAbsolutePath();

        String directory = getSubDirectory(baseDir, openlResourcesDirectory).replace("\\", "/");
        if (srcFile.startsWith(directory)) {
            srcFile = getSubDirectory(directory, srcFile);
            task.setResourcesPath(directory);
            task.setSrcFile(srcFile);
            return;
        }

        @SuppressWarnings("unchecked")
        List<Resource> resources = (List<Resource>) project.getResources();
        for (Resource resource : resources) {
            String resourceDirectory = resource.getDirectory();
            resourceDirectory = getSubDirectory(baseDir, resourceDirectory).replace("\\", "/");

            if (srcFile.startsWith(resourceDirectory)) {
                srcFile = getSubDirectory(resourceDirectory, srcFile);
                task.setResourcesPath(resourceDirectory);
                task.setSrcFile(srcFile);
                break;
            }
        }
    }

    private String getSubDirectory(String baseDir, String resourceDirectory) {
        if (resourceDirectory.startsWith(baseDir)) {
            resourceDirectory = resourceDirectory.substring(resourceDirectory.lastIndexOf(baseDir) + baseDir.length());
            resourceDirectory = removeSlashFromBeginning(resourceDirectory);
        }
        return resourceDirectory;
    }

    private String removeSlashFromBeginning(String resourceDirectory) {
        if (resourceDirectory.startsWith("/") || resourceDirectory.startsWith("\\")) {
            resourceDirectory = resourceDirectory.substring(1);
        }
        return resourceDirectory;
    }
}
