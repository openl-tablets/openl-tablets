package org.openl.rules.workspace.mock;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.FolderAPI;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MockFolder extends MockArtefact implements FolderAPI {
    private Map<String, MockArtefact> artefacts = new HashMap<String, MockArtefact>();

    public MockFolder(String name) {
        super(name);
    }

    public MockArtefact getArtefact(String name) throws ProjectException {
        return artefacts.get(name);
    }

    public boolean hasArtefact(String name) {
        return artefacts.containsKey(name);
    }

    public MockFolder addFolder(String name) throws ProjectException {
        MockFolder folder = new MockFolder(name);
        artefacts.put(name, folder);
        return folder;
    }

    public MockResource addResource(String name, InputStream content) throws ProjectException {
        MockResource resource = new MockResource(name);
        resource.setContent(content);
        artefacts.put(name, resource);
        return resource;
    }

    public Collection<MockArtefact> getArtefacts() {
        return artefacts.values();
    }
    
    @Override
    public boolean isFolder() {
        return true;
    }

    public MockFolder getVersion(CommonVersion version) throws ProjectException{
        return this;
    }
}
