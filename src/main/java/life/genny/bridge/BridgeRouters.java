package life.genny.bridge;


import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import life.genny.channel.Routers;
import life.genny.metrics.Metrics;



public class BridgeRouters {

      
  protected static void routers(final Vertx vertx) {
    Router router = Routers.getRouter(vertx);


    router.route("/frontend/*").handler(BridgeHandler.eventBusHandler(vertx));
    router.route(HttpMethod.GET, "/api/events/init").handler(RouterHandlers::apiGetInitHandler);
    router.route(HttpMethod.POST, "/api/events/init").handler(RouterHandlers::apiInitHandler);
    // router.route(HttpMethod.GET, "/api/session").handler(RouterHandlers::apiSession);
    router.route(HttpMethod.POST, "/api/service").handler(RouterHandlers::apiServiceHandler);
    router.route(HttpMethod.POST, "/api/cmds").handler(RouterHandlers::apiHandler);
    router.route(HttpMethod.POST, "/api/data").handler(RouterHandlers::apiHandler);
    
    router.route(HttpMethod.GET, "/metrics").handler(Metrics::metrics);

  }

}
