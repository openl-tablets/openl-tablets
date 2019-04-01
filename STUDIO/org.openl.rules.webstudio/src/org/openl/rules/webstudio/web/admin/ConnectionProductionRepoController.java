package org.openl.rules.webstudio.web.admin;

import java.io.File;
import java.io.IOException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.openl.util.StringUtils;

/**
 * @author Pavel Tarasevich
 *
 */

@ManagedBean
@ViewScoped
public class ConnectionProductionRepoController extends AbstractProductionRepoController {

    @Override
    public void save() {
        RepositoryConfiguration repoConfig = createRepositoryConfiguration();

        if (isInputParamInvalid(repoConfig)) {
            return;
        }

        if (!checkConnection(repoConfig)) {
            return;
        }

        // repoConfig.save();
        addProductionRepoToMainConfig(repoConfig);
        clearForm();
    }

    private boolean checkConnection(RepositoryConfiguration repoConfig) {
        setErrorMessage("");

        if (getRepositoryConfiguration().getType().equals("local")) {
            return checkLocalRepo(repoConfig);
        } else {
            return checkRemoteConnection(repoConfig);
        }
    }

    private boolean checkRemoteConnection(RepositoryConfiguration repoConfig) {
        try {
            RepositoryValidators.validateConnection(repoConfig, getProductionRepositoryFactoryProxy());
            return true;
        } catch (RepositoryValidationException e) {
            setErrorMessage(e.getMessage());
            return false;
        }
    }

    private boolean checkLocalRepo(RepositoryConfiguration repoConfig) {
        CommonRepositorySettings settings = (CommonRepositorySettings) repoConfig.getSettings();
        File repoDir = new File(settings.getPath());
        String errorMessage = "There is no repository in this folder. Please, correct folder path";
        if (repoDir.exists()) {
            File[] files = repoDir.listFiles();
            RepoDirChecker checker = new RepoDirChecker(settings.getPath());
            if (files == null) {
                setErrorMessage(errorMessage);
                return false;
            }

            for (File file : files) {
                try {
                    checker.check(file.getCanonicalPath());
                } catch (IOException e) {
                    setErrorMessage(errorMessage);
                    return false;
                }
            }

            if (!checker.isRepoThere()) {
                setErrorMessage(errorMessage);
                return false;
            }

            if (StringUtils.isNotEmpty(settings.getLogin())) {
                try {
                    RepositoryValidators.validateConnection(repoConfig, getProductionRepositoryFactoryProxy());
                } catch (RepositoryValidationException e) {
                    setErrorMessage(e.getMessage());
                    return false;
                }
            }
        } else {
            setErrorMessage(errorMessage);
            return false;
        }

        this.setChecked(true);
        return true;
    }

    public static class RepoDirChecker {
        private String root;
        private boolean hasRepoDir = false;
        private boolean hasVersionDir = false;
        private boolean hasWorkSpacesDir = false;

        public RepoDirChecker(String root) {
            this.root = root;
        }

        public void check(String str) {
            String subFolder = str.toLowerCase().replace(root.toLowerCase(), "");

            checkRepoDir(subFolder);
            checkVersionDir(subFolder);
            checkWorkSpacesDir(subFolder);
        }

        private void checkRepoDir(String dir) {
            if (dir.startsWith("repository") || dir.startsWith("\\repository")) {
                hasRepoDir = true;
            }
        }

        private void checkVersionDir(String dir) {
            if (dir.startsWith("version") || dir.startsWith("\\version")) {
                hasVersionDir = true;
            }
        }

        private void checkWorkSpacesDir(String dir) {
            if (dir.startsWith("workspaces") || dir.startsWith("\\workspaces")) {
                hasWorkSpacesDir = true;
            }
        }

        public boolean isRepoThere() {
            return hasRepoDir && hasVersionDir && hasWorkSpacesDir;
        }
    }
}
