package life.genny.bridge;


import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.quarkus.runtime.Startup;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import life.genny.channel.Consumer;
import life.genny.channel.KProducer;
import life.genny.channel.Producer;
import life.genny.channel.Routers;
import life.genny.cluster.Cluster;
import life.genny.cluster.CurrentVtxCtx;
import life.genny.eventbus.EventBusInterface;
import life.genny.eventbus.EventBusVertx;
import life.genny.eventbus.VertxCache;
import life.genny.qwandautils.GennyCacheInterface;
import life.genny.utils.VertxUtils;

@Startup
@ApplicationScoped
public class ServiceVerticle {
	
  protected static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


  @Inject 
  Vertx vertx;
//  public void start() {
//    log.info("Setting up routes");
//    final Future<Void> startFuture = Future.future();
//    Cluster.joinCluster().compose(res -> {
//      EventBusInterface eventBus = new EventBusVertx();
//      GennyCacheInterface vertxCache = new VertxCache();
//      VertxUtils.init(eventBus,vertxCache);
//      Routers.routers(vertx);
//      BridgeRouters.routers(vertx);
//      Routers.activate(vertx);
//      log.info("Bridge now ready");
//      
//      EBCHandlers.registerHandlers();
//      startFuture.complete();
//    }, startFuture);
//    
//  }
   @PostConstruct
     public void start() {
       EventBus eb = vertx.eventBus();
       Cluster.joinCluster();
       System.out.println(vertx.isClustered());
       Consumer.registerAllConsumer(eb);
       Producer.registerAllProducers(eb);
         CurrentVtxCtx.getCurrentCtx().setClusterVtx(vertx);
         EventBusInterface eventBus = new EventBusVertx();
         GennyCacheInterface vertxCache = new VertxCache();
         VertxUtils.init(eventBus,vertxCache);
         Routers.routers(vertx);
         BridgeRouters.routers(vertx);
         Routers.activate(vertx);
         
         EBCHandlers.registerHandlers();
         KProducer.sendr();
     }
}
