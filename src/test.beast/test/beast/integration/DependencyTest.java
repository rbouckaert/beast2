package test.beast.integration;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.junit.Test;

import beast.base.util.FileUtils;
import junit.framework.TestCase;

/**
 * 
 * Test for java package dependencies
 * 
 */
public class DependencyTest extends TestCase {

	
	private static class StreamGobbler implements Runnable {
	    private InputStream inputStream;
	    private Consumer<String> consumer;

	    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
	        this.inputStream = inputStream;
	        this.consumer = consumer;
	    }

	    @Override
	    public void run() {
	        new BufferedReader(new InputStreamReader(inputStream)).lines()
	          .forEach(consumer);
	    }
	}
	
	@Test
	public void testMutualDependencyOfPackageDependencies() throws Exception {
		
		// obtain package dependencies in dot file by executing
		// jdeps -dotoutput /tmp/jdeps/ beast
		
		String jdepsDir = "/tmp/jdeps";
		
		System.err.println("Running jdeps...");
		if (!new File(jdepsDir).exists()) {
			new File(jdepsDir).mkdirs();
		}
		ProcessBuilder builder = new ProcessBuilder();
		builder.command("jdeps", "-dotoutput", jdepsDir+"/", "beast");
		String dir = System.getProperty("user.dir");
		if (dir.indexOf("/src/") > 0) {			
			dir = dir.substring(0, dir.indexOf("/src/"));
		}
		builder.directory(new File(dir + "/build"));
		Process process = builder.start();
		StreamGobbler streamGobbler = 
		  new StreamGobbler(process.getInputStream(), System.out::println);
		Executors.newSingleThreadExecutor().submit(streamGobbler);
		int exitCode = process.waitFor();
		assert exitCode == 0;

		// parse /tmp/jdeps/beast.dot for dependencies
		System.err.println("Processing dependencies");
		String dotFile = FileUtils.load(new File(jdepsDir + "/beast.dot"));
		Set<String> map = new HashSet<>();	
		for (String str : dotFile.split("\n")) {
			if (str.contains("->")) {
				String [] strs = str.split("->");
				String from = strs[0].trim().replaceAll("\"","");
				String to = strs[1].replaceAll("\"","");
				to = to.substring(0, to.indexOf("(")).trim();
				map.add(from + "->" + to);
			}
		}

		
		Set<String> cycles = new HashSet<>();
		
		for (String dep : map) {
			String [] str = dep.split("->");
			String from = str[0];
			String to = str[1];			
			String d = to + "->" + from;
			if (map.contains(d)) {
				if (from.compareTo(to)> 0) {
					cycles.add(from + " <-> " + to);
				} else {
					cycles.add(to + " <-> " + from);
				}
			}	
		}
		
		if (cycles.size() > 0) {
			System.err.println("Cycles found:");
			for (String cycle : cycles) {
				System.err.println(cycle);
			}
		} else {			
			System.err.println("Bravo! No cycles found");
		}
		assertEquals(cycles.size(), 0);
		
		// TODO: test for cycles longer than 2?
		
		System.err.print("Done");
		
	}
}
