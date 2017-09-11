package life.genny.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;

public class SecureResources {
	private static String keycloackFile = "./realm/localhost";
	
	/**
	 * @return the keycloakJsonMap
	 */
	public static Map<String, String> getKeycloakJsonMap() {
		return keycloakJsonMap;
	}

	private static Map<String, String> keycloakJsonMap = new HashMap<String, String>();
	private static String hostIP = System.getenv("HOSTIP")!=null?System.getenv("HOSTIP"):"127.0.0.1";

	/**
	 * @param keycloakJsonMap the keycloakJsonMap to set
	 * @return
	 */
	public static Future<Void> setKeycloakJsonMap(final Vertx vertx) {
		final Future<Void> fut = Future.future();
		vertx.executeBlocking(exec ->{
		// Load in keycloakJsons
		  readFilenamesFromDirectory("./realm", keycloakJsonMap);
	// update afterwrads
//			vertx.fileSystem().readFile(keycloackFile, d -> {
//				if (!d.failed()) {
//					try {
//						String keycloakJsonText = d.result().toString().replaceAll("localhost", hostIP);
//						keycloakJsonMap.put("localhost", keycloakJsonText);
//						System.out.println(keycloakJsonMap);
//						fut.complete();
//					} catch (DecodeException dE) {
//
//					}
//				} else {
//					System.err.println("Error reading ? file!");
//				}
//			});
		}, res -> {
		});
		return fut;
	}
	
	   private static void readFilenamesFromDirectory(final String rootFilePath, final Map<String, String> keycloakJsonMap) {
	        final File folder = new File(rootFilePath);
	        final File[] listOfFiles = folder.listFiles();

	        for (int i = 0; i < listOfFiles.length; i++) {
	            if (listOfFiles[i].isFile()) {
	                System.out.println("File " + listOfFiles[i].getName());
	                try {
	                    String keycloakJsonText = getFileAsText(listOfFiles[i]);
	                    // Handle case where dev is in place with localhost
	                    final String localIP = System.getenv("HOSTIP");
	                    keycloakJsonText = keycloakJsonText.replaceAll("localhost", localIP);
	                    keycloakJsonMap.put(listOfFiles[i].getName(), keycloakJsonText);
	                } catch (final IOException e) {
	                    // TODO Auto-generated catch block
	                    e.printStackTrace();
	                }

	            } else if (listOfFiles[i].isDirectory()) {
	                System.out.println("Directory " + listOfFiles[i].getName());
	                readFilenamesFromDirectory(listOfFiles[i].getName(), keycloakJsonMap);
	            }
	        }
	    }

	    private static String getFileAsText(final File file) throws IOException {
	        final BufferedReader in = new BufferedReader(new FileReader(file));
	        String ret = "";
	        String line = null;
	        while ((line = in.readLine()) != null) {
	            ret += line;
	        }
	        in.close();

	        return ret;
	}
}
