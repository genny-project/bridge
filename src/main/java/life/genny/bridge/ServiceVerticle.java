package life.genny.bridge;


import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import life.genny.channel.Routers;
import life.genny.cluster.Cluster;
import life.genny.qwandautils.QwandaUtils;
import life.genny.eventbus.EventBusInterface;
import life.genny.eventbus.EventBusVertx;
import life.genny.eventbus.VertxCache;
import life.genny.qwandautils.GennyCacheInterface;
import life.genny.security.SecureResources;
import life.genny.utils.VertxUtils;

public class ServiceVerticle extends AbstractVerticle {

  @Override
  public void start() {
    System.out.println("Setting up routes");
    final Future<Void> startFuture = Future.future();
    Cluster.joinCluster().compose(res -> {
      final Future<Void> fut = Future.future();
      EventBusInterface eventBus = new EventBusVertx();
      GennyCacheInterface vertxCache = new VertxCache();
      VertxUtils.init(eventBus,vertxCache);

      SecureResources.setKeycloakJsonMap().compose(p -> {
    	Routers.routers(vertx);
        BridgeRouters.routers(vertx);
        Routers.activate(vertx);
        System.out.println("QWANDA UTILS CODE: " + QwandaUtils.testMethod());
        System.out.println("Bridge now ready");
        fut.complete();
      }, fut);
      EBCHandlers.registerHandlers();
      startFuture.complete();
    }, startFuture);
    
  }
}
