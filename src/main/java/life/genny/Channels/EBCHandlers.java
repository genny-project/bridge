package life.genny.Channels;

public class EBCHandlers {
	
	public static void registerHandlers(){
		EBConsumers.getEBConsumers().getFromClientOutbound().subscribe(res->{
			System.out.println(res.body()+"ksdklfsdklfjdsf====================================");
		});
	}
	
}
