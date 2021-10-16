package life.genny.bridge.live.data;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

/**
 * ExternalConsumerConfig ---
 *
 * @author    hello@gada.io
 *
 */
public class ExternalConsumerConfig {

	@Inject Vertx vertx;
	@Inject ExternalConsumer handler;

	private static List<PermittedOptions> setInbounds(){
		List<PermittedOptions> inbounds = new ArrayList<PermittedOptions>();
		inbounds.add(new PermittedOptions().setAddress("address.inbound"));
		inbounds.add(new PermittedOptions().setAddressRegex(".*"));
		return inbounds;
	}
	
	private static List<PermittedOptions> setOutbounds(){
		List<PermittedOptions> inbounds = new ArrayList<PermittedOptions>();
		inbounds.add(new PermittedOptions().setAddressRegex("address.outbound"));
		inbounds.add(new PermittedOptions().setAddressRegex("^(?!(address\\.inbound)$).*"));
		return inbounds;
	}

	protected static SockJSBridgeOptions setBridgeOptions(){
		SockJSBridgeOptions options = new SockJSBridgeOptions();
		options.setMaxHandlersPerSocket(10);
		options.setPingTimeout(120000); // 2 minutes
		options.setReplyTimeout(60000);
		setInbounds().stream().forEach(options::addInboundPermitted);
		setOutbounds().stream().forEach(options::addOutboundPermitted);
		return options;
	}

	public static CorsHandler cors() {
		return CorsHandler.create(
				"http://localhost:\\d\\d|"+
				"https://localhost:\\d\\d|"+
				"http://localhost:\\d\\d\\d\\d|"+
				"https://localhost:\\d\\d\\d\\d|"+
				"https://.*.genny.life|https://.*.gada.io|"+
				System.getenv("CORS_URLS")
				).allowCredentials(true)
			.allowedMethod(HttpMethod.GET)
			.allowedMethod(HttpMethod.POST)
			.allowedMethod(HttpMethod.PUT)
			.allowedMethod(HttpMethod.OPTIONS)
			.allowedHeader("X-PINGARUNER")
			.allowedHeader("Content-Type")
			.allowedHeader("Authorization")
			.allowedHeader("Accept")
			.allowedHeader("X-Requested-With");
	}

	public void init(@Observes Router router) {
		SockJSHandlerOptions sockOptions = new SockJSHandlerOptions().setHeartbeatInterval(2000);
		SockJSHandler sockJSHandler = SockJSHandler.create(vertx, sockOptions);
		sockJSHandler.bridge(setBridgeOptions(),handler::handleConnectionTypes);
		router.route().handler(cors());
		router.route("/frontend/*").handler(sockJSHandler);
	}
}
