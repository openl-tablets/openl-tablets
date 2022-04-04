package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class contains errors shown in Repository tab.
 * It contains 2 types of errors: permanent errors (for example Repository misconfiguration issue) and errors occurred
 * during current request (for example temporary connection loss during expanding repository tree).
 * Request errors are cleared on each getErrors() call (they are for one request only).
 * Both Permanent and Request errors are cleared when clear() is invoked.
 */
public final class ErrorsContainer {
    private final List<String> permanentErrors;
    private final List<String> requestErrors;
    private final int errorsLimit;

    /**
     * Create errors container.
     *
     * @param errorsLimit maximum number of errors to show to a user.
     */
    public ErrorsContainer(int errorsLimit) {
        this.errorsLimit = errorsLimit;
        this.permanentErrors = new ArrayList<>();
        this.requestErrors = new ArrayList<>();
    }

    /**
     * Add permanent error (for example Repository misconfiguration issue).
     *
     * @param message error message to show to a user
     */
    public void addPermanentError(String message) {
        if (canAdd() && !permanentErrors.contains(message)) {
            permanentErrors.add(message);
        }
    }

    /**
     * Add permanent errors (for example Repository misconfiguration issues).
     *
     * @param errors error messages to show to a user
     */
    public void addPermanentErrors(List<String> errors) {
        Iterator<String> iterator = errors.iterator();
        while (canAdd() && iterator.hasNext()) {
            addPermanentError(iterator.next());
        }
    }

    /**
     * Add request error (for example temporary connection loss during expanding repository tree).
     *
     * @param message error message to show to a user
     */
    public void addRequestError(String message) {
        if (canAdd() && !requestErrors.contains(message)) {
            requestErrors.add(message);
        }
    }

    /**
     * Get errors to show to a user. Request errors are cleared after this method, permanent errors aren't.
     *
     * @return errors to show to a user
     */
    public List<String> getErrors() {
        ArrayList<String> result = new ArrayList<>(permanentErrors);

        result.addAll(requestErrors);
        requestErrors.clear();

        return result;
    }

    public boolean hasErrors() {
        return !permanentErrors.isEmpty() || !requestErrors.isEmpty();
    }

    /**
     * Clear both Permanent and Request errors.
     */
    public void clear() {
        permanentErrors.clear();
        requestErrors.clear();
    }
    
    private boolean canAdd() {
        return requestErrors.size() + permanentErrors.size() < errorsLimit;
    }
}
