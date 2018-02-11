package life.genny.cluster;

import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import rx.functions.Action1;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.channels.ClusterMap;
import life.genny.channels.EBCHandlers;
import life.genny.channels.EBConsumers;
import life.genny.channels.EBProducers;

public class Cluster {

	static Action1<? super Vertx> registerAllChannels = vertx -> {
		EventBus eb = vertx.eventBus();
		EBConsumers.registerAllConsumer(eb);
		EBProducers.registerAllProducers(eb);
		EBCHandlers.registerHandlers();
		ClusterMap.setVertxContext(vertx);
	};

	static Action1<Throwable> clusterError = error -> {
		System.out.println("error in the cluster: " + error.getMessage());
	};

	public static Future<Void> joinCluster() {
		Future<Void> fut = Future.future();
		Vertx.currentContext().owner().rxClusteredVertx(ClusterConfig.configCluster())
			.subscribe(registerAllChannels, clusterError);
		fut.complete();
		return fut;
	}

}
