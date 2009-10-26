import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;

import junit.framework.TestCase;

import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.util.FileTreeIterator;

public class TestPoi extends TestCase
{
	
	static String[] exceptions = {
		"C:\\Documents and Settings\\Administrator\\Templates\\excel.xls",
		"C:\\Documents and Settings\\Administrator\\Templates\\excel4.xls",
		"C:\\Documents and Settings\\Default User\\Templates\\excel.xls",
		"C:\\Documents and Settings\\Default User\\Templates\\excel4.xls",
		"C:\\Documents and Settings\\StanislavS\\Local Settings\\Temp\\Functional Template.xls",
	};
	
	public static void main(String[] args) throws IOException
	{
		
		File ff = new File("/temp/__cvs");
		
		long mod = ff.lastModified();
		
		long now = System.currentTimeMillis();
		
		System.out.println((now-mod));
		

		FileTreeIterator it = new FileTreeIterator(new File("c:/")
				.getCanonicalFile(), 0);

		int nd = 0, nf = 0;
		Calendar cal = Calendar.getInstance();

		for (; it.hasNext();)
		{
			File f = (File) it.next();
			
			long fmod = f.lastModified();

			
			
			
			if (now - fmod < (600 * 1000L ))
			{	
				cal.setTimeInMillis(fmod);
				System.err.print(f.getAbsolutePath());
				System.err.println( " - " + f.getName() + " - " +  cal.getTime());
				++nf;
			}	
			
				
				
//			String abs = f.getAbsolutePath();
			if (f.isDirectory())
			{
				++nd;
				if (nd % 1000 == 0)
				{	
					System.err.println("" + nd + " : " + nf);
					System.err.println(f.getAbsolutePath());
				}	
			} 
//			else
//			{
//				
//				if (f.getName().equals("cvspass"))
//				{	
//					System.err.println(f.getAbsolutePath());
//					++nf;
//				}	
//				
//				}
			}
		}

	
	
	public static void main2(String[] args) throws IOException
	{
		HashSet exc = new HashSet();
		for (int i = 0; i < exceptions.length; i++)
		{
			exc.add(exceptions[i]);
		}
		
		FileTreeIterator it = new FileTreeIterator(new File("c:/").getCanonicalFile(), 0);
	
	int nd = 0, nf = 0;	
		
	for(;it.hasNext();)
	{
		File f = (File)it.next();
		String abs = f.getAbsolutePath();
		if (f.isDirectory())
		{	
			++nd;
			if (nd % 100 == 0)
			   System.err.println("" + nd + " : " + nf);
		}	
		else 
		{
			++nf;
			if (f.getAbsolutePath().endsWith(".xls"))
			{
				System.err.println(" ++++   " + abs);
				if (abs.indexOf("C:\\Documents and Settings") >= 0)
					continue;
				if (abs.endsWith("excel.xls"))
					continue;
				if (abs.endsWith("excel4.xls"))
					continue;
				if (exc.contains(abs))
					continue;
				
				
				XlsWorkbookSourceCodeModule xwbs = new XlsWorkbookSourceCodeModule(new FileSourceCodeModule(f, null));
				xwbs.getWorkbook();
			}	
		}		
	}


}
	
	
}	
