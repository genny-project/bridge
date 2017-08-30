package life.genny.rulesservice;

public enum BusinessProcessEvent {

	START("start"),
	SKIPT("skipt"),
	ABORT("abort"),
	FIND("find"),
	SIGNAL("signal"),
	SET("set"),
	GET("get"),
	COMPLETE("Complete"),
	FIRE("Fire"),
	PREINFO("Preinfo");
	
	public String getDescription() {
		return description.toLowerCase();
	}
	
	private final String description;
	BusinessProcessEvent(String desc){
		description =  desc;
	}
}
