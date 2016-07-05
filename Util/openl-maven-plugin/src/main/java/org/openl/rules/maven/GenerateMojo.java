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

import java.io.File;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openl.OpenL;
import org.openl.conf.ant.JavaAntTask;
import org.openl.conf.ant.JavaInterfaceAntTask;
import org.openl.util.FileUtils;

/**
 * Generate OpenL interface, domain classes, project descriptor and unit tests
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends BaseOpenLMojo {
    /**
     * Tasks that will generate classes.
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
     * <td>Reference to the folder with additional compiled classes imported
     * by the module when the interface is generated. Default value is:
     * null.</td>
     * </tr>
     * <tr>
     * <td>ignoreTestMethods</td>
     * <td>boolean</td>
     * <td>false</td>
     * <td>If true, test methods will not be added to interface class. Used
     * only in JavaInterfaceAntTask. Default value is: true.</td>
     * </tr>
     * <tr>
     * <td>generateUnitTests</td>
     * <td>boolean</td>
     * <td>false</td>
     * <td>Overwrites base {@link #generateUnitTests} value</td>
     * </tr>
     * <tr>
     * <td>unitTestTemplatePath</td>
     * <td>String</td>
     * <td>false</td>
     * <td>Overwrites base {@link #unitTestTemplatePath} value</td>
     * </tr>
     * <tr>
     * <td>overwriteUnitTests</td>
     * <td>boolean</td>
     * <td>false</td>
     * <td>Overwrites base {@link #overwriteUnitTests} value</td>
     * </tr>
     * </table>
     * <p>
     */
    @Parameter(required = true)
    private JavaAntTask[] generateInterfaces;

    /**
     * If true, rules.xml will be generated if it doesn't exist. If
     * false, rules.xml will not be generated. Default value is "true".
     * @see #overwriteProjectDescriptor
     */
    @Parameter(defaultValue = "true")
    private boolean createProjectDescriptor;

    /**
     * If true, rules.xml will be overwritten on each run. If
     * false, rules.xml generation will be skipped if it exists.
     * Makes sense only if {@link #createProjectDescriptor} == true.
     * Default value is "true".
     * @see #createProjectDescriptor
     */
    @Parameter(defaultValue = "true")
    private boolean overwriteProjectDescriptor;

    /**
     * Default project name in rules.xml. If omitted, the name of the first
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

    /**
     * If true, JUnit tests for OpenL Tablets Test tables will be generated.
     * Default value is "false"
     */
    @Parameter(defaultValue = "false")
    private Boolean generateUnitTests;

    /**
     * Path to Velocity template for generated unit tests.
     * If omitted, default template will be used.
     * Available in template variables:
     * <table border="1">
     * <tr>
     * <th>Name</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>openlInterfacePackage</td>
     * <td>Package of generated interface class</td>
     * </tr>
     * <tr>
     * <td>openlInterfaceClass</td>
     * <td>Generated interface class name</td>
     * </tr>
     * <tr>
     * <td>testMethodNames</td>
     * <td>Available test method names</td>
     * </tr>
     * <tr>
     * <td>projectRoot</td>
     * <td>Root directory of OpenL project</td>
     * </tr>
     * <tr>
     * <td>srcFile</td>
     * <td>Reference to the Excel file for which an interface class must be
     * generated.</td>
     * </tr>
     * <tr>
     * <td>StringUtils</td>
     * <td>Apache commons utility class</td>
     * </tr>
     * </table>
     */
    @Parameter(defaultValue = "org/openl/rules/maven/JUnitTestTemplate.vm")
    private String unitTestTemplatePath;

    /**
     * If true, existing JUnit tests will be overwritten. If false, only absent tests will be generated, others will be skipped.
     */
    @Parameter(defaultValue = "false")
    private Boolean overwriteUnitTests;

    @Override
    public void execute() throws MojoExecutionException {
        if (getLog().isInfoEnabled()) {
            getLog().info("Running OpenL GenerateMojo...");
        }
        for (JavaAntTask task : generateInterfaces) {
            if (getLog().isInfoEnabled()) {
                getLog().info(String.format("Generating classes for module '%s'...", task.getSrcFile()));
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
            task.setDisplayName(FileUtils.getBaseName(task.getSrcFile()));
        }

        initResourcePath(task);

        if (task instanceof JavaInterfaceAntTask) {
            JavaInterfaceAntTask interfaceTask = (JavaInterfaceAntTask) task;
            initCreateProjectDescriptorState(interfaceTask);
            interfaceTask.setDefaultProjectName(projectName);
            interfaceTask.setDefaultClasspaths(classpaths);

            if (task instanceof GenerateInterface) {
                GenerateInterface generateInterface = (GenerateInterface) task;
                generateInterface.setLog(getLog());
                generateInterface.setTestSourceDirectory(project.getBuild().getTestSourceDirectory());
                if (generateInterface.getGenerateUnitTests() == null) {
                    generateInterface.setGenerateUnitTests(generateUnitTests);
                }
                if (generateInterface.getUnitTestTemplatePath() == null) {
                    generateInterface.setUnitTestTemplatePath(unitTestTemplatePath);
                }
                if (generateInterface.getOverwriteUnitTests() == null) {
                    generateInterface.setOverwriteUnitTests(overwriteUnitTests);
                }
            }
        }
    }

    private void initCreateProjectDescriptorState(JavaInterfaceAntTask task) {
        if (createProjectDescriptor) {
            if (new File(task.getResourcesPath(), "rules.xml").exists()) {
                task.setCreateProjectDescriptor(overwriteProjectDescriptor);
                return;
            }
        }
        task.setCreateProjectDescriptor(createProjectDescriptor);
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
