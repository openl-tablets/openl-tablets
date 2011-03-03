package org.openl.rules.repository;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.openl.rules.repository.exceptions.RRepositoryException;

public class GetFileVersion {
    public static void main(String[] args) {
        RRepository repository = null;
        // CommonUser user = new CommonUserImpl("user1");

        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();

            RProject p1 = repository.getProject("p1");
            RFile file1 = p1.getRootFolder().getFolders().get(0).getFiles().get(0);

            List<RVersion> vers = file1.getVersionHistory();
            for (RVersion v : vers) {
                out.println("  " + v.getVersionName());

                printContent(file1, v);
            }

        } catch (Exception e) {
            System.err.println("*** Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (repository != null) {
                repository.release();
            }
        }
    }

    private static void printContent(RFile file, RVersion version) throws RRepositoryException, IOException {
        InputStream content = null;

        try {
            content = file.getContent4Version(version);

            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            while (reader.ready()) {
                String s = reader.readLine();
                out.println("   >" + s);
            }
        } finally {
            if (content != null) {
                content.close();
            }
        }
        out.println("   >LOB=" + file.getLineOfBusiness());
    }
}
