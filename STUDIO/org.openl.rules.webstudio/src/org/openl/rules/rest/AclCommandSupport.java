package org.openl.rules.rest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import org.openl.security.acl.permission.AclPermission;
import org.openl.util.StringUtils;

public class AclCommandSupport {
    public static final String MSG1 = "Each line must contains 3 parts for 'list' or 'listAll' and 6 parts for 'add', 'set', or 'remove' action. Format: 'command:repo type/repo id:resource[:sid type:sid name or group id:permissions]'.";
    public static final String MSG2 = "Permissions list is an empty.";
    public static final String MSG3 = "Permission %s is not supported for repository type '%s'.";
    public static final String MSG4 = "Expected empty resource path, because concrete repository is not defined.";
    public static final String MSG5 = "Expected 'add' or 'remove' command, but found '%s'.";
    public static final String MSG6 = "Expected 'user', 'group' or 'groupId' for sid type, but found '%s'.";
    public static final String MSG7 = "Expected repository type one of: 'design', 'prod' or 'deployConfig', but found '%s'.";

    public enum Action {
        ADD,
        SET,
        LIST,
        LIST_ALL,
        REMOVE
    }

    public enum SidType {
        USERNAME,
        GROUP_NAME,
        GROUP_ID;
    }

    public enum RepoType {
        DESIGN("design"),
        PROD("prod"),
        DEPLOY_CONFIG("deployConfig");

        private final String name;

        RepoType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class AclCommand {
        Action action;
        SidType sidType;
        String sid;
        RepoType repoType;
        String repo;
        String resource;
        String[] permissions;

        public AclCommand(Action action,
                SidType sidType,
                String sid,
                RepoType repoType,
                String repo,
                String resource,
                String[] permissions) {
            this.action = action;
            this.sidType = sidType;
            this.sid = sid;
            this.repoType = repoType;
            this.repo = repo;
            this.resource = resource;
            this.permissions = permissions;
        }

        public String getResourceString() {
            if (StringUtils.isBlank(repo)) {
                return repoType.toString().toLowerCase();
            } else {
                String r = repoType.toString().toLowerCase() + "/" + repo;
                if (StringUtils.isBlank(resource)) {
                    return r;
                } else {
                    return r + resource;
                }
            }
        }
    }

    public static Collection<AclPermission> listAllSupportedPermissions(RepoType repoType) {
        if (RepoType.DESIGN == repoType) {
            return AclPermission.ALL_SUPPORTED_DESIGN_REPO_PERMISSIONS;
        } else if (RepoType.DEPLOY_CONFIG == repoType) {
            return AclPermission.ALL_SUPPORTED_DEPLOY_CONFIG_REPO_PERMISSIONS;
        } else if (RepoType.PROD == repoType) {
            return AclPermission.ALL_SUPPORTED_PROD_REPO_PERMISSIONS;
        } else {
            throw new IllegalArgumentException("Unknown repo type: " + repoType);
        }
    }

    public static AclCommand toCommand(String line, String deployConfigRepo) throws CommandFormatException {
        String[] split = line.split(":");
        if (line.endsWith(":")) {
            String[] t = new String[split.length + 1];
            System.arraycopy(split, 0, t, 0, split.length);
            t[t.length - 1] = "";
            split = t;
        }
        if (split.length <= 1) {
            throw new CommandFormatException(MSG1);
        }
        Action action = toAction(split[0].trim());
        if (split.length != 3 && (Action.LIST == action || Action.LIST_ALL == action) || split.length != 6 && (Action.ADD == action || Action.REMOVE == action || Action.SET == action)) {
            throw new CommandFormatException(MSG1);
        }
        RepoType repoType = toRepoType(split[1].trim());
        String repo = toRepo(split[1].trim());
        if (StringUtils.isBlank(repo) && !StringUtils.isBlank(deployConfigRepo) && RepoType.DEPLOY_CONFIG == repoType) {
            repo = deployConfigRepo;
        }
        String resource = split[2].trim();
        if (repo == null && !resource.isEmpty()) {
            throw new CommandFormatException(MSG4);
        }
        if (Action.ADD == action || Action.REMOVE == action || Action.SET == action) {
            SidType sidType = toSidType(split[3].trim());
            String sid = split[4].trim();
            String[] permissions = split[5].trim().split(",");
            permissions = Arrays.stream(permissions)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);
            if (permissions.length == 0 && Action.SET != action) {
                throw new CommandFormatException(MSG2);
            }
            for (String permission : permissions) {
                if (listAllSupportedPermissions(repoType).stream()
                    .noneMatch(e -> Objects.equals(AclPermission.toString(e), permission.toUpperCase()))) {
                    throw new CommandFormatException(String.format(MSG3, permission, repoType.getName()));
                }
            }
            return new AclCommand(action, sidType, sid, repoType, repo, resource, permissions);
        } else {
            return new AclCommand(action, null, null, repoType, repo, resource, null);
        }
    }

    private static String toRepo(String repo) {
        int index = repo.indexOf("/");
        if (index > 0) {
            repo = repo.substring(index + 1).trim();
        } else {
            return null;
        }
        if (repo.length() == 0) {
            return null;
        }
        return repo;
    }

    private static Action toAction(String command) throws CommandFormatException {
        if ("add".equalsIgnoreCase(command)) {
            return Action.ADD;
        } else if ("set".equalsIgnoreCase(command)) {
            return Action.SET;
        } else if ("remove".equalsIgnoreCase(command)) {
            return Action.REMOVE;
        } else if ("list".equalsIgnoreCase(command)) {
            return Action.LIST;
        } else if ("listAll".equalsIgnoreCase(command)) {
            return Action.LIST_ALL;
        } else {
            throw new CommandFormatException(String.format(MSG5, command));
        }
    }

    private static SidType toSidType(String sidType) throws CommandFormatException {
        switch (sidType.toLowerCase()) {
            case "user":
                return SidType.USERNAME;
            case "group":
                return SidType.GROUP_NAME;
            case "groupid":
                return SidType.GROUP_ID;
            default:
                throw new CommandFormatException(String.format(MSG6, sidType));
        }
    }

    private static RepoType toRepoType(String type) throws CommandFormatException {
        int index = type.indexOf("/");
        if (index > 0) {
            type = type.substring(0, index).trim();
        }
        if (RepoType.DESIGN.getName().equalsIgnoreCase(type)) {
            return RepoType.DESIGN;
        } else if (RepoType.PROD.getName().equalsIgnoreCase(type)) {
            return RepoType.PROD;
        } else if (RepoType.DEPLOY_CONFIG.getName().equalsIgnoreCase(type)) {
            return RepoType.DEPLOY_CONFIG;
        } else {
            throw new CommandFormatException(String.format(MSG7, type));
        }
    }
}
