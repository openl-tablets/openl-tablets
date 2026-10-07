package org.openl.studio.common.exception;

import java.util.List;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A request that could mean several things, so the client has to choose one of them.
 *
 * <p>It is answered as a conflict that lists the candidates beside its message. A client offers them as they
 * are rather than reading them out of the text.
 */
@ResponseStatus(code = HttpStatus.CONFLICT)
public class AmbiguityException extends RestRuntimeException {

    @Getter
    private final transient @NonNull List<?> candidates;

    public AmbiguityException(@NonNull String code, @NonNull List<?> candidates, Object... args) {
        super(code, args);
        this.candidates = List.copyOf(candidates);
    }
}
