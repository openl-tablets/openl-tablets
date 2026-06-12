package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Searches a mount for files and folders.
 *
 * <p>{@code SUBTREE} scope walks the mount tree and matches each entry by path pattern, extension,
 * type and a case-insensitive content substring. {@code ANCESTORS} scope walks up from the anchor
 * path to the repository root, returning the same-named file at each level — not limited to the
 * project — nearest first, each with its content.
 *
 * @author Yury Molchan
 */
@Component
@RequiredArgsConstructor
class FileSearchSupport {

    private static final long MAX_CONTENT_SEARCH_BYTES = 1024L * 1024L;

    private final AclProjectsHelper aclProjectsHelper;
    private final FileNodeMapper resourceMapper;

    List<FsNode> search(FileRoot root, FileSearchQuery query) {
        root.requireReadable();
        if (query.scope() == FileSearchQuery.Scope.ANCESTORS) {
            return searchAncestors(root, query);
        }
        Set<String> extensions = query.extensions().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        String pattern = StringUtils.isBlank(query.pattern()) ? null : query.pattern();
        AntPathMatcher matcher = pattern == null ? null : new AntPathMatcher();
        String contentNeedle = StringUtils.isBlank(query.content()) ? null : query.content().toLowerCase();

        List<FsNode> result = new ArrayList<>();
        Deque<AProjectFolder> queue = new ArrayDeque<>();
        queue.add(root.readFolder(query.version()));
        while (!queue.isEmpty()) {
            AProjectFolder folder = queue.poll();
            for (AProjectArtefact artefact : folder.getArtefacts()) {
                if (matchesSearch(artefact, query, pattern, matcher, extensions, contentNeedle)) {
                    result.add(resourceMapper.map(artefact));
                }
                if (query.recursive() && artefact.isFolder()) {
                    queue.add((AProjectFolder) artefact);
                }
            }
        }
        result.sort(FileNodeMapper.NODE_COMPARATOR);
        return result;
    }

    /**
     * Tests one artefact against the search criteria. The expensive checks (content read, ACL)
     * run last.
     */
    private boolean matchesSearch(AProjectArtefact artefact,
                                  FileSearchQuery query,
                                  String pattern,
                                  AntPathMatcher matcher,
                                  Set<String> extensions,
                                  String contentNeedle) {
        if (query.type() == FileSearchQuery.FileType.FILE && artefact.isFolder()) {
            return false;
        }
        if (query.type() == FileSearchQuery.FileType.FOLDER && !artefact.isFolder()) {
            return false;
        }
        if (!extensions.isEmpty()) {
            if (artefact.isFolder()) {
                return false;
            }
            String ext = FileUtils.getExtension(artefact.getName());
            if (ext == null || !extensions.contains(ext.toLowerCase())) {
                return false;
            }
        }
        if (matcher != null && !matcher.match(pattern, artefact.getInternalPath())) {
            return false;
        }
        if (contentNeedle != null) {
            if (artefact.isFolder()) {
                return false;
            }
            String text = readBoundedText((AProjectResource) artefact);
            if (text == null || !text.toLowerCase().contains(contentNeedle)) {
                return false;
            }
        }
        return aclProjectsHelper.hasPermission(artefact, BasePermission.READ);
    }

    /**
     * Reads UTF-8 text from a file while keeping memory use bounded. Files larger than the limit
     * (or unreadable) yield {@code null} so they are treated as a content non-match.
     */
    private static String readBoundedText(AProjectResource resource) {
        try (var in = resource.getContent()) {
            if (in == null) {
                return null;
            }
            byte[] data = in.readNBytes((int) MAX_CONTENT_SEARCH_BYTES + 1);
            if (data.length > MAX_CONTENT_SEARCH_BYTES) {
                return null;
            }
            return new String(data, StandardCharsets.UTF_8);
        } catch (ProjectException | IOException e) {
            return null;
        }
    }

    private static List<FsNode> searchAncestors(FileRoot root, FileSearchQuery query) {
        String leaf = StringUtils.isBlank(query.pattern()) ? "" : query.pattern();
        String lookupPath = StringUtils.isBlank(query.from()) ? leaf : query.from() + "/" + leaf;
        if (lookupPath.isEmpty()) {
            return List.of();
        }
        // Reject absolute paths and parent traversal before they are anchored to a mount path.
        try {
            Repository.validatePath(lookupPath);
        } catch (InvalidPathException e) {
            throw new BadRequestException("file.path.invalid.message");
        }
        return root.searchAncestors(lookupPath);
    }
}
