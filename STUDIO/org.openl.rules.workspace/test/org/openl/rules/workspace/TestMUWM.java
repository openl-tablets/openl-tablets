package org.openl.rules.workspace;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.Ignore;
import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.Property;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.PropertyImpl;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.uw.UserWorkspace;

@Ignore("Manual test")
public class TestMUWM {
    @Ignore("Auxiliary class")
    public static class PR extends AProjectResource {

        public PR() {
            super(null, null, null);
        }

        public void addProperty(Property property) throws PropertyException {
            throw new PropertyException("Not supported", null);
        }

        public AProjectArtefact getArtefact(String name) throws ProjectException {
            throw new ProjectException("Not supported", null);
        }

        public ArtefactPath getArtefactPath() {
            return new ArtefactPathImpl("/noname");
        }

        public InputStream getContent() throws ProjectException {
            String s = "Generated at " + System.currentTimeMillis();

            return new ByteArrayInputStream(s.getBytes());
        }

        public String getName() {
            return "noname";
        }

        public Collection<Property> getProperties() {
            return new LinkedList<Property>();
        }

        public Property getProperty(String name) throws PropertyException {
            throw new PropertyException("Not supported", null);
        }

        public boolean hasArtefact(String name) {
            return false;
        }

        public boolean hasProperty(String name) {
            return false;
        }

        public boolean isFolder() {
            return false;
        }

        public Property removeProperty(String name) throws PropertyException {
            throw new PropertyException("Not supported", null);
        }
    }

    public static void main(String[] args) throws WorkspaceException, ProjectException {
        MultiUserWorkspaceManager muwm = new MultiUserWorkspaceManager();

        WorkspaceUser wu = new WorkspaceUserImpl("127.0.0.1");
        UserWorkspace uw = muwm.getUserWorkspace(wu);
        uw.activate();

        System.out.println(uw.getProjects().size());

        String name = "p1";
        AProject p;
        if (!uw.hasProject(name)) {
            p = uw.createProject(name);
        } else {
            p = uw.getProject(name);
        }

        p.edit(wu);

        AProjectFolder uwpf;
        try {
            uwpf = (AProjectFolder) p.getArtefact("F1");
            AProjectResource uwpr = (AProjectResource) uwpf.getArtefact("some-file");

            // String s = "Updated at " + System.currentTimeMillis();
            // uwpr.setContent(new ByteArrayInputStream(s.getBytes()));

            Collection<ProjectVersion> vers = uwpr.getVersions();
            System.out.println("- listing versions: " + vers.size());
            for (ProjectVersion ver : vers) {
                System.out.println("  " + ver.getVersionName());
            }
            try {
                uwpr.addProperty(new PropertyImpl("LOB", "line of business"));
            } catch (Exception e) {
                // TODO: handle exception
            }

            Collection<Property> props = uwpr.getProperties();
            System.out.println("- listing properties: " + props.size());
            for (Property pr : props) {
                System.out.println("  " + pr.getName() + " = " + pr.getString());
            }
        } catch (ProjectException e) {
            uwpf = p.addFolder("F1");
            uwpf.addFolder("F1-1");
            PR resource = new PR();
            uwpf.addResource("some-file", resource);
        }

        p.save(wu);

        for (RulesProject uwp : uw.getProjects()) {
            System.out.println("-> opening " + uwp.getName());
            uwp.open();
        }

        uw.passivate();

        System.out.println("Done.");
    }
}
