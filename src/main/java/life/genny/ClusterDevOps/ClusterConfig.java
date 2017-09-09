package life.genny.ClusterDevOps;

import com.hazelcast.config.Config;

import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * @author Adam Crow
 * @author Byron Aguirre
 */
public class ClusterConfig {
	
	
	private static String hostIP = System.getenv("DOCKER_HOST") != null ? 
			System.getenv("DOCKER_HOST") : "127.0.0.1";
			
	private static String privateIP = System.getenv("MYIP");
	private final static int portHazelcastCluster = 5701;
	private final static int portEBCluster = 15702;

	/**
	 * @param toClientOutbount the toClientOutbount to set
	 */
	public static Config configHazelcastCluster() {
		Config hazelcastConfig = new Config();
		hazelcastConfig.getNetworkConfig().setPort(portHazelcastCluster);
		hazelcastConfig.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		hazelcastConfig.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
		hazelcastConfig.getNetworkConfig().setPublicAddress(hostIP);
		hazelcastConfig.getNetworkConfig().getJoin().getTcpIpConfig().addMember(hostIP);
		return hazelcastConfig;
	}

	/**
	 * @param toClientOutbount the toClientOutbount to set
	 */
	public static EventBusOptions configEBCluster() {
		EventBusOptions eb = new EventBusOptions();
		if (privateIP != null)
			eb.setPort(portEBCluster).setHost(privateIP);
		return eb;
	}

	/**
	 * @param toClientOutbount the toClientOutbount to set
	 */
	public static VertxOptions configCluster() {
		ClusterManager mgr = new HazelcastClusterManager(configHazelcastCluster());
		VertxOptions options = new VertxOptions();
		options.setClusterManager(mgr);
		options.setEventBusOptions(configEBCluster());
		return options;
	}
}
