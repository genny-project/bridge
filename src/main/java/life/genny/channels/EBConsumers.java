package life.genny.channels;

import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import rx.Observable;

public class EBConsumers {
	
	private static Observable<Message<Object>> fromCmds;
	private static Observable<Message<Object>> fromData;

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


	public static void registerAllConsumer(EventBus eb){
		setFromCmds(eb.consumer("cmds").toObservable());
		setFromData(eb.consumer("data").toObservable());
	}

}
