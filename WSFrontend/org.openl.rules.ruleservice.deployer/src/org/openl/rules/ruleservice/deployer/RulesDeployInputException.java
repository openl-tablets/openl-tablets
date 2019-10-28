package org.openl.rules.ruleservice.deployer;

public class RulesDeployInputException extends Exception {

    private static final long serialVersionUID = 1642177867400252271L;

    public RulesDeployInputException() {
        super();
    }

    public RulesDeployInputException(String message, Throwable cause) {
        super(message, cause);
    }

    public RulesDeployInputException(String message) {
        super(message);
    }

    public RulesDeployInputException(Throwable cause) {
        super(cause);
    }
}
