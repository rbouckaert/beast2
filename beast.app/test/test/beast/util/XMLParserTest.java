package test.beast.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import org.junit.Test;

import beast.base.evolution.substitutionmodel.JukesCantor;
import beast.base.parser.XMLParser;
import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.launcher.BEASTVersion;
import junit.framework.TestCase;

public class XMLParserTest extends TestCase {

	
	// Note that this test must run in a separate class from XMLTest
	// since the XMLParser globally imports the class map
	// so if another test were run before this one, the class map 
	// (which this test temporarily changes) will not be picked up		
    @Test
    public void testClassMap() throws IOException {
    	String versionFile = null;
    	System.setProperty("beast.user.package.dir", System.getProperty("user.dir")+ "/NONE");
    	
        // make sure output goes to test directory
        File testDir =  new File(System.getProperty("user.dir")+ "/NONE");
        if (!testDir.exists()) {
             testDir.mkdir();
        }
    	for (String dir : PackageManager.getBeastDirectories()) {
    		if (dir.contains("NONE")) {
    	    	versionFile = dir+"/version.xml";
    		}
    	};
    	// back up version.xml 
    	if (new File(versionFile).exists())    	
    		Files.move(new File(versionFile).toPath(), 
        		new File("version.xml.backup").toPath(), 
        		java.nio.file.StandardCopyOption.REPLACE_EXISTING);

    	
    	// create new version.xml
    	PrintStream out = new PrintStream(new File(versionFile));
    	out.println("<package name='BEAST' version='" + new BEASTVersion().getVersion() + "'>");
    	out.println("<map from='beast.base.evolution.substitutionmodel.JoMamma' to='beast.base.evolution.substitutionmodel.JukesCantor'/>");
    	out.println("</package>");
    	out.close();
    	
    	// parse XML containing entry in map
    	Object o = null;
    	try {
	    	String xml = "<beast namespace=\"beast.base.evolution.substitutionmodel:beast.base.evolution.likelihood\" version=\"2.7\">"
	    			+ "<input spec='JoMamma'/>"
	    			+ "</beast>";
	    
	    	XMLParser parser = new XMLParser();
    		o = parser.parseBareFragment(xml, false);
    	} catch (Throwable e) {
    		e.printStackTrace();
    	}

    	// restore version.xml
    	if (new File("version.xml.backup").exists())    	
    		Files.move(new File("version.xml.backup").toPath(), 
        		new File(versionFile).toPath(), 
        		java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        
        assertEquals(true, o instanceof JukesCantor);
    }
}
