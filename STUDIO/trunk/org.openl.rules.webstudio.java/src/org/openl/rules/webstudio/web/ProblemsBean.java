package org.openl.rules.webstudio.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tree.TreeNodeData;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * Request scope managed bean providing logic for problems tree page of OpenL Studio.
 * 
 * @author Andrei Astrouski
 */
public class ProblemsBean {

    public static final String ERRORS_ROOT_NAME = "Errors";
    public static final String WARNINGS_ROOT_NAME = "Warnings";

    public static final String ERROR_NODE_NAME = "error";
    public static final String WARNING_NODE_NAME = "warning";
    
    private static Map<Class<?>, MessageHandler> messageHandlers;
    
    static {
        messageHandlers = new HashMap<Class<?>, MessageHandler>();
        messageHandlers.put(OpenLErrorMessage.class, new ErrorMessageHandler());
        messageHandlers.put(OpenLWarnMessage.class, new WarningMessageHandler());
        messageHandlers.put(OpenLMessage.class, new MessageHandler());
    }
    
    public ProblemsBean() {
    }

    public TreeNode<?> getTree() {
        int nodeCount = 1;

        WebStudio studio = WebStudioUtils.getWebStudio();

        if (studio.getCurrentProject() != null) {
            ProjectModel model = studio.getModel();
            CompiledOpenClass compiledOpenClass = model.getCompiledOpenClass();

            List<OpenLMessage> messages = compiledOpenClass.getMessages();

            TreeNode<TreeNodeData> root = new TreeNodeImpl<TreeNodeData>();

            List<OpenLMessage> errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR);
            if (CollectionUtils.isNotEmpty(errorMessages)) {
                TreeNode<TreeNodeData> errorsRoot = createMessagesRoot(ERRORS_ROOT_NAME, errorMessages.size());
                addMessageNodes(errorsRoot, ERROR_NODE_NAME, errorMessages, model);
                root.addChild(nodeCount++, errorsRoot);
            }

            List<OpenLMessage> warningMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
            if (CollectionUtils.isNotEmpty(warningMessages)) {
                TreeNode<TreeNodeData> warningsRoot = createMessagesRoot(WARNINGS_ROOT_NAME, warningMessages.size());
                addMessageNodes(warningsRoot, WARNING_NODE_NAME, warningMessages, model);
                root.addChild(nodeCount++, warningsRoot);
            }

            return root;
        }
        return null;
    }

    private TreeNode<TreeNodeData> createMessagesRoot(String rootName, int messagesNumber) {
        TreeNode<TreeNodeData> messagesRoot = new TreeNodeImpl<TreeNodeData>();
        TreeNodeData nodeData = new TreeNodeData(
                rootName + " [" + messagesNumber + "]", rootName, null, 0, rootName.toLowerCase(), true);
        messagesRoot.setData(nodeData);
        return messagesRoot;
    }

    private void addMessageNodes(TreeNode<TreeNodeData> parent, String nodeName, List<OpenLMessage> messages, ProjectModel model) {
        int nodeCount = 1;

        for (OpenLMessage message : messages) {
            TreeNode<TreeNodeData> messageNode = new TreeNodeImpl<TreeNodeData>();
            String url = getNodeUrl(message, model);
            TreeNodeData nodeData = new TreeNodeData(message.getSummary(), message.getDetails(),
                    url, 0, nodeName.toLowerCase(), true);
            messageNode.setData(nodeData);
            parent.addChild(nodeCount++, messageNode);
        }
    }

    private String getNodeUrl(OpenLMessage message, ProjectModel model) {
        String url = null;
        
        MessageHandler messageHandler = messageHandlers.get(message.getClass());
        
        url = messageHandler.getSourceUrl(message, model);

        if (StringUtils.isBlank(url)) {
            url = messageHandler.getUrlForEmptySource(message);
        }

        return url;
    }

}
