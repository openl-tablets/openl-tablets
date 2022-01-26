package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ErrorsContainer {
    private final List<String> permanentErrors;
    private final List<String> requestErrors;
    private final int errorsLimit;

    public ErrorsContainer(int errorsLimit) {
        this.errorsLimit = errorsLimit;
        this.permanentErrors = new ArrayList<>();
        this.requestErrors = new ArrayList<>();
    }

    public void addPermanentError(String message) {
        if (canAdd() && !permanentErrors.contains(message)) {
            permanentErrors.add(message);
        }
    }

    public void addPermanentErrors(List<String> errors) {
        Iterator<String> iterator = errors.iterator();
        while (canAdd() && iterator.hasNext()) {
            addPermanentError(iterator.next());
        }
    }

    public void addRequestError(Exception e) {
        String message = e.getMessage();

        if (canAdd() && !requestErrors.contains(message)) {
            requestErrors.add(message);
        }
    }

    public List<String> getErrors() {
        ArrayList<String> result = new ArrayList<>(permanentErrors);

        result.addAll(requestErrors);
        requestErrors.clear();

        return result;
    }
    
    public void clear() {
        permanentErrors.clear();
        requestErrors.clear();
    }
    
    private boolean canAdd() {
        return requestErrors.size() + permanentErrors.size() < errorsLimit;
    }
}
