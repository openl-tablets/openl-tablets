package org.openl.studio.projects.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Per-request memoization for the project listing.
 *
 * <p>Listing a page maps every project independently, yet a few inputs are constant for the whole
 * request: the workspace dependency-name index used to resolve project dependencies, and whether the
 * user can deploy to any production repository. Recomputing them per project turns an O(N) listing
 * into O(N&#178;). This request-scoped holder computes each value once and reuses it.
 */
@Component
@RequestScope
public class ProjectListingContext {

    private Object dependencyIndex;
    private Object usedByIndex;
    private Boolean canDeployToAnyRepository;
    private Boolean canCreateInAnyRepository;
    private final Map<String, String> repositoryTypes = new HashMap<>();
    private final Map<String, Boolean> branchMembership = new HashMap<>();

    @FunctionalInterface
    public interface CheckedSupplier<T, E extends Exception> {
        T get() throws E;
    }

    /** The workspace dependency index, computed once per request. */
    @SuppressWarnings("unchecked")
    public <T> T dependencyIndex(Supplier<T> compute) {
        if (dependencyIndex == null) {
            dependencyIndex = compute.get();
        }
        return (T) dependencyIndex;
    }

    /** The reverse dependency index, computed once per request. */
    @SuppressWarnings("unchecked")
    public <T, E extends Exception> T usedByIndex(CheckedSupplier<T, E> compute) throws E {
        if (usedByIndex == null) {
            usedByIndex = compute.get();
        }
        return (T) usedByIndex;
    }

    /** Whether the user can deploy to any production repository, computed once per request. */
    public boolean canDeployToAnyRepository(BooleanSupplier compute) {
        if (canDeployToAnyRepository == null) {
            canDeployToAnyRepository = compute.getAsBoolean();
        }
        return canDeployToAnyRepository;
    }

    /**
     * The configured type of a repository, read once per repository per request.
     *
     * <p>Every project of a page repeats its repository, and reading the type parses the repository
     * settings, so the answer is remembered rather than parsed per row.
     */
    public String repositoryType(String repositoryId, UnaryOperator<String> compute) {
        return repositoryTypes.computeIfAbsent(repositoryId, compute);
    }

    /**
     * Whether a branch holds a project, computed once per request for each project and branch asked about.
     *
     * <p>Every project of a listing resolves its dependencies against the same few branches, so the same
     * question comes back for row after row.
     */
    public boolean branchHoldsProject(String repositoryId,
                                      String name,
                                      String branch,
                                      BooleanSupplier compute) {
        return branchMembership.computeIfAbsent(repositoryId + ':' + name + ':' + branch,
                key -> compute.getAsBoolean());
    }

    /** Whether the user can create a project in any repository, computed once per request. */
    public boolean canCreateInAnyRepository(BooleanSupplier compute) {
        if (canCreateInAnyRepository == null) {
            canCreateInAnyRepository = compute.getAsBoolean();
        }
        return canCreateInAnyRepository;
    }
}
