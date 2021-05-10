package life.genny.bridge;


import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.TimeoutHandler;
//import life.genny.channel.RouterHandlers;
//import life.genny.metrics.Metrics;
import life.genny.qwandautils.GennySettings;



@ApplicationScoped
public class BridgeRouters {
	  protected static final Logger log = org.apache.logging.log4j.LogManager
		      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

		@Inject
		BridgeRouterHandlers bridgeRouterHandlers;

		@Inject
		BridgeHandler bridgeHandler;
  
	static private Router bridgeRouter = null;
 
  //protected static void routers(final Vertx vertx) {
  protected void routers(final Vertx vertx) {
	  bridgeRouter = Router.router(vertx);  // create new router
	  
	  BridgeRouterHandlers.avertx = vertx;

	  bridgeRouter.route().handler(BridgeRouterHandlers.cors());

	  bridgeRouter.route("/frontend/*").subRouter(bridgeHandler.eventBusHandler(vertx));
	  bridgeRouter.route(HttpMethod.GET, "/api/events/init").handler(BridgeRouterHandlers::apiGetInitHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/events/init").handler(BridgeRouterHandlers::apiInitHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/service").handler(bridgeRouterHandlers::apiServiceHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/service/sync").handler(bridgeRouterHandlers::apiSyncHandler).handler(TimeoutHandler.create(120000));   // old mobile
	  bridgeRouter.route(HttpMethod.POST, "/v7/api/service/sync").blockingHandler(bridgeRouterHandlers::apiSync2Handler).handler(TimeoutHandler.create(120000)); // mobile v7

	  bridgeRouter.route(HttpMethod.GET, "/api/pull/:key").handler(BridgeRouterHandlers::apiGetPullHandler);

	  bridgeRouter.route(HttpMethod.GET, "/version").handler(BridgeRouterHandlers::apiGetVersionHandler);
      bridgeRouter.route(HttpMethod.GET, "/health").handler(BridgeRouterHandlers::apiGetHealthHandler);

	  bridgeRouter.route(HttpMethod.POST, "/api/cmds").handler(bridgeRouterHandlers::apiHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/data").handler(bridgeRouterHandlers::apiHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/devices").blockingHandler(bridgeRouterHandlers::apiDevicesHandler);
	  bridgeRouter.route(HttpMethod.GET, "/api/search").handler(BridgeRouterHandlers::apiSearchHandler);

	  bridgeRouter.route(HttpMethod.POST, "/api/virtualbus").blockingHandler(bridgeRouterHandlers::virtualEventBusHandler);
    
	  //bridgeRouter.route(HttpMethod.GET, "/metrics").handler(Metrics::metrics);
	log.info("Activating Bridge Routes on port "+GennySettings.apiPort+" given ["+GennySettings.apiPort+"]");
	
	HttpServerOptions serverOptions = new HttpServerOptions();
	//  serverOptions.setUsePooledBuffers(true);
	//  serverOptions.setCompressionSupported(true);
	//  serverOptions.setCompressionLevel(3);

	//  serverOptions.setUseAlpn(true);
	
	
	vertx.createHttpServer(serverOptions).requestHandler(bridgeRouter::accept).listen(Integer.parseInt(GennySettings.apiPort));

  }

}
