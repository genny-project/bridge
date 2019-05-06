package life.genny.bridge;


import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import life.genny.channel.Routers;
import life.genny.cluster.Cluster;
import life.genny.eventbus.EventBusInterface;
import life.genny.eventbus.EventBusVertx;
import life.genny.eventbus.VertxCache;
import life.genny.qwandautils.GennyCacheInterface;
import life.genny.security.SecureResources;
import life.genny.utils.VertxUtils;

public class ServiceVerticle extends AbstractVerticle {
	
	  protected static final Logger log = org.apache.logging.log4j.LogManager
		      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


  @Override
  public void start() {
    log.info("Setting up routes");
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
        log.info("Bridge now ready");
        fut.complete();
      }, fut);
      EBCHandlers.registerHandlers();
      startFuture.complete();
    }, startFuture);
    
  }
}
