package life.genny.channels;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.MessageProducer;

public class EBProducers {

	private static MessageProducer<JsonObject> toClientOutbound;
	private static MessageProducer<JsonObject> toEvents;
	private static MessageProducer<JsonObject> toData;
	private static MessageProducer<JsonObject> toMessages;
	private static MessageProducer<JsonObject> toCmds;

	/**
	 * @return the toClientOutbount
	 */
	public static MessageProducer<JsonObject> getToClientOutbound() {
		return toClientOutbound;
	}

	/**
	 * @param toClientOutbount
	 *            the toClientOutbount to set
	 */
	public static void setToClientOutbound(MessageProducer<JsonObject> toClientOutbount) {
		EBProducers.toClientOutbound = toClientOutbount;
	}

	/**
	 * @return the toEvents
	 */
	public static MessageProducer<JsonObject> getToEvents() {
		return toEvents;
	}

	/**
	 * @return the toData
	 */
	public static MessageProducer<JsonObject> getToData() {
		return toData;
	}

	/**
	 * @param toData
	 *            the toData to set
	 */
	public static void setToData(MessageProducer<JsonObject> toData) {
		EBProducers.toData = toData;
	}

	/**
	 * @param toEvents
	 *            the toEvents to set
	 */
	public static void setToEvents(MessageProducer<JsonObject> toEvents) {
		EBProducers.toEvents = toEvents;
	}

	public static MessageProducer<JsonObject> getToMessages() {
		return toMessages;
	}

	public static void setToMessages(MessageProducer<JsonObject> toMessages) {
		EBProducers.toMessages = toMessages;
	}
	
	public static MessageProducer<JsonObject> getToCmds() {
		return toCmds;
	}

	public static void setToCmds(MessageProducer<JsonObject> toCmds) {
		EBProducers.toCmds = toCmds;
	}

	public static void registerAllProducers(EventBus eb) {
		setToEvents(eb.publisher("events"));
		setToData(eb.publisher("data"));
		setToCmds(eb.publisher("cmds"));
		setToMessages(eb.publisher("messages"));
	}
}
