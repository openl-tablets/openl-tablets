/**
 * 
 */
package com.exigen.le.project;

import com.exigen.le.project.cache.Cache;
import com.exigen.le.project.cache.CacheFactory;
import com.exigen.le.smodel.ServiceModel;

/**
 * LE Project and his versions
 * @author vabramovs
 *
 */
public class Project {
	private String name;
	private Cache<String,ProjectVersion > versions;
	/**
	 * @param versionDesc
	 * @param versions
	 */
	public Project( String name) {
		this.name = name;
		this.versions = CacheFactory.createVersionCache();
	}
	/**
	 * @return the versionDesc
	 */
	public String getName() {
		return this.name;
	}
	/**
	 * @return the versions
	 */
	public Cache<String, ProjectVersion> getVersions() {
		return versions;
	}
	
	/**
	 * @param projectVersion
	 */
	public  void addVersion(ProjectVersion projectVersion){
		versions.put(projectVersion.getVersionDesc().getVersion(), projectVersion);
	}
}
