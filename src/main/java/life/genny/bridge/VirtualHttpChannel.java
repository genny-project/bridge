package life.genny.bridge;

import java.util.function.Consumer;

import io.vertx.core.json.JsonObject;

@FunctionalInterface
public interface VirtualHttpChannel {

	public void send(Object o, Consumer<JsonObject> handler);

	static VirtualHttpChannel post(VirtualChannelServices client){
		return (object,handler) -> handler.accept(client.sendPayload(object));
	}

}
