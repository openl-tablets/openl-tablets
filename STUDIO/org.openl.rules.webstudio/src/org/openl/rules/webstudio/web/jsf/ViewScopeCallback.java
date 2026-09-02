package org.openl.rules.webstudio.web.jsf;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ViewScopeCallback {
    @Getter
    private final String name;

    private final Runnable callback;

    private boolean called;

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
