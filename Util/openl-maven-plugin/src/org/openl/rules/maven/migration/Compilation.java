package org.openl.rules.maven.migration;

import java.util.Collection;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.message.OpenLMessage;

/**
 * Compiles the project as it is in its folder and keeps the messages of the compilation until a migrator rewrites
 * the rules, so the migrators that only read them compile the project once.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
public final class Compilation {

    private final Supplier<Collection<OpenLMessage>> compiler;
    private @Nullable Collection<OpenLMessage> messages;

    /**
     * The messages of the compilation, its errors included.
     */
    Collection<OpenLMessage> messages() {
        if (messages == null) {
            messages = compiler.get();
        }
        return messages;
    }

    /**
     * Tells that the rules changed, so the next call of {@link #messages()} compiles the project again.
     */
    void changed() {
        messages = null;
    }
}
