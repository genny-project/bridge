package life.genny.channels;

import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import rx.Observable;

public class EBConsumers {
	
	private static Observable<Message<Object>> fromCmds;
	private static Observable<Message<Object>> fromData;
	private static Observable<Message<Object>> fromServices;
	/**
	 * @return the fromCmds
	 */
	public static Observable<Message<Object>> getFromCmds() {
		return fromCmds;
	}
	

	/**
	 * @param fromCmds the fromCmds to set
	 */
	private static void setFromCmds(Observable<Message<Object>> fromCmds) {
		EBConsumers.fromCmds = fromCmds;
	}
	
	
	
	/**
	 * @return the fromData
	 */
	public static Observable<Message<Object>> getFromData() {
		return fromData;
	}


	/**
	 * @param fromData the fromData to set
	 */
	public static void setFromData(Observable<Message<Object>> fromData) {
		EBConsumers.fromData = fromData;
	}


	/**
	 * @return the fromServices
	 */
	public static Observable<Message<Object>> getFromServices() {
		return fromServices;
	}


	/**
	 * @param fromServices the fromServices to set
	 */
	public static void setFromServices(Observable<Message<Object>> fromServices) {
		EBConsumers.fromServices = fromServices;
	}


	public static void registerAllConsumer(EventBus eb){
		setFromCmds(eb.consumer("cmds").toObservable());
		setFromData(eb.consumer("data").toObservable());
		setFromServices(eb.consumer("services").toObservable());
	}

}
