package life.genny.bridge;

import java.io.IOException;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.vertx.rxjava.ext.web.RoutingContext;
import life.genny.channel.DistMap;
import life.genny.cluster.ClusterConfig;
import life.genny.utils.PropertiesJsonDeserializer;

public class VersionHandler {
	 private static final Gson gson = new GsonBuilder()
		        .registerTypeAdapter(Properties.class, PropertiesJsonDeserializer.getPropertiesJsonDeserializer())
		        .create();
	  
	  public static void apiGetVersionHandler(final RoutingContext routingContext) {
		    routingContext.request().bodyHandler(body -> {
		            Properties properties = new Properties();
		            System.out.println("Here it is: "+DistMap.getDistBE().size());
		            try {
		              properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties")
		                  .openStream());
		            } catch (IOException e) {
		              // TODO Auto-generated catch block
		              e.printStackTrace();
		            }

		            
		          routingContext.response().putHeader("Content-Type", "application/json");
		          routingContext.response().end(gson.toJson(properties));

		    });
		  }
}
