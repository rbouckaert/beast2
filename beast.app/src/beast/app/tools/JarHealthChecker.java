package beast.app.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import beast.app.NameSpaceInfo;
import beast.app.beauti.PriorProvider;
import beast.app.inputeditor.AlignmentImporter;
import beast.app.inputeditor.InputEditor;
import beast.app.util.OutFile;
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


@Description("Checks the health of a jar file with classes for a package")
public class JarHealthChecker extends Runnable {
	final public Input<File> jarFileInput = new Input<>("jar", "jar-file containing BEAST package classes", new File(OutFile.NO_FILE)); 
	final public Input<OutFile> outputInput = new Input<>("output", "output-file where report is stored. Use stdout if not specified.", new OutFile(OutFile.NO_FILE)); 

	
	private PrintStream out = System.out;
//	private Set<String> classesInJar;
//	private Map<String, Set<String>> declaredServices;
	
	public JarHealthChecker() {}
	public JarHealthChecker(File jarFile) {
		initByName("jar", jarFile);
	}
	
	@Override
	public void initAndValidate() {
	}

	@Override
	public void run() throws Exception {
		if (OutFile.isSpecified(outputInput.get())) {
			out = new PrintStream(outputInput.get());
		}
		File jarFile = jarFileInput.get();	
		Set<String> classesInJar = collectClasses(jarFile);  
		Map<String, Set<String>> declaredServices = getDeclaredServices(jarFile);
		checkServices(classesInJar, out, declaredServices);
		
		if (OutFile.isSpecified(outputInput.get())) {
			out = new PrintStream(outputInput.get());
		}

		if (OutFile.isSpecified(outputInput.get())) {
			out.close();
		}
		Log.warning("Done");
	}
	
	private Set<String> collectClasses(File file) {
		Set<String> classesInJar = new HashSet<>();				
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
						report("Class " + name + " has multiple entries in jar files");
					}
					name = name.substring(0, name.length() - 6).replaceAll("/", ".");
					classesInJar.add(name);
				}
			}
			zip.close();
		} catch (IOException e) {
			report(e.getMessage());
		}	
		return classesInJar;
	}
	
	private Map<String, Set<String>> getDeclaredServices(File file) {
		Map<String, Set<String>> declaredServices = new HashMap<>();
		String destDir = Utils.isWindows() ? "\\temp\\" :"/tmp/" + file + "_extracted";
		new File(destDir).mkdir();
		try {
			PackageManager.doUnzip(file.getAbsolutePath(), destDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		File serviceDir = new File(destDir +"/META-INF/services");
		if (!serviceDir.exists()) {
			report("No services found in jar " + file);
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
		return declaredServices;				
	}
	
	
	public void checkServices(Set<String> classesInJar, PrintStream out, Map<String, Set<String>> declaredServices) {
		report("Checking services");
		boolean serviceDeclarationMissing = false;

		if (declaredServices.size() == 0) {
			report("No declared services found. If there are any classes that are one of these:\n"
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
					report("Service " + service + " declared with class " + className + " but class could not be found in any jar "
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
						report("Expected class " + className + " to be declared as service beast.base.evolution.datatype.DataType");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof NameSpaceInfo) {
					if (hasDeclaredService(declaredServices, "beast.pkgmgmt.NameSpaceInfo", className)) {
						report("Expected class " + className + " to be declared as service beast.pkgmgmt.NameSpaceInfo");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof InputEditor) {
					if (hasDeclaredService(declaredServices, "beast.app.inputeditor.InputEditor", className)) {
						report("Expected class " + className + " to be declared as service beast.app.inputeditor.InputEditor");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof AlignmentImporter) {
					if (hasDeclaredService(declaredServices, "beast.app.inputeditor.AlignmentImporter", className)) {
						report("Expected class " + className + " to be declared as service beast.app.inputeditor.AlignmentImporter");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof PriorProvider) {
					if (hasDeclaredService(declaredServices, "beast.app.beauti.PriorProvider", className)) {
						report("Expected class " + className + " to be declared as service beast.app.beauti.PriorProvider");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof NameSpaceInfo) {
					if (hasDeclaredService(declaredServices, "beast.pkgmgmt.NameSpaceInfo", className)) {
						report("Expected class " + className + " to be declared as service beast.pkgmgmt.NameSpaceInfo");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof ModelLogger) {
					if (hasDeclaredService(declaredServices, "beast.base.inference.util.ModelLogger", className)) {
						report("Expected class " + className + " to be declared as service beast.base.inference.util.ModelLogger");
						serviceDeclarationMissing = true;
					}
				} else if (o instanceof BEASTInterface) {
					if (hasDeclaredService(declaredServices, "beast.base.core.BEASTInterface", className)) {
						report("Expected class " + className + " to be declared as service");
						serviceDeclarationMissing = true;
					}
				}
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				// e.printStackTrace();
				if (!className.contains("$")) { // ignore problems with inner classes
				report("Could not instantiate class " + className + " through default constructor "
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
		report("\n\nTo declare services in a jar file, in the build.xml file,inside the 'jar' element that creates the jar file, "
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


	
	private void report(String msg) {
		out.println(msg);
	}

	public static void main(String[] args) throws Exception {
		new Application(new JarHealthChecker(), "Jar Health Checker", args);
	}
}
