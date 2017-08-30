package life.genny.rulesservice;

public enum BusinessProcessType {

	PROCESS("Process"),
	WORKITEM("Workitem"),
	NODE("Node"),
	USERTASK("Usertask"),
	SERVICETASK("Servicetask"),
	ASSOCIATEENTITY("Associateentity"),
	VARIABLES("Variable"),
	RULE("Rule");
	
	public String getDescription() {
		return description;
	}

	private final String description;
	
	BusinessProcessType (String desc){
		description = desc.toLowerCase();
	}
}
