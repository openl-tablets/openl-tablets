package org.openl.rules.workspace;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.openl.util.HashingUtils;

/**
 * Makes the ids projects are addressed by.
 *
 * <p>An id is the first 72 bits of the SHA-256 digest of where a project is stored — its repository and its folder
 * there — written as 12 characters of URL-safe base64. Neither the name, the branch nor the revision is part of it,
 * so a project keeps its id while it stays in its folder: opened or closed, on every branch, after a rename in
 * {@code rules.xml}, after a restart and on every Studio instance.
 *
 * <p>The folder is the internal path of the project in a repository with folders, the key it is stored under in a
 * repository without folders, and the workspace folder of a local project. It is read with {@code /} separators,
 * without a leading or trailing one, and normalized to Unicode NFC, so a file system that reports decomposed names
 * gives the id a Git repository gives.
 *
 * <p>Two folders whose identities share a digest are told apart when a collection of projects is indexed: the one
 * whose identity sorts first keeps the plain id, and the others add a number to their identity. The numbers last
 * until the server restarts. One folder listed twice is one project and keeps its id.
 *
 * @author Yury Molchan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class ProjectIds {

    private static final String VERSION = "v1";
    /** The unit separator, which joins the fields of an identity; no repository id or path contains it. */
    private static final char SEPARATOR = '\u001F';
    private static final int ID_BYTES = 9;
    private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{12}");

    /** The ids of the identities found sharing a digest with another one, by identity. */
    private static final Map<String, ProjectId> NUMBERED = new ConcurrentHashMap<>();
    /** The identities found listed twice, warned about once until the server restarts. */
    private static final Set<String> LISTED_TWICE = ConcurrentHashMap.newKeySet();

    /**
     * The id of the project stored in the given folder of the given repository.
     *
     * @param repositoryId the id of the repository, {@code local} for a project of the user's workspace only
     * @param folder       where the project is stored in the repository
     */
    public static ProjectId of(String repositoryId, String folder) {
        return idOf(identity(repositoryId, folder), ProjectIds::digest);
    }

    /**
     * Whether the text has the form of an id: 12 characters of URL-safe base64. A text of another form names no
     * project.
     */
    public static boolean isId(String text) {
        return ID.matcher(text).matches();
    }

    /**
     * The id the text writes, when the text has the form of one.
     *
     * @see #isId(String)
     */
    public static Optional<ProjectId> parse(String text) {
        return isId(text) ? Optional.of(new ProjectId(text)) : Optional.empty();
    }

    /**
     * Indexes the given projects by their ids.
     *
     * <p>Projects whose identities share a digest are numbered here, in the order of their identities, so that each
     * one is addressed by an id of its own from now on, by {@link #of(String, String)} too.
     *
     * <p>A folder listed more than once is one project, not several whose ids collide: it keeps its id, the first of
     * its entries is indexed, and a warning names the folder the first time.
     *
     * @param projects   the projects to index
     * @param locationOf where a project is stored
     * @return the projects by their ids
     */
    public static <T> Map<ProjectId, T> index(Collection<T> projects, Function<T, ProjectKey> locationOf) {
        return index(projects, locationOf, ProjectIds::digest);
    }

    static <T> Map<ProjectId, T> index(Collection<T> projects,
                                       Function<T, ProjectKey> locationOf,
                                       Function<String, ProjectId> digest) {
        var identities = new TreeMap<String, T>();
        for (var project : projects) {
            var location = locationOf.apply(project);
            var identity = identity(location.repositoryId(), location.repositoryPath());
            // No listing is expected to hold a folder twice: name it once, so the listing that does can be found.
            if (identities.putIfAbsent(identity, project) != null && LISTED_TWICE.add(identity)) {
                log.warn("The folder '{}' of the repository '{}' is listed twice. Its first entry is indexed.",
                        location.repositoryPath(),
                        location.repositoryId());
            }
        }

        var index = HashMap.<ProjectId, T>newHashMap(identities.size());
        for (var entry : identities.entrySet()) {
            var identity = entry.getKey();
            var id = idOf(identity, digest);
            var number = 1;
            while (index.containsKey(id)) {
                number++;
                id = digest.apply(identity + SEPARATOR + "~" + number);
            }
            if (number > 1) {
                NUMBERED.put(identity, id);
            }
            index.put(id, entry.getValue());
        }
        return index;
    }

    private static ProjectId idOf(String identity, Function<String, ProjectId> digest) {
        var numbered = NUMBERED.get(identity);
        return numbered != null ? numbered : digest.apply(identity);
    }

    static String identity(String repositoryId, String folder) {
        return VERSION + SEPARATOR + repositoryId + SEPARATOR + normalize(folder);
    }

    private static String normalize(String folder) {
        return Normalizer.normalize(StringUtils.strip(folder.replace('\\', '/'), "/"), Normalizer.Form.NFC);
    }

    private static ProjectId digest(String identity) {
        return new ProjectId(BASE64.encodeToString(Arrays.copyOf(HashingUtils.sha256(identity), ID_BYTES)));
    }
}
