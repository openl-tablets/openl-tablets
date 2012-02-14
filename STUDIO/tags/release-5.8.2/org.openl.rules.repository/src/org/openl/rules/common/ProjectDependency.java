package org.openl.rules.common;

import java.io.InputStream;
import java.util.List;

import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDependencyImpl;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;

public interface ProjectDependency {
    public static class ProjectDependencyConverter implements Converter {

        public boolean canConvert(Class cls) {
            return ProjectDependency.class.isAssignableFrom(cls);
        }

        public void marshal(Object arg0, HierarchicalStreamWriter arg1, MarshallingContext arg2) {
            ProjectDependency dep = (ProjectDependency) arg0;
            arg1.startNode("projectName");
            arg1.setValue(dep.getProjectName());
            arg1.endNode();
            ;
            arg1.startNode("lowerLimit");
            arg2.convertAnother(dep.getLowerLimit());
            arg1.endNode();
            ;
            if (dep.hasUpperLimit()) {
                arg1.startNode("upperLimit");
                arg2.convertAnother(dep.getUpperLimit());
                arg1.endNode();
                ;
            }
        }

        public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
            arg0.moveDown();
            String projectName = arg0.getValue();
            arg0.moveUp();
            arg0.moveDown();
            CommonVersionImpl lowerLimit = (CommonVersionImpl) arg1.convertAnother(null, CommonVersion.class);
            arg0.moveUp();
            if (arg0.hasMoreChildren()) {
                arg0.moveDown();
                CommonVersionImpl upperLimit = (CommonVersionImpl) arg1.convertAnother(null, CommonVersion.class);
                arg0.moveUp();
                return new ProjectDependencyImpl(projectName, lowerLimit, upperLimit);
            } else {
                return new ProjectDependencyImpl(projectName, lowerLimit);
            }
        }
    }

    CommonVersion getLowerLimit();

    String getProjectName();

    CommonVersion getUpperLimit();

    boolean hasUpperLimit();

    public static class ProjectDependencyHelper {
        private static final XStream XSTREAM = new XStream(new DomDriver());
        static {
            XSTREAM.alias("dependencies", List.class);
            XSTREAM.aliasType("dependency", ProjectDependency.class);
            XSTREAM.aliasType("version", CommonVersion.class);
            XSTREAM.registerConverter(new CommonVersion.CommonVersionConverter());
            XSTREAM.registerConverter(new ProjectDependencyConverter());
        }

        public static String serialize(List<ProjectDependency> dependencies) {
            return XSTREAM.toXML(dependencies);
        }

        public static List<ProjectDependency> deserialize(InputStream source) {
            return (List<ProjectDependency>) XSTREAM.fromXML(source);
        }
    }
}
