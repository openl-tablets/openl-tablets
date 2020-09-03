package org.openl.rules.webstudio.web.repository.tree;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.webstudio.filter.IFilter;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.webstudio.web.repository.UiConst;

public class TreeProductFolder extends TreeFolder {
    private IFilter<AProjectArtefact> filter;

    public TreeProductFolder(String id, String name, IFilter<AProjectArtefact> filter) {
        super(id, name, filter);
        this.filter = filter;
    }

    @Override
    public void addChild(AProjectArtefact childArtefact) {
        String name = childArtefact.getName();

        String id = RepositoryUtils.getTreeNodeId(childArtefact);
        if (childArtefact.isFolder()) {
            TreeProductFolder treeFolder = new TreeProductFolder(id, name, filter);
            treeFolder.setData(childArtefact);
            add(treeFolder);
        } else {
            TreeProductFile treeFile = new TreeProductFile(id, name);
            treeFile.setData(childArtefact);
            add(treeFile);
        }
    }

    @Override
    public String getType() {
        return UiConst.TYPE_PRODUCTION_FOLDER;
    }

    /** {@inheritDoc} */
    @Override
    public String getIcon() {
        return UiConst.ICON_FOLDER;
    }

    /** {@inheritDoc} */
    @Override
    public String getIconLeaf() {
        // in both cases we use the same icons
        return getIcon();
    }

}
