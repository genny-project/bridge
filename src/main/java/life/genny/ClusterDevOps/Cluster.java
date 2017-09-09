package life.genny.ClusterDevOps;

import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import rx.functions.Action1;
import life.genny.Channels.EBProducers;
import life.genny.Channels.EBConsumers;
import life.genny.Channels.EBCHandlers;
import io.vertx.rxjava.core.eventbus.EventBus;
public class Cluster {

	static Action1<? super Vertx> registerAllChannels = vertx -> {
		EventBus eb = vertx.eventBus();
		EBConsumers.getEBConsumers().registerAllConsumer(eb);
		EBProducers.getEBProducers().registerAllProducers(eb);
	};

	static Action1<Throwable> clusterError = error -> {
		System.out.println("error in the cluster: " + error.getMessage());
	};

	public static Future<Void> joinCluster(Vertx vertx) {
		Future<Void> fut = Future.future();
		vertx.rxClusteredVertx(ClusterConfig.configCluster())
			.subscribe(registerAllChannels, clusterError);
		fut.complete();
		return fut;
	}

	// private static final Logger logger =
	// LoggerFactory.getLogger(ServiceVerticle.class);
	// static String myip = System.getenv("MYIP");
	// private static EventBus eventBus = null;
	//
	// public static MessageProducer<JsonObject> msgToFrontEnd;
	// public static MessageProducer<JsonObject> toEvents;
	// public static MessageProducer<JsonObject> toCmds;
	// public static MessageProducer<JsonObject> toData;
	//
	// public static Observable<Message<Object>> events;
	// public static Observable<Message<Object>> cmds;
	// public static Observable<Message<Object>> data;
	// public static MessageProducer<JsonObject> msgToKieClient;
	//
	// public static Future<Void> createCluster() {
	//
	// Future<Void> startFuture = Future.future();
	// Vertx vertx = Vertx.vertx();
	// vertx.<HazelcastInstance>executeBlocking(future -> {
	//
	// if (System.getenv("SWARM") != null) {
	//
	// Config conf = new ClasspathXmlConfig("hazelcast-genny.xml");
	// System.out.println("Starting hazelcast DISCOVERY!!!!!");
	// NodeContext nodeContext = new DefaultNodeContext() {
	// @Override
	// public AddressPicker createAddressPicker(Node node) {
	// return new SwarmAddressPicker(new SystemPrintLogger());
	// }
	// };
	//
	// HazelcastInstance hazelcastInstance =
	// HazelcastInstanceFactory.newHazelcastInstance(conf, "bridge",
	// nodeContext);
	// System.out.println("Done hazelcast DISCOVERY");
	// future.complete(hazelcastInstance);
	// } else {
	// future.complete(null);
	// }
	// }, res -> {
	// if (res.succeeded()) {
	// System.out.println("RESULT SUCCEEDED");
	// HazelcastInstance hazelcastInstance = (HazelcastInstance) res.result();
	// ClusterManager mgr = null;
	// if (hazelcastInstance != null) {
	// mgr = new HazelcastClusterManager(hazelcastInstance);
	// } else {
	// mgr = new HazelcastClusterManager(); // standard docker
	// }
	// System.out.println("Starting Clustered Vertx");
	// VertxOptions options = new VertxOptions().setClusterManager(mgr);
	//
	// if (System.getenv("SWARM") == null) {
	// System.out.println("NOT SWARM");
	// if (System.getenv("GENNYDEV") == null) {
	// System.out.println("setClusterHost etc");
	// options.setClusterHost("bridge").setClusterPublicHost("bridge").setClusterPort(15701);
	// } else {
	// logger.info("Running DEV mode, no cluster");
	// options.setBlockedThreadCheckInterval(200000000);
	// options.setMaxEventLoopExecuteTime(Long.MAX_VALUE);
	// }
	//
	// } else {
	// options.setClusterPublicHost(myip).setClusterPublicPort(5701);
	// }
	//
	// Vertx.clusteredVertx(options, res2 -> {
	// if (res2.succeeded()) {
	// eventBus = res2.result().eventBus();
	// // handler.setEventBus(eventBus);
	// System.out.println("Bridge Cluster
	// _________________+++++++++++++++++Started!");
	// startFuture.complete();
	// } else {
	// // failed!
	// }
	// });
	// }
	// });
	// return startFuture;
	// }
	//
	// public static void eventListeners() {
	// events = eventBus.consumer("events").toObservable();
	// cmds = eventBus.consumer("cmds").toObservable();
	// data = eventBus.consumer("data").toObservable();
	// toEvents = eventBus.publisher("events");
	// toCmds = eventBus.publisher("cmds");
	// toData = eventBus.publisher("data");
	// }
	//
	// public static void registerLocalAddresses() {
	// msgToFrontEnd = eventBus.publisher("address.outbound");
	// }
	//
	// public static void setupCluster() {
	// Vertx vertx = Vertx.vertx();
	// System.out.println(myip+"jkskfjsdklfjskdjfsdfkjsdlk 000000000000");
	//
	// Future<Void> startFuture = Future.future();
	// ClusterTopo.createCluster().compose(v -> {
	// msgToKieClient = eventBus.publisher("rules.kieclient");
	// eventListeners();
	// registerLocalAddresses();
	// eventsInOutFromCluster();
	//
	// startFuture.complete();
	// }, startFuture);
	// }
	// public static void eventsInOutFromCluster() {
	// cmds.subscribe(arg -> {
	// String incomingCmd = arg.body().toString();
	// logger.info(incomingCmd);
	// if (!incomingCmd.contains("<body>Unauthorized</body>")) {
	// // ugly, but remove the outer array
	// if (incomingCmd.startsWith("[")) {
	// incomingCmd = incomingCmd.replaceFirst("\\[", "");
	// incomingCmd = incomingCmd.substring(0, incomingCmd.length()-1);
	// }
	// JsonObject json = new JsonObject(incomingCmd); //
	// Buffer.buffer(arg.toString().toString()).toJsonObject();
	// DeliveryOptions options = new DeliveryOptions();
	// options.addHeader("Content-Type", "application/json");
	// msgToFrontEnd.deliveryOptions(options);
	// msgToFrontEnd.write(json);
	// } else {
	// logger.error("Cmd with Unauthorised data recieved");
	// }
	// });
	// data.subscribe(arg -> {
	// String incomingData = arg.body().toString();
	// logger.info(incomingData);
	// JsonObject json = new JsonObject(incomingData); //
	// Buffer.buffer(arg.toString().toString()).toJsonObject();
	// msgToFrontEnd.write(json);
	// //
	// msgToFrontEnd.write(Buffer.buffer(arg.body().toString()).toJsonObject());
	// });
	// }
}
