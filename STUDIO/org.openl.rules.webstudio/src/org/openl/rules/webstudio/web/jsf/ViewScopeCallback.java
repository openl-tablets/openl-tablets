package org.openl.rules.webstudio.web.jsf;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ViewScopeCallback {
    private final String name;

    private final Runnable callback;

    private boolean called = false;

    public String getName() {
        return name;
    }

    void run() {
        if (!called) {
            try {
                callback.run();
            } finally {
                called = true;
            }
        }
    }
}
