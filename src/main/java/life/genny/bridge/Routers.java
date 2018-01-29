package life.genny.bridge;

import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;



public class Routers {

  private static int serverPort = 8088;

 
  protected static void routers(final Vertx vertx) {
    final Router router = Router.router(vertx);
    RouterHandlers.vertx = vertx;
    router.route().handler(RouterHandlers.cors());
    router.route("/frontend/*").handler(BridgeHandler.eventBusHandler(vertx));
    router.route(HttpMethod.GET, "/api/events/init").handler(RouterHandlers::apiGetInitHandler);
    router.route(HttpMethod.POST, "/api/events/init").handler(RouterHandlers::apiInitHandler);
    router.route(HttpMethod.GET, "/api/session").handler(RouterHandlers::apiSession);
    router.route(HttpMethod.POST, "/api/service").handler(RouterHandlers::apiServiceHandler);
    router.route(HttpMethod.POST, "/api/cmds").handler(RouterHandlers::apiHandler);
    router.route(HttpMethod.POST, "/api/data").handler(RouterHandlers::apiHandler);
    router.route(HttpMethod.POST, "/write/:param1/:param2").handler(RouterHandlers::apiMapPutHandler);
    router.route(HttpMethod.GET, "/read/:param1").handler(RouterHandlers::apiMapGetHandler);
 
    router.route(HttpMethod.GET, "/version").handler(VersionHandler::apiGetVersionHandler);

    
    vertx.createHttpServer().requestHandler(router::accept).listen(serverPort);
  }
  
}
