package beast.app.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import beast.app.NameSpaceInfo;
import beast.app.beauti.PriorProvider;
import beast.app.inputeditor.AlignmentImporter;
import beast.app.inputeditor.InputEditor;
import beast.app.util.Utils;
import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.datatype.DataType;
import beast.base.inference.ModelLogger;
import beast.base.inference.Runnable;
import beast.base.util.FileUtils;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.Version;
import beast.pkgmgmt.launcher.BEASTVersion;
import cern.colt.Arrays;

@Description("Does sanity checks on a BEAST package\n" 
		+ "o make sure all classes implementing services are registered as services\n"
		+ "o make sure folders are in the right place (lib, template, examples)\n"
		+ "o make sure source code is available and matches jar library\n"
		+ "o make sure version.xml is present\n"
		+ "and more...")
public class PackageHealthChecker extends Runnable {
	final public Input<File> packageInput = new Input<>("package", "zip-file containing BEAST pacakge", new File("[[none]]")); 
	
	private String packageName;
	private String packageFileName;
	private String packageDir;
	private Set<String> classesInJar = null;
	
	@Override
	public void initAndValidate() {
	}

	@Override
	public void run() throws Exception {
		if (packageInput.get() == null || packageInput.get().getName().equals("[[none]]")) {
			throw new IllegalArgumentException("package zip file must be specified");
		}
		if (!packageInput.get().exists()) {
			throw new IllegalArgumentException("package zip file (" + packageInput.get().getPath() + ") does not exist");
		}
		
		
		// unzip the package file
		packageFileName = packageInput.get().getName();
		if (packageFileName.contains(".")) {
			packageFileName = packageFileName.substring(0, packageFileName.lastIndexOf('.'));
		}
		packageDir = (Utils.isWindows() ? "c:\\temp\\" : "/tmp/") + packageFileName;
		new File(packageDir).mkdir();
		PackageManager.doUnzip(packageInput.get().getPath(), packageDir);
		
		
		// do checks
		checkVersionFile();
		checkServices();
		checkFolders();
		checkSourceCode();
		
		// clean up package directory
		deleteRecursively(new File(packageDir));
		Log.info("Done.");
		System.exit(0);
	}
	
	private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteRecursively(f);
            }
        }
        file.delete();            
    }


	private void checkServices() {
		Log.info("Checking services");
		boolean serviceDeclarationMissing = false;

		Map<String,Set<String>> declaredServices = collectDecladedServices();
		if (declaredServices.size() == 0) {
			Log.info("No declared services found. If there are any classes that are one of these:\n"
					+ "o beast.base.core.BEASTInterface\n"
					+ "o beast.base.evolution.datatype.DataType\n"
					+ "o beast.base.inference.util.ModelLogger\n"
					+ "o beast.pkgmgmt.NameSpaceInfo\n"
					+ "o beast.app.inputeditor.InputEditor\n"
					+ "o beast.app.inputeditor.AlignmentImporter\n"
					+ "o beast.app.beauti.PriorProvider\n"
					+ "probably a service should be declared."
					);
			serviceDeclarationMissing = true;
		}

		// check all services declared are actually in this package
		for (String service : declaredServices.keySet()) {
			for (String className : declaredServices.get(service)) {
				if (!classesInJar.contains(className)) {
					Log.info("Service " + service + " declared with class " + className + " but class could not be found in any jar "
							+ "(possibly a typo in the class name used to declare the class in the build.xml file)");
				}
			}
		}
		
		// check all classes that are known services are declared as service
		for (String className: classesInJar) {
			try {
				Object o = BEASTClassLoader.forName(className).newInstance();
				if (o instanceof DataType) {
					if (hasDeclaredService(declaredServices, "beast.base.evolution.datatype.DataType", className)) {
						Log.info("Expected class " + className + " to be declared as service beast.base.evolution.datatype.DataType");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof NameSpaceInfo) {
					if (hasDeclaredService(declaredServices, "beast.pkgmgmt.NameSpaceInfo", className)) {
						Log.info("Expected class " + className + " to be declared as service beast.pkgmgmt.NameSpaceInfo");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof InputEditor) {
					if (hasDeclaredService(declaredServices, "beast.app.inputeditor.InputEditor", className)) {
						Log.info("Expected class " + className + " to be declared as service beast.app.inputeditor.InputEditor");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof AlignmentImporter) {
					if (hasDeclaredService(declaredServices, "beast.app.inputeditor.AlignmentImporter", className)) {
						Log.info("Expected class " + className + " to be declared as service beast.app.inputeditor.AlignmentImporter");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof PriorProvider) {
					if (hasDeclaredService(declaredServices, "beast.app.beauti.PriorProvider", className)) {
						Log.info("Expected class " + className + " to be declared as service beast.app.beauti.PriorProvider");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof NameSpaceInfo) {
					if (hasDeclaredService(declaredServices, "beast.pkgmgmt.NameSpaceInfo", className)) {
						Log.info("Expected class " + className + " to be declared as service beast.pkgmgmt.NameSpaceInfo");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof ModelLogger) {
					if (hasDeclaredService(declaredServices, "beast.base.inference.util.ModelLogger", className)) {
						Log.info("Expected class " + className + " to be declared as service beast.base.inference.util.ModelLogger");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof BEASTInterface) {
					if (hasDeclaredService(declaredServices, "beast.base.core.BEASTInterface", className)) {
						Log.info("Expected class " + className + " to be declared as service");
						serviceDeclarationMissing = true;
					}
				}
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				// e.printStackTrace();
				if (!className.contains("$")) { // ignore problems with inner classes
				Log.info("Could not instantiate class " + className + " through default constructor "
						+ "-- skipping service check, but this might be an undeclared service");				
				}
			}
		}
		if (serviceDeclarationMissing) {
			showServiceInfo();
		}
	}

	private boolean hasDeclaredService(Map<String,Set<String>> declaredServices, String service, String className) {
		Set<String> services = declaredServices.get(service);		
		if (services == null) {
			return false;
		}
		return services.contains(className);
	}

	private void showServiceInfo() {
		Log.info("\n\nTo declare services in a jar file, in the build.xml file,inside the 'jar' element that creates the jar file, "
				+ "add a 'service' element with appropriate type attribute to the build.xml file, and 'provider' elements for each class "
				+ "that provides the service. For example, the DataType services in beast.base are declared like so:\n"
				+ "    <service type=\"beast.base.evolution.datatype.DataType\">\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.Aminoacid\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.Nucleotide\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.TwoStateCovarion\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.Binary\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.IntegerData\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.StandardData\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.UserDataType\"/>\n"
				+ "    </service>\n");
	}

	private Map<String, Set<String>> collectDecladedServices() {
		Map<String,Set<String>> declaredServices = new HashMap<>();		
		for (String file : new File(packageDir + "/lib").list()) {
			if (file.toLowerCase().endsWith(".jar")) {
				String destDir = packageDir + "/lib/" + file + "_extracted";
				new File(destDir).mkdir();
				try {
					PackageManager.doUnzip(packageDir + "/lib/" + file, destDir);
				} catch (IOException e) {
					e.printStackTrace();
				}
				File serviceDir = new File(destDir +"/META-INF/services");
				if (!serviceDir.exists()) {
					Log.info("No services found in jar " + file);
				} else {
					for (String metaInfFile : serviceDir.list()) {
						if (!metaInfFile.endsWith("MANIFEST.MF") && !(metaInfFile.charAt(0)=='.')) {
							if (!declaredServices.containsKey(metaInfFile)) {
								declaredServices.put(metaInfFile, new HashSet<String>());
							}
							try {
								for (String className : FileUtils.load(metaInfFile).split("\n")) {
									declaredServices.get(metaInfFile).add(className);
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		return declaredServices;
	}

	private void checkFolders() {
		Log.info("Checking folder structure");
		boolean hasExamples = false;
		int exampleCount = 0;
		boolean hasLib = false;
		int libCount = 0;
		boolean hasTemplates = false;
		int templateCount = 0;
		
		String [] files = new File(packageDir).list();
		for (String fileName : files) {
			if (fileName.toLowerCase().equals("examples")) {
				hasExamples = true;
				for (String example : new File(packageDir+"/examples").list()) {
					if (example.toLowerCase().endsWith("xml") || example.toLowerCase().endsWith("json")) {
						exampleCount++;
					}
				}
			}
			if (fileName.toLowerCase().equals("lib")) {
				hasLib = true;
				for (String example : new File(packageDir+"/lib").list()) {
					if (example.toLowerCase().endsWith("jar")) {
						libCount++;
					}
				}
			}
			if (fileName.toLowerCase().equals("templates")) {
				hasTemplates = true;
				for (String example : new File(packageDir+"/templates").list()) {
					if (example.toLowerCase().endsWith("xml")) {
						templateCount++;
					}
				}
			}
		}		
		if (!hasExamples) {
			Log.info("No examples directory found. It is recommended to have at least one XML example file "
					+ "showing the features of the package in the examples directory at the top level of the package");
		}
		if (hasExamples && exampleCount == 0) {
			Log.info("No examples in examples directory found. It is recommended to have at least one XML example "
					+ "file showing the features of the package");
		}
		if (!hasLib) {
			Log.info("No lib directory found. It is recommended to have at least one jar file containing java classes "
					+ "in the lib directory at the top level of the package");
		}
		if (hasLib && libCount == 0) {
			Log.info("No jar library found in lib directory.");
		}
		if (!hasTemplates) {
			Log.info("No templates directory found. For BEAUti support of the package, the BEAUti templates are "
					+ "expected to be in the templates directory at the top level of the package");
		}
		if (hasTemplates && templateCount == 0) {
			Log.info("No BEAUti template found in templates directory, so BEAUti will have no support for this package.");
		}
	}

	private void checkSourceCode() {
		Log.info("Checking source code file");
		if (classesInJar == null) {
			collectClasses();
		}
		// collect source file names
		Set<String> sourceClassFiles = new HashSet<>();
		for (String fileName : new File(packageDir).list()) {
			if (fileName.endsWith("jar") || fileName.endsWith("zip")) {
				try {
					ZipInputStream zip = new ZipInputStream(new FileInputStream(packageDir + "/" + fileName));
					while(true) {
					    ZipEntry e = zip.getNextEntry();
					    if (e == null)
					      break;
					    String name = e.getName();
					    if (name.toLowerCase().endsWith("java")) {
					    	sourceClassFiles.add(name);
					    }
					}
					zip.close();
				} catch (IOException e) {
					Log.info(e.getMessage());
				}
			}
		}
		if (sourceClassFiles.size() == 0) {
			Log.info("Source code file expected (jar or zip) at top level as perhaps " + packageName+ ".src.jar, but no source files found");
			return;
		}
		
		// check source file names have associated classes
		for (String sourceFile : sourceClassFiles) {
			String clazz = sourceFile.replaceAll(".java", "").replaceAll("/", ".");
			if (!classesInJar.contains(clazz)) {
				Log.info("Source file " + sourceFile + " in source jar but class file " + clazz + " was not in class jar");
			}
		}

		// check class file names have associated sources
		for (String clazz : classesInJar) {
			if (!clazz.contains("$")) {
				String sourceFile = clazz.replaceAll("\\.", "/") + ".java";
				if (!sourceClassFiles.contains(sourceFile)) {
					Log.info("Class file " + clazz + " in class jar but source file " + sourceFile + " was not in source jar");
				}
			}
		}
	}

	private void checkVersionFile() {
		Log.info("Checking version.xml");
		String versionFileName = packageDir + "/version.xml";
		if (!new File(versionFileName).exists()) {
			Log.info("Expected file version.xml at top level in the zip file");
			// perhaps it is somewhere else in the zip file?
			try {
				ZipInputStream zip = new ZipInputStream(new FileInputStream(packageInput.get()));
				while(true) {
				    ZipEntry e = zip.getNextEntry();
				    if (e == null)
				      break;
				    String name = e.getName();
				    if (name.endsWith("version.xml")) {
				    	Log.info("but found it here: " + name);
				    }
				}
			} catch (IOException e) {
				Log.info(e.getMessage());
			}
			Log.info("Could not find file version.xml where I expected it: " + versionFileName);
			Log.info("Cannot determine package name, so assume it is " + packageFileName);
			packageName = packageFileName;
			return;
		}
		
		packageName = determinPackageName(versionFileName);
		
		checkDependenciesInVersionXML(versionFileName);
		checkApplicationsInVersionXML(versionFileName);
		checkMapElementsInVersionXML(versionFileName);
	}
	
	private String determinPackageName(String versionFileName) {
		packageName = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // find name of package
            Document doc = factory.newDocumentBuilder().parse(versionFileName);
            Element packageElement = doc.getDocumentElement();
            if (packageElement.getNodeName().equals("addon")) {
            	Log.info("Deprecated top level element 'addon' found. Use 'package' instead.");
            }
            packageName = packageElement.getAttribute("name");
            String version = packageElement.getAttribute("version");
            if (version == null || version.length() == 0) {
    			Log.info("Excpected version attribute containing package version to be specified on the root element of version.xml");
            } else {
            	if (version.split("\\.").length != 3) {
        			Log.info("The version attribute containing package version on the root element of version.xml does not appear to be in standard <major-version>.<minor-version>.<bug-fix-version>, e.g \"2.7.0\"");
        			Log.info("More info on semantic versioning: https://semver.org/");
            	}
            }
            
            NodeList content = packageElement.getChildNodes();
            for (int i = 0; i < content.getLength(); i++) {
            	Node node = content.item(i);
            	if (node instanceof Element) {
            		String name = ((Element) node).getNodeName();
            		if (!(name.equals("packageapp") || name.equals("map") || name.equals("addonapp") || name.equals("depends"))) {
            			Log.info("Unrecognised element found in version.xml, which will be ignored:" + name + " (potentially a typo)");
            		}
            	}
            }
            
        } catch (SAXException e) {
            // too bad, won't print out any info
			Log.info("Excpected  " + versionFileName + " to be an XML file with name attribute containing the package name on the root element");
        } catch (IOException e) {
        	Log.info("Cannot read version.xml file: " + e.getMessage());
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

        if (packageName == null) {
			Log.info("Cannot determine package name, so assume it is " + packageFileName);
			packageName = packageFileName;
        }
		return packageName;
	}

	private void checkDependenciesInVersionXML(String versionFileName) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            doc = factory.newDocumentBuilder().parse(new File(versionFileName));
            doc.normalize();
            // get package-app info from version.xml
            NodeList nodes = doc.getElementsByTagName("depends");
            if (nodes.getLength() == 0) {
            	Log.info("No package dependencies specified in version.xml.");
            	Log.info("At least one dependency of the form <depends on='beast2' version='" + BEASTVersion.INSTANCE.getVersion() + "'/> was expected.");
            	return;
            }
            for (int j = 0; j < nodes.getLength(); j++) {
                Element packageAppElement = (Element) nodes.item(j);
                NamedNodeMap atts = packageAppElement.getAttributes();
                for (int i = 0; i < atts.getLength(); i++) {
                	String name = atts.item(i).getNodeName();
                	if (!(name.equals("on") || name.equals("version")|| name.equals("atleast")|| name.equals("atmost"))) {
                		Log.info("Unrecognised attributes " + name + " found in 'depends' element: use 'on' 'version', 'atleast' or 'atmost'");
                	}
                }
                
                String onName = packageAppElement.getAttribute("on");
                if (onName == null || onName.length() == 0) {
                	Log.info("depends element found, but 'on' attribute is not specified");
                }
                String versionName = packageAppElement.getAttribute("version");
                String versionAtLeast = packageAppElement.getAttribute("atleast");
                String versionAtMost = packageAppElement.getAttribute("atmost");
                if ((versionName == null || versionName.length() == 0) &&
                		(versionAtLeast == null || versionAtLeast.length() == 0)&&
                		(versionAtMost == null || versionAtMost.length() == 0)) {
                	Log.info("depends element found, but 'version', 'atleast' or 'atmost' attribute is not specified");
                } else if (versionAtMost != null && versionAtLeast != null && versionAtMost.length() > 0 && versionAtLeast.length() > 0) {
                	try {
                		if (BEASTVersion.parseVersion(versionAtMost) < BEASTVersion.parseVersion(versionAtLeast)) {
                			Log.info("The atmost attribute must be larger than the atleast attribute, but: " + versionAtLeast + " > " + versionAtMost);                		
                		}
                	} catch (Throwable e) {
                		Log.info("There may be an ill-formatted version in a 'depends' element: "+ versionAtLeast + " or " + versionAtMost);
                	}
                }
            }
        } catch (Exception e) {
            // ignore
            System.err.println(e.getMessage());
        }
	}

	private void checkApplicationsInVersionXML(String versionFileName) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            doc = factory.newDocumentBuilder().parse(new File(versionFileName));
            doc.normalize();
            // get package-app info from version.xml
            NodeList nodes = doc.getElementsByTagName("packageapp");
            if (nodes.getLength() == 0) {
            	nodes = doc.getElementsByTagName("addonapp");
                if (nodes.getLength() != 0) {
                	Log.info("Deprecated element name 'addonapp' used in version.xml. Use 'packageapp' instead");
                }
            }
            if (nodes.getLength() == 0) {
            	Log.info("Observation: No package applications found in version.xml so no apps will be available for the BEAST applauncher");
            	Log.info("More information about applauncher:");
            	Log.info("http://www.beast2.org/2014/08/04/beast-apps-for-the-appstore.html");
            	Log.info("http://www.beast2.org/2019/07/23/better-apps-for-the-beast-appstore.html");
            	return;
            }
            for (int j = 0; j < nodes.getLength(); j++) {
                Element packageAppElement = (Element) nodes.item(j);
                Set<String> recognisedAttributes = new HashSet<>();
                recognisedAttributes.add("class");
                recognisedAttributes.add("description");
                recognisedAttributes.add("args");
                recognisedAttributes.add("icon");
                NamedNodeMap atts = packageAppElement.getAttributes();
                for (int i = 0; i < atts.getLength(); i++) {
                	String name = atts.item(i).getNodeName();
                	if (!recognisedAttributes.contains(name)) {
                		Log.info("Unrecognised attributes " + name + " found: use one of " + Arrays.toString(recognisedAttributes.toArray()));
                	}
                }
                
                String className = packageAppElement.getAttribute("class");
                if (className == null || className.length() == 0) {
                	Log.info("packageapp element found, but class attribute is not specified");
                } else {
                	if (!jarFileContainsClass(className)) {
                    	Log.info("class " + className +" specified in packageapp of version.xml could not be found in lib/" + packageName + ".jar");
                	}
                }
                
                String description = packageAppElement.getAttribute("description");
                if (description == null || description.length() == 0) {
                	Log.info("packageapp element found, but no description provided: please specify the description attribute");
                }
                String argumentsString = packageAppElement.getAttribute("args");

                String iconLocation = packageAppElement.getAttribute("icon");
                if (iconLocation == null || iconLocation.length() == 0) {
                	Log.info("packageapp element found, but no icon provided (no or empty icon attribute) so default icon will be used in BEAST applauncher");
                }
            }
        } catch (Exception e) {
            // ignore
            System.err.println(e.getMessage());
        }
    }
	
	private void checkMapElementsInVersionXML(String versionFileName) {
		try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document doc = factory.newDocumentBuilder().parse(new File(versionFileName));
            doc.normalize();
            NodeList nodes = doc.getElementsByTagName("map");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element map = (Element) nodes.item(i);
                
                NamedNodeMap atts = map.getAttributes();
                for (int j = 0; j < atts.getLength(); j++) {
                	String name = atts.item(i).getNodeName();
                	if (!(name.equals("from") || name.equals("to"))) {
                		Log.info("Unrecognised attributes " + name + " found: must be \"from\" or \"to\"");
                	}
                }
                
                String fromClass = map.getAttribute("from");
                if (fromClass == null || fromClass.length() == 0) {
                	Log.info("The 'from' attribute in map element in version.xml must be specified");
                }
                String toClass = map.getAttribute("to");
                if (toClass == null || toClass.length() == 0) {
                	Log.info("The 'from' attribute in map element in version.xml must be specified");
                }
        		if (!jarFileContainsClass(toClass)) {
                	Log.info("class " + toClass +" specified in map element of version.xml could not be found in lib/" + packageName + ".jar");
            	}
            }
        } catch (ParserConfigurationException|SAXException|IOException e) {
            e.printStackTrace();
        }        		

	}

	private boolean jarFileContainsClass(String toClass) {
		if (classesInJar == null) {
			collectClasses();
		}
		return classesInJar.contains(toClass);
	}
	
	private void collectClasses() {
		classesInJar = new HashSet<>();
		File jardir = new File(packageDir + "/lib");
		if (!jardir.exists()) {
			Log.info("Expected lib directory at top level of zip file containing jar files with classes, but could not find any");
			return;
		}
		for (File file : jardir.listFiles()) {
			if (file.getName().toLowerCase().endsWith("jar")) {
				try {
					BEASTClassLoader.classLoader.addJar(file.getPath());
				} catch (Throwable t) {
					// ignore
				}
				try {
					ZipInputStream zip = new ZipInputStream(new FileInputStream(file));
					while(true) {
						ZipEntry e = zip.getNextEntry();
						if (e == null)
							break;
						String name = e.getName();
						if (name.endsWith("class")) {
							if (classesInJar.contains(name)) {
								Log.info("Class " + name + " has multiple entries in jar files");
							}
							name = name.substring(0, name.length() - 6).replaceAll("/", ".");
							classesInJar.add(name);
						}
					}
					zip.close();
				} catch (IOException e) {
					Log.info(e.getMessage());
				}	
			}
		}
	}

	public static void main(String[] args) throws Exception {
		new Application(new PackageHealthChecker(), "Package Health Checker", args);
	}

}
