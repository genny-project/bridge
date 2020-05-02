package life.genny.bridge;


import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import life.genny.channel.VersionHandler;
//import life.genny.channel.RouterHandlers;
import life.genny.metrics.Metrics;
import life.genny.qwandautils.GennySettings;



public class BridgeRouters {
	  protected static final Logger log = org.apache.logging.log4j.LogManager
		      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

  
	static private Router bridgeRouter = null;
 
  protected static void routers(final Vertx vertx) {
	  bridgeRouter = Router.router(vertx);  // create new router
	  
	  BridgeRouterHandlers.avertx = vertx;

	  bridgeRouter.route().handler(BridgeRouterHandlers.cors());

	  bridgeRouter.route("/frontend/*").handler(BridgeHandler.eventBusHandler(vertx));
	  bridgeRouter.route(HttpMethod.GET, "/api/events/init").handler(BridgeRouterHandlers::apiGetInitHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/events/init").handler(BridgeRouterHandlers::apiInitHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/service").handler(BridgeRouterHandlers::apiServiceHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/service/sync").handler(BridgeRouterHandlers::apiSyncHandler);   // old mobile
	  bridgeRouter.route(HttpMethod.POST, "/v7/api/service/sync").handler(BridgeRouterHandlers::apiSync2Handler); // mobile v7

	  bridgeRouter.route(HttpMethod.GET, "/api/pull/:key").handler(BridgeRouterHandlers::apiGetPullHandler);

	  bridgeRouter.route(HttpMethod.GET, "/version").handler(BridgeRouterHandlers::apiGetVersionHandler);
      bridgeRouter.route(HttpMethod.GET, "/health").handler(BridgeRouterHandlers::apiGetHealthHandler);

	  bridgeRouter.route(HttpMethod.POST, "/api/cmds").handler(BridgeRouterHandlers::apiHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/data").handler(BridgeRouterHandlers::apiHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/devices").handler(BridgeRouterHandlers::apiDevicesHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/virtualbus").handler(BridgeRouterHandlers::virtualEventBusHandler);
    
	  bridgeRouter.route(HttpMethod.GET, "/metrics").handler(Metrics::metrics);
	log.info("Activating Bridge Routes on port "+GennySettings.apiPort+" given ["+GennySettings.apiPort+"]");
	
	HttpServerOptions serverOptions = new HttpServerOptions();
	  serverOptions.setUsePooledBuffers(true);
	  serverOptions.setCompressionSupported(true);
	  serverOptions.setCompressionLevel(3);

	  serverOptions.setUseAlpn(true);
	
	vertx.createHttpServer(/*serverOptions*/).requestHandler(bridgeRouter::accept).listen(Integer.parseInt(GennySettings.apiPort));

  }

}
