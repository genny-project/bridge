package life.genny.channels;

import java.util.HashMap;
import java.util.Map;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.MessageProducer;

public class EBProducers {

	private static MessageProducer<JsonObject> toClientOutbound;
	private static MessageProducer<JsonObject> toEvents;
	private static MessageProducer<JsonObject> toData;
	private static MessageProducer<JsonObject> toMessages;
	private static MessageProducer<JsonObject> toSession;
	private static Map<String, MessageProducer<JsonObject>> channelSessionList = new HashMap<String, MessageProducer<JsonObject>>(); 
	/**
   * @return the channelSessionList
   */
  public static Map<String, MessageProducer<JsonObject>> getChannelSessionList() {
    return channelSessionList;
  }

  /**
   * @param channelSessionList the channelSessionList to set
   */
  public static void setChannelSessionList(
      Map<String, MessageProducer<JsonObject>> channelSessionList) {
    EBProducers.channelSessionList = channelSessionList;
  }

  /**
   * @return the toSession
   */
  public static MessageProducer<JsonObject> getToSession() {
    return toSession;
  }

  /**
   * @param toSession the toSession to set
   */
  public static void setToSession(MessageProducer<JsonObject> toSession) {
    EBProducers.toSession = toSession;
  }

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

	public static void registerAllProducers(EventBus eb) {
		setToEvents(eb.publisher("events"));
		setToData(eb.publisher("data"));
		setToMessages(eb.publisher("messages"));
	}
}
