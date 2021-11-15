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
		static Map<String, MultiParentURLClassLoader> package2classLoaderMap = new HashMap<>();
	
		// singleton class loader
		static final public BEASTClassLoader classLoader = new BEASTClassLoader(new URL[0], BEASTClassLoader.class.getClassLoader());

		
		static private Map<String, Set<String>> services = new HashMap<>();
		static private Map<String, ClassLoader> class2loaderMap = new HashMap<>();
		/**
		 * Class loader should only be created by the singleton BEASTClassLoader.classLoader
		 * so keep this private
		 */
	    private BEASTClassLoader(URL[] urls, ClassLoader parent) {
	            super(urls, parent);
	    }

	    /** dynamically load jars **/
	    @Override
	    // TODO: purge use of addURL(jarFile)
	    @Deprecated // use addURL(jarFile, packageName) instead
	    public void addURL(URL url) {
	    	super.addURL(url);
	    }

	    public void addURL(URL url, String packageName, Map<String, Set<String>> services) {
	    	MultiParentURLClassLoader loader = getClassLoader(packageName);
	    	loader.addURL(url);

	    	if (services != null) {
	    		addServices(packageName, services);
	    	}
	    	
	    }

	    public void addParent(String packageName, String parentPackage) {
    		if (parentPackage.equals(packageName)) {
    			return;
    		}
	    	MultiParentURLClassLoader loader = getClassLoader(packageName);
	    	MultiParentURLClassLoader parentLoader = getClassLoader(parentPackage);
	    	loader.addParentLoader(parentLoader);
	    }
	    
	    /** dynamically load jars **/
	    // TODO: purge use of addJar(jarFile)
	    @Deprecated // use addJar(jarFile, packageName) instead
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
	    	MultiParentURLClassLoader loader = getClassLoader(packageName);
	    	loader.addURL(jarFile);
	    } 	
	   
	    /**
	     *  The BEAST package alternative for Class.forName().
	     *  The latter won't work for loading classes from packages from BEAST v2.6.0 onwards. 
	     * **/
		public static Class<?> forName(String className) throws ClassNotFoundException {
			if (class2loaderMap.containsKey(className)) {
				ClassLoader loader = class2loaderMap.get(className);
				return Class.forName(className, false, loader);
			}
			
			System.err.println("Loading non-service: " + className);
			for (MultiParentURLClassLoader loader : package2classLoaderMap.values()) {
				try { 
					// System.err.println("Trying to load "+className+" using " + loader.name);
					return Class.forName(className, false, loader);
				} catch (NoClassDefFoundError | java.lang.ClassNotFoundException e) {
					// ignore -- assume another loader contains the class
				}
			}
			
			try { 
				// System.err.println("Trying to load using BEASTClassLoader.classLoader");
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
		public static Set<String> loadService(Class<?> service) {
			Set<String> providers = services.get(service.getName());
			if (providers == null) {
				providers = new HashSet<>();
				services.put(service.getName(), providers);				
			}
			return providers;
			
//			Set<Object> classes = new HashSet<>();
			
//			Set<Object> classes = new HashSet<>();
//			for (MultiParentURLClassLoader loader : package2classLoaderMap.values()) {
//				Iterable<?> services = java.util.ServiceLoader.load(service, loader);
//		        for (Object d : services) {
//		        	classes.add(d);
//		        }
//			}
//
//			Iterable<?> services = java.util.ServiceLoader.load(service, BEASTClassLoader.classLoader);
//	        for (Object d : services) {
//	        	classes.add(d);
//	        }
//	       
//			return classes;
		}

		public void addServices(String packageName, Map<String, Set<String>> services) {
			ClassLoader loader = getClassLoader(packageName);

	    	for (String service : services.keySet()) {
	    		if (!BEASTClassLoader.services.containsKey(service)) {	    			
	    			BEASTClassLoader.services.put(service, new HashSet<>());
	    		}
	    		Set<String> providers = BEASTClassLoader.services.get(service);
	    		providers.addAll(services.get(service));
	    		for (String provider : services.get(service)) {
	    			class2loaderMap.put(provider, loader);
	    		}
	    	}	    	
			
		}

		/*
		 * get deepest level class loader associated with a specific package
		 * identified by the package name
		 */
		private static MultiParentURLClassLoader getClassLoader(String packageName) {
	    	if (!package2classLoaderMap.containsKey(packageName)) {
		    	package2classLoaderMap.put(packageName, new MultiParentURLClassLoader(new URL[0], packageName));
		    	System.err.println("Created classloader >>" + packageName + "<<");
	    	}
	    		
	    	MultiParentURLClassLoader loader = package2classLoaderMap.get(packageName);
	    	return loader;
		}

}
