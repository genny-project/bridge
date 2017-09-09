package life.genny.bridgecmd;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import io.vertx.rxjava.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import life.genny.Channels.EBProducers;
import life.genny.ClusterDevOps.Cluster;

public class ServiceVerticle extends AbstractVerticle {
	
	private static int serverPort = 8081;
	
	@Override
	public void start() {
		System.out.println("Setting up routes");
		Future<Void> startFuture = Future.future();
		
		Cluster.joinCluster(vertx).compose(res->{
			return routers();
		}).compose(System.out::println,startFuture);
	}

	public Future<Void> routers() {
		Future<Void> fut = Future.future();
		Router router = Router.router(vertx);
		router.route().handler(cors());
		router.route("/frontend/*").handler(eventBusHandler());
		vertx.createHttpServer().requestHandler(router::accept).listen(serverPort);
		fut.complete();
		return fut;
	}

	public CorsHandler cors() {
		return CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST)
				.allowedMethod(HttpMethod.OPTIONS).allowedHeader("X-PINGARUNER").allowedHeader("Content-Type");
	}

	private SockJSHandler eventBusHandler() {
		PermittedOptions inboundPermitted1 = new PermittedOptions().setAddress("address.inbound");
		PermittedOptions outboundPermitted2 = new PermittedOptions().setAddressRegex("address.outbound");
		BridgeOptions options = new BridgeOptions();
		options.setMaxAddressLength(10000);
		options.addInboundPermitted(inboundPermitted1);
		options.addOutboundPermitted(outboundPermitted2);
		SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
		
		return sockJSHandler.bridge(options, this::bridgeHandler);

	}

	public void bridgeHandler(BridgeEvent bridgeEvent) {
		if (bridgeEvent.type() == BridgeEventType.SOCKET_CREATED) {

		} else if (bridgeEvent.type() == BridgeEventType.PUBLISH || bridgeEvent.type() == BridgeEventType.SEND) {
			JsonObject rawMessage = bridgeEvent.getRawMessage().getJsonObject("body");
			String token = bridgeEvent.getRawMessage().getString("token");
			rawMessage = rawMessage.getJsonObject("data");
			System.out.println("jjsdfkljsdklfjslkjfdjs");
//			vertx.eventBus().publisher("address.outbound").write(new JsonObject().put("sdkjfksdjf", "sdklfsdlkfjsjd"));
			EBProducers.getEBProducers().getToClientOutbound().write(new JsonObject().put("sdkjfksdjf", "sdklfsdlkfjsjd"));
		}
		bridgeEvent.complete(true);
	}
}
