package org.openl.rules.repository.git;

import java.io.File;

record GitRoot(boolean remote, File localGitRoot, boolean empty) {
}
