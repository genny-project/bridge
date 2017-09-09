package life.genny.Channels;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.MessageProducer;

public class EBProducers {
	
	
	private MessageProducer<JsonObject> toClientOutbound;
	private MessageProducer<JsonObject> toEvents;
	
	private static EBProducers ebProducers = null;
	
	public static EBProducers getEBProducers() {
        if (ebProducers == null) 
        		ebProducers = new EBProducers();
        return ebProducers;
	}
	/**
	 * @return the toClientOutbount
	 */
	public MessageProducer<JsonObject> getToClientOutbound() {
		return toClientOutbound;
	}
	
	/**
	 * @param toClientOutbount the toClientOutbount to set
	 */
	private void setToClientOutbound(MessageProducer<JsonObject> toClientOutbount) {
		this.toClientOutbound = toClientOutbount;
	}
	
	/**
	 * @return the toEvents
	 */
	public MessageProducer<JsonObject> getToEvents() {
		return toEvents;
	}
	
	/**
	 * @param toEvents the toEvents to set
	 */
	public void setToEvents(MessageProducer<JsonObject> toEvents) {
		this.toEvents = toEvents;
	}
		
	public void registerAllProducers(EventBus eb){
		setToClientOutbound(eb.publisher("address.outbound"));
		setToEvents(eb.publisher("events"));
	}
}
