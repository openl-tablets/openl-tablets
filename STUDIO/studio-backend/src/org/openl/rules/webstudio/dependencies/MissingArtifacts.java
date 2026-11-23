package org.openl.rules.webstudio.dependencies;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilter;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.transfer.ArtifactFilteredOutException;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * The artifacts and metadata the Maven repository answered it does not have, which are not asked for again for a
 * while.
 *
 * <p>Every resolution asks the repository for what it misses: each {@code pom.xml} of a walk names the same version
 * no pipeline publishes, such as a {@code -SNAPSHOT} of a module of the same build, and a repository group answers it
 * is missing only after asking each repository it groups. So a missing one is asked for once a period, not once a
 * resolution.
 *
 * <p>Nothing is recorded on disk: an artifact published meanwhile is found once the period is over, as the answers
 * kept in memory are renewed. A failure to transfer, such as a timeout, is not recorded.
 *
 * @author Yury Molchan
 */
final class MissingArtifacts extends AbstractRepositoryListener implements RemoteRepositoryFilterSource {

    private static final RemoteRepositoryFilter.Result ACCEPTED = new Answer(true, "");

    private final Clock clock;
    private final Duration period;
    /** Until when each missing one is not asked for again, by its repository and its coordinates. */
    private final Map<String, Instant> missing = new ConcurrentHashMap<>();

    MissingArtifacts(Clock clock, Duration period) {
        this.clock = clock;
        this.period = period;
    }

    /** Each repository that answered it does not have the artifact. */
    @Override
    public void artifactResolved(RepositoryEvent event) {
        for (var e : event.getExceptions()) {
            // A missing one refused here is not missing anew: its period is not prolonged
            if (e instanceof ArtifactNotFoundException notFound && !(e instanceof ArtifactFilteredOutException)
                    && notFound.getRepository() != null) {
                record(keyOf(notFound.getRepository(), notFound.getArtifact()));
            }
        }
    }

    @Override
    public void metadataResolved(RepositoryEvent event) {
        if (event.getException() instanceof MetadataNotFoundException notFound && notFound.getRepository() != null) {
            record(keyOf(notFound.getRepository(), notFound.getMetadata()));
        }
    }

    @Override
    public RemoteRepositoryFilter getRemoteRepositoryFilter(RepositorySystemSession session) {
        return new RemoteRepositoryFilter() {
            @Override
            public Result acceptArtifact(RemoteRepository repository, Artifact artifact) {
                return accept(keyOf(repository, artifact));
            }

            @Override
            public Result acceptMetadata(RemoteRepository repository, Metadata metadata) {
                return accept(keyOf(repository, metadata));
            }
        };
    }

    /** Forgets every missing one, so each is asked for again. */
    void clear() {
        missing.clear();
    }

    private void record(String key) {
        var now = clock.instant();
        // A missing one no resolution asks for again is forgotten with the next one found missing
        missing.values().removeIf(until -> !until.isAfter(now));
        missing.put(key, now.plus(period));
    }

    private RemoteRepositoryFilter.Result accept(String key) {
        var until = missing.get(key);
        if (until == null) {
            return ACCEPTED;
        }
        if (!until.isAfter(clock.instant())) {
            missing.remove(key, until);
            return ACCEPTED;
        }
        return new Answer(false, "The Maven repository did not have it a while ago.");
    }

    private static String keyOf(ArtifactRepository repository, Artifact artifact) {
        return repository.getId() + "|" + ArtifactIdUtils.toBaseId(artifact);
    }

    private static String keyOf(ArtifactRepository repository, Metadata metadata) {
        return "%s|%s:%s:%s:%s".formatted(repository.getId(),
                metadata.getGroupId(),
                metadata.getArtifactId(),
                metadata.getVersion(),
                metadata.getType());
    }

    private record Answer(boolean isAccepted, String reasoning) implements RemoteRepositoryFilter.Result {
    }
}
