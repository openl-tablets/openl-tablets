package org.openl.rules.webstudio.web.repository.tree;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.repository.AllFilter;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.webstudio.web.repository.SelectFolderBean;

public class RepositoryFolderNode extends TreeFolder {
    private final Logger log = LoggerFactory.getLogger(RepositoryFolderNode.class);

    private Map<Object, TreeNode> elements;
    private final Repository repository;
    private final String path;
    private final SelectFolderBean selectFolderBean;

    public RepositoryFolderNode(Repository repository, String name, String path, SelectFolderBean selectFolderBean) {
        super(RepositoryUtils.getTreeNodeId(repository.getId(), name), name, new AllFilter<>());
        this.repository = repository;
        this.path = path;
        this.selectFolderBean = selectFolderBean;
    }

    @Override
    public Map<Object, TreeNode> getElements() {
        if (elements == null && !isLeafOnly()) {
            elements = new LinkedHashMap<>();
            try {
                String pathToList = path.substring(1);
                List<FileData> fileData = repository.listFolders(pathToList);
                for (FileData data : fileData) {
                    String childPath = data.getName();
                    String childName = childPath.substring(childPath.lastIndexOf('/') + 1);
                    RepositoryFolderNode child = new RepositoryFolderNode(repository, childName, "/" + childPath + "/", selectFolderBean);
                    child.setParent(this);
                    elements.put(child.getId(), child);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return elements;
    }

    @Override
    public boolean isLeaf() {
        // If elements aren't initialized, consider it as not leaf
        return isLeafOnly() || elements != null && elements.isEmpty();
    }

    @Override
    public void refresh() {
        super.refresh();
        elements = null;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean isExpanded() {
        return super.isExpanded() || selectFolderBean.isExpanded(this);
    }
}
