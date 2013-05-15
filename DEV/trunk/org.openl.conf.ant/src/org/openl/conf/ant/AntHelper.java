/*
 * Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf.ant;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

/**
 * @author snshor
 * 
 */
public class AntHelper {
    Project project;

    public static void main(String[] args) {
        new AntHelper(args[0], args[1], null);
    }

    public AntHelper(String antFile, String targetName, Properties props) {
        project = new Project();
        project.init();
        project.addBuildListener(createLogger());
        project.setCoreLoader(Thread.currentThread().getContextClassLoader());

        if (props != null) {
            for (Iterator<Map.Entry<Object, Object>> iter = props.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<Object, Object> element = iter.next();
                project.setProperty((String) element.getKey(), (String) element.getValue());
            }
        }

        ProjectHelper helper = ProjectHelper.getProjectHelper();
        project.addReference("ant.projectHelper", helper);
        helper.parse(project, new File(antFile));

        project.executeTarget(targetName);

    }

    BuildLogger createLogger() {
        BuildLogger logger = new DefaultLogger();

        logger.setMessageOutputLevel(Project.MSG_WARN);
        logger.setOutputPrintStream(System.err);
        logger.setErrorPrintStream(System.err);

        return logger;
    }

    Object getConfigurationObject(String name) {
        return project.getReference(name);
    }
}
