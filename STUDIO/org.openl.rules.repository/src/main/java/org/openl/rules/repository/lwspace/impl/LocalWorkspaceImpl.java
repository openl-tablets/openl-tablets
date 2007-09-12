package org.openl.rules.repository.lwspace.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.jcr.RepositoryException;

import org.openl.rules.repository.jcr.JcrFile;
import org.openl.rules.repository.jcr.JcrFolder;
import org.openl.rules.repository.jcr.JcrProject;
import org.openl.rules.repository.lwspace.LocalWorkspace;

/**
 * Implementation of Local Workspace.
 * 
 * @author Aleh Bykhavets
 *
 */
public class LocalWorkspaceImpl implements LocalWorkspace {
	private static final int BUFFER_SIZE = 1024 * 16;
	
	/** Temporary folder */
	private File tempFolder;
	/** OpenL Project in JCR */
	private JcrProject project;

    /** {@inheritDoc} */
	public void initialize(JcrProject project, File tempLocation) throws RepositoryException {
		if (!tempLocation.exists()) {
			// there is no temporary folder -- create it
			if (!tempLocation.mkdir()) {
				throw new RuntimeException("Failed to create temporary folder");
			}
		} else {
			// check tempLocation, it must be a folder
			if (!tempLocation.isDirectory()) {
				throw new RuntimeException("Temporary location is not a folder");
			}
		}
		
		tempFolder = tempLocation;
		this.project = project;

		// clean up... just in case
		cleanFolder(tempLocation);
		
		// downloads project files from JCR
		downloadProject();
	}

    /** {@inheritDoc} */
	public void clean() {
		cleanFolder(tempFolder);
		
		// GC clean up
		project = null;
		tempFolder = null;
	}
	
    /** {@inheritDoc} */
	public void revert() throws RepositoryException {
		cleanFolder(tempFolder);

		downloadProject();
	}

    /** {@inheritDoc} */
	public void commit() throws RepositoryException  {
		//TODO: implement commit
		// 1. delete from JCR that is absent in Local Workspace
		// 2. add new folders
		// 3. compare/upload updated/new files
//		uploadProject();
	}

	// ------ private methods ------
	
	/**
	 * Clean folder recursively.
	 * It deletes all files, sub folders and the folder itself.
	 * 
	 * @param folder root folder
	 */
	private void cleanFolder(File folder) {
		File[] files = folder.listFiles();
		
		for (File f : files) {
			if (f.isDirectory()) {
				// delete recursively
				cleanFolder(f);
			} else {
				// delete file
				f.delete();
			}
		}
		
		// delete folder itself
		folder.delete();
	}
	
	private void downloadProject() throws RepositoryException {
		JcrFolder root = project.getRootFolder();
		
		try {
			downloadJcrFolder(root, tempFolder);
		} catch (IOException e) {
			throw new RepositoryException("Failed to download project from JCR: " + e.getMessage(), e);
		}		
	}
	
	private void downloadJcrFolder(JcrFolder jcrFolder, File tempFolder) throws IOException, RepositoryException {
		List<JcrFolder> subFolders = jcrFolder.listSubFolders();
		
		for (JcrFolder subFolder : subFolders) {
			// create temporary sub folder
			String name = subFolder.getName();
			File subTemp = new File(tempFolder, name);
			subTemp.mkdir();
			// process recursively
			downloadJcrFolder(subFolder, subTemp);
		}
		
		List<JcrFile> files = jcrFolder.listFiles();
		for (JcrFile file : files) {
			String name = file.getName();
			
			File f = new File(tempFolder, name);
			writeFile(f, file);
		}
	}
	
	/**
	 * Writes file in temporary location from JCR.
	 * 
	 * @param f new file
	 * @param is input stream with content of a file
	 */
	private void writeFile(File f, JcrFile jcrFile) throws IOException {
		InputStream is = jcrFile.getInputStream();
		FileOutputStream fos = new FileOutputStream(f);
		
		byte[] buffer = new byte[BUFFER_SIZE];
		
		try {
			// transfer data
			while (true) {
				int readed = is.read(buffer);
				if (readed <= 0)
					break; // nothing to write

				fos.write(buffer, 0, readed);
			}
		} finally {
			fos.close();
			is.close();
		}		
	}
}
