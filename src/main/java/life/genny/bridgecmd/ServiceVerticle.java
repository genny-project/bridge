package life.genny.bridgecmd;

import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import life.genny.cluster.Cluster;
import life.genny.security.SecureResources;

public class ServiceVerticle extends AbstractVerticle {
	
	private static int serverPort = 8081;

	@Override
	public void start() {
		System.out.println("Setting up routes");
		Future<Void> startFuture = Future.future();
		Cluster.joinCluster(vertx).compose( res -> {
			Future<Void> fut = Future.future();
			SecureResources.setKeycloakJsonMap(vertx).compose(p -> {
				routers();
				fut.complete();
			},fut);
			startFuture.complete();
		},startFuture);
	}

	public void routers() {
		Router router = Router.router(vertx);
		router.route().handler(RouterHandlers.cors());
		router.route("/frontend/*").handler(BridgeHandler.eventBusHandler(vertx));
		router.route(HttpMethod.GET, "/api/events/init").handler(RouterHandlers::apiGetInitHandler);
		router.route(HttpMethod.POST, "/api/events/init").handler(RouterHandlers::apiInitHandler);
		router.route(HttpMethod.POST, "/api/service").handler(RouterHandlers::apiServiceHandler);
		vertx.createHttpServer().requestHandler(router::accept).listen(serverPort);
	}
}
