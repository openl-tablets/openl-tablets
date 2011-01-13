//package org.openl.rules.workspace.mock;
//
//import org.openl.rules.workspace.abstracts.ProjectArtefact;
//import org.openl.rules.workspace.abstracts.ProjectFolder;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Date;
//import java.util.LinkedList;
//
//public class MockFolder extends MockArtefact implements ProjectFolder {
//    private static final Collection<ProjectArtefact> EMPTY_LIST = new LinkedList<ProjectArtefact>();
//
//    private Collection<ProjectArtefact> artefacts;
//
//    public MockFolder(String name, MockFolder parent) {
//        super(name, parent);
//    }
//
//    public MockFolder _setEffectiveDate(Date date) {
//        setEffectiveDate(date);
//        return this;
//    }
//
//    public MockArtefact add(MockArtefact artefact) {
//        if (artefacts == null) {
//            artefacts = new ArrayList<ProjectArtefact>();
//        }
//        artefacts.add(artefact);
//        return artefact;
//    }
//
//    public MockResource addFile(String artefactName) {
//        return (MockResource) add(new MockResource(artefactName, this));
//    }
//
//    public MockFolder addFolder(String artefactName) {
//        MockFolder folder = new MockFolder(artefactName, this);
//        add(folder);
//        return folder;
//    }
//
//    public Collection<? extends ProjectArtefact> getArtefacts() {
//        return (artefacts == null) ? EMPTY_LIST : artefacts;
//    }
//
//    @Override
//    public boolean isFolder() {
//        return true;
//    }
//
//    public void setArtefacts(Collection<ProjectArtefact> artefacts) {
//        this.artefacts = artefacts;
//    }
//}
