package life.genny.channels;

import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import rx.Observable;

public class EBConsumers {
	
	private static Observable<Message<Object>> fromCmds;

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
	
	public static void registerAllConsumer(EventBus eb){
		setFromCmds(eb.consumer("events").toObservable());
	}

}
