package life.genny.bridge;


import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import life.genny.metrics.Metrics;



public class BridgeRouters {

	private static int bridgeServerPort = (System.getenv("API_PORT") != null) ? (Integer.parseInt(System.getenv("API_PORT")))
			: 8088;

	static private Router bridgeRouter = null;
 
  protected static void routers(final Vertx vertx) {
	  bridgeRouter = Router.router(vertx);  // create new router


	  bridgeRouter.route("/frontend/*").handler(BridgeHandler.eventBusHandler(vertx));
	  bridgeRouter.route(HttpMethod.GET, "/api/events/init").handler(RouterHandlers::apiGetInitHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/events/init").handler(RouterHandlers::apiInitHandler);
    // router.route(HttpMethod.GET, "/api/session").handler(RouterHandlers::apiSession);
	  bridgeRouter.route(HttpMethod.POST, "/api/service").handler(RouterHandlers::apiServiceHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/cmds").handler(RouterHandlers::apiHandler);
	  bridgeRouter.route(HttpMethod.POST, "/api/data").handler(RouterHandlers::apiHandler);
    
	  bridgeRouter.route(HttpMethod.GET, "/metrics").handler(Metrics::metrics);
	System.out.println("Activating Bridge Routes on port "+bridgeServerPort+" given ["+System.getenv("API_PORT")+"]");
	vertx.createHttpServer().requestHandler(bridgeRouter::accept).listen(bridgeServerPort);

  }

}
