package org.openl.rules.webstudio.web;

import java.util.Collection;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.message.IOpenLMessages;
import org.openl.message.OpenLMessage;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tree.richfaces.TreeNode;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

/**
 * Request scope managed bean providing logic for problems tree page of OpenL Studio.
 *
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
public class ProblemsBean {

    public static final String ERRORS_ROOT_NAME = "Errors";
    public static final String WARNINGS_ROOT_NAME = "Warnings";

    public static final String ERROR_NODE_NAME = "error";
    public static final String WARNING_NODE_NAME = "warning";

    private static MessageHandler messageHandler = new MessageHandler();

    public ProblemsBean() {
    }

    public TreeNode getTree() {
        int nodeCount = 1;

        WebStudio studio = WebStudioUtils.getWebStudio();

        if (studio.getCurrentProject() != null) {
            ProjectModel model = studio.getModel();

            IOpenLMessages openLMessages = model.getModuleMessages();

            TreeNode root = new TreeNode();

            if (openLMessages.hasErrors()) {
                TreeNode errorsRoot = createMessagesRoot(ERRORS_ROOT_NAME, openLMessages.getErrors().size());
                addMessageNodes(errorsRoot, ERROR_NODE_NAME, openLMessages.getErrors(), model);
                root.addChild(nodeCount++, errorsRoot);
            }

            if (openLMessages.hasWarnings()) {
                TreeNode warningsRoot = createMessagesRoot(WARNINGS_ROOT_NAME, openLMessages.getWarnings().size());
                addMessageNodes(warningsRoot, WARNING_NODE_NAME, openLMessages.getWarnings(), model);
                root.addChild(nodeCount++, warningsRoot);
            }

            return root;
        }
        return null;
    }

    public boolean isHasProblems() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        return studio.getCurrentProject() != null && studio.getModel().getModuleMessages().hasMessages();
    }

    private TreeNode createMessagesRoot(String rootName, int messagesNumber) {
        return new TreeNode(rootName, rootName, null, 0, messagesNumber, rootName.toLowerCase(), true);
    }

    private void addMessageNodes(TreeNode parent,
            String nodeName,
            Collection<OpenLMessage> messages,
            ProjectModel model) {
        int nodeCount = 1;

        for (OpenLMessage message : messages) {
            String url = getNodeUrl(message, model);
            TreeNode messageNode = new TreeNode(true,
                message.getSummary(),
                "",
                url,
                0,
                0,
                nodeName.toLowerCase(),
                true);
            parent.addChild(nodeCount++, messageNode);
        }
    }

    private String getNodeUrl(OpenLMessage message, ProjectModel model) {
        String url = messageHandler.getSourceUrl(message, model);

        if (StringUtils.isBlank(url)) {
            url = messageHandler.getUrlForEmptySource(message);
        }

        return url;
    }

}
