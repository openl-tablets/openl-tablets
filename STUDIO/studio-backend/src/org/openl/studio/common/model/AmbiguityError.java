package org.openl.studio.common.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import org.openl.studio.common.ApiExceptionControllerAdvice;
import org.openl.studio.common.exception.AmbiguityException;

/**
 * The answer to a request that could mean several things: the error, and the candidates the client chooses from.
 *
 * @see AmbiguityException
 * @see ApiExceptionControllerAdvice
 */
public final class AmbiguityError extends BaseError {

    @Getter
    @Parameter(description = "What the request could mean. The client chooses one of them")
    private final @NonNull List<?> candidates;

    /**
     * @param error      the code and the message of the error
     * @param candidates what the request could mean
     */
    public AmbiguityError(BaseError.@NonNull Builder error, @NonNull List<?> candidates) {
        super(error);
        this.candidates = List.copyOf(candidates);
    }
}
