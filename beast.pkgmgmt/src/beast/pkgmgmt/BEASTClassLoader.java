package beast.pkgmgmt;




import java.util.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class helps dynamically load BEAST packages
 * using the URLClassLoader mechanism used to be the default
 * class loader in Java 8, but isn't in Java 9+ any more.
 * 
 * It requires package developers to use 
 * BEASTClassLoader.forName() instead of Class.forName().
 * and BEASTClassLoader.classLoader.getResource to access 
 * resources (like images) from jar files instead of
 * ClassLoader.getResource
 */
public class BEASTClassLoader extends URLClassLoader {
//
//	
//	// singleton class loader
//	static public BEASTClassLoader classLoader;// = new BEASTClassLoader(new URL[0], BEASTClassLoader.class.getClassLoader());
//
//    private BEASTClassLoader(ClassLoader parent) {
//        super(new URL[]{}, parent);
//    }
//	/**
//	 * Class loader should only be created by the singleton BEASTClassLoader.classLoader
//	 * so keep this private
//	 */
//    private BEASTClassLoader(URL[] urls, ClassLoader parent) {
//            super(urls, parent);
//    }
//
//    /** dynamically load jars **/
//    @Override
//    public void addURL(URL url) {
//    	super.addURL(url);
//    }
//
//    /** dynamically load jars **/
//    public void addJar(String jarFile) {
//    	System.err.println("Attempting to load " + jarFile);
//    	// TODO: fix this
//        File file = new File(jarFile);
//        if (file.exists()) {
//        	System.err.println("found file " + jarFile);
//                try {
//                    URL url = file.toURI().toURL();
//                    classLoader.addURL(url);
//                    System.err.println("Loaded " + url);
//                } catch (MalformedURLException e) {
//                        e.printStackTrace();
//                }
//        }
//    }
//   
//    
//    /**
//     *  The BEAST package alternative for Class.forName().
//     *  The latter won't work for loading classes from packages from BEAST v2.6.0 onwards. 
//     * **/
//	public static Class<?> forName(String className) throws ClassNotFoundException {
//		if (classLoader ==  null) {
//			Path [] paths = new Path[]{
////					Paths.get("build/dist/beast.base.jar"),
////					Paths.get("build/dist/beast.app.jar"),
////					Paths.get("build/dist/json.jar"),
////					Paths.get("build/dist/commons-math.jar"),
//			};
//			ModuleFinder moduleFinder = ModuleFinder.of(paths);
//			
//	        //Create a new Configuration for a new module layer deriving from the boot configuration, and resolving
//	        //the "my.implementation" module.
//	        var cfg = ModuleLayer.boot().configuration().resolve(moduleFinder ,ModuleFinder.of(),Set.of("beast.base", "beast.app"));
//	        
//	        //Create classloader
//	        // var mcl = new URLClassLoader(new URL[] {new URL("file:///tmp/mymodule.jar")});        
//	        var mcl = new URLClassLoader(new URL[] {});
//	        
//	        // make the module layer, using the configuration and classloader.        
//	        ModuleLayer ml = ModuleLayer.boot().defineModulesWithOneLoader(cfg, mcl);
//	        classLoader = new BEASTClassLoader(ml.findLoader("beast.base"));
//		}
//		
//		// System.err.println("Loading: " + className);
//		try { 
//			return Class.forName(className, false, classLoader);
//		} catch (NoClassDefFoundError e2) {
//			throw new ClassNotFoundException(e2.getMessage());
//		}
//	}
	
	
		static Map<String, MultiParentURLClassLoader> package2classLoaderMap = new HashMap<>();
	
		// singleton class loader
		static final public BEASTClassLoader classLoader = new BEASTClassLoader(new URL[0], BEASTClassLoader.class.getClassLoader());

		/**
		 * Class loader should only be created by the singleton BEASTClassLoader.classLoader
		 * so keep this private
		 */
	    private BEASTClassLoader(URL[] urls, ClassLoader parent) {
	            super(urls, parent);
	    }

	    /** dynamically load jars **/
	    @Override
	    public void addURL(URL url) {
	    	super.addURL(url);
	    }

	    public void addURL(URL url, String packageName) {
	    	if (!package2classLoaderMap.containsKey(packageName)) {
		    	package2classLoaderMap.put(packageName, new MultiParentURLClassLoader(new URL[0], null));
	    	}
	    	MultiParentURLClassLoader loader = package2classLoaderMap.get(packageName);
	    	loader.addURL(url);
	    }
	    
	    /** dynamically load jars **/
	    public void addJar(String jarFile) {
	        File file = new File(jarFile);
	        if (file.exists()) {
	        	System.err.println("found file " + jarFile);
	            try {
	                URL url = file.toURI().toURL();
	                super.addURL(url);
	                System.err.println("Loaded " + url);
	            } catch (MalformedURLException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    
	    public void addJar(String jarFile, String packageName) {
	    	System.err.println("Attempting to load " + jarFile);
	    	if (!package2classLoaderMap.containsKey(packageName)) {
		    	package2classLoaderMap.put(packageName, new MultiParentURLClassLoader(new URL[0], null));
	    	}
	    		
	    	MultiParentURLClassLoader loader = package2classLoaderMap.get(packageName);
	    	loader.addURL(jarFile);
	    } 	
	   
	    /**
	     *  The BEAST package alternative for Class.forName().
	     *  The latter won't work for loading classes from packages from BEAST v2.6.0 onwards. 
	     * **/
		public static Class<?> forName(String className) throws ClassNotFoundException {
			// System.err.println("Loading: " + className);
			for (MultiParentURLClassLoader loader : package2classLoaderMap.values()) {
				try { 
					return Class.forName(className, false, loader);
				} catch (NoClassDefFoundError e) {
					// ignore -- assume another loader contains the class
				}
			}
			
			try { 
				return Class.forName(className, false, BEASTClassLoader.classLoader);
			} catch (NoClassDefFoundError e) {
				throw new ClassNotFoundException(e.getMessage());
			}
		}	
		
		
		
		/**
		 * Return set of services provided by all packages
		 * @param service: class identifying the service
		 * @return set of services found
		 */
		public static Set<?> loadService(Class<?> service) {
			Set<Object> classes = new HashSet<>();
			for (MultiParentURLClassLoader loader : package2classLoaderMap.values()) {
				Iterable<?> services = java.util.ServiceLoader.load(service, loader);
		        for (Object d : services) {
		        	classes.add(d);
		        }
			}

			Iterable<?> services = java.util.ServiceLoader.load(service, BEASTClassLoader.classLoader);
	        for (Object d : services) {
	        	classes.add(d);
	        }
	       
			return classes;
		}

}
