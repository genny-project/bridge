package life.genny.bridgecmd;

import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public class Routers {

	private static int serverPort = 8081;
	
	protected static void routers(Vertx vertx) {
		Router router = Router.router(vertx);
		router.route().handler(RouterHandlers.cors());
		router.route("/frontend/*").handler(BridgeHandler.eventBusHandler(vertx));
		router.route(HttpMethod.GET, "/api/events/init").handler(RouterHandlers::apiGetInitHandler);
		router.route(HttpMethod.POST, "/api/events/init").handler(RouterHandlers::apiInitHandler);
		router.route(HttpMethod.POST, "/api/service").handler(RouterHandlers::apiServiceHandler);
		vertx.createHttpServer().requestHandler(router::accept).listen(serverPort);
	}
}
