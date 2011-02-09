/**
 * 
 */
package org.apache.poi.ss.util;

/**
 * @author vabramovs
 *
 */
public class PathUtils {
	public static String hewExtension(String file){
		if(file.indexOf(".")>0){
			return file.substring(0,file.lastIndexOf(".")).toUpperCase();
			}
		else{
			return file.toUpperCase();
		}
	}
   public static String extractFile(String path){
		// Extract file from possible absolute path and resolve it from resource
		// TODO - need solution when path to resource is more complex
		
		String file = path;
		for(int i=0;i<2;i++){
			int lastpos  = file.lastIndexOf(new String("/\\").charAt(i));
			if(lastpos != (-1))
				 file = file.substring(lastpos+1);
		}
      return hewExtension(file);
   }
}
