package life.genny.security;

import java.util.HashMap;
import java.util.Map;
import io.vertx.core.json.DecodeException;
import io.vertx.rxjava.core.Vertx;

public class SecureResources {
	private static String keycloackFile = "./realm/localhost";
	
	/**
	 * @return the keycloakJsonMap
	 */
	public static Map<String, String> getKeycloakJsonMap() {
		return keycloakJsonMap;
	}
	

	/**
	 * @param keycloakJsonMap the keycloakJsonMap to set
	 */
	public static void setKeycloakJsonMap(Map<String, String> keycloakJsonMap) {
		SecureResources.keycloakJsonMap = keycloakJsonMap;
	}
	

	private static Map<String, String> keycloakJsonMap = new HashMap<String, String>();
	private static String hostIP = System.getenv("HOSTIP")!=null?System.getenv("HOSTIP"):"127.0.0.1";

	public static void getFile(Vertx vertx) {
		vertx.executeBlocking(exec ->{
			vertx.fileSystem().readFile(keycloackFile, d -> {
				if (!d.failed()) {
					try {
						String keycloakJsonText = d.result().toString().replaceAll("localhost", hostIP);
						keycloakJsonMap.put("localhost", keycloakJsonText);
					} catch (DecodeException dE) {	
						
					}	
				} else {
					System.err.println("Error reading data.json file!");
				}
			});
		}, res -> {
		});
	}
}
