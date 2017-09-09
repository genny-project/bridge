package life.genny.Channels;

import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import rx.Observable;

public class EBConsumers {
	
	private static EBConsumers ebConsumers = null;
	
	private Observable<Message<Object>> fromCmds;
	private Observable<Message<Object>> fromClientOutbound;
	
	
	
	public static EBConsumers getEBConsumers() {
        if (ebConsumers == null) 
        		ebConsumers = new EBConsumers();
        return ebConsumers;
	}
	
	

	/**
	 * @return the fromClientOutbound
	 */
	public Observable<Message<Object>> getFromClientOutbound() {
		return fromClientOutbound;
	}
	


	/**
	 * @param fromClientOutbound the fromClientOutbound to set
	 */
	public void setFromClientOutbound(Observable<Message<Object>> fromClientOutbound) {
		this.fromClientOutbound = fromClientOutbound;
	}
	


	/**
	 * @return the fromCmds
	 */
	public Observable<Message<Object>> getFromCmds() {
		return fromCmds;
	}
	

	/**
	 * @param fromCmds the fromCmds to set
	 */
	private void setFromCmds(Observable<Message<Object>> fromCmds) {
		this.fromCmds = fromCmds;
	}
	
	public void registerAllConsumer(EventBus eb){
		setFromCmds(eb.consumer("events").toObservable());
		setFromClientOutbound(eb.consumer("address.outbound").toObservable());
		getFromClientOutbound().subscribe(res->{
			System.out.println(res.body()+"ksdklfsdklfjdsf====================================");
		});
	}

}
