package life.genny.bridgecmd;

import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import life.genny.cluster.Cluster;
import life.genny.security.SecureResources;

public class ServiceVerticle extends AbstractVerticle {

	@Override
	public void start() {
		System.out.println("Setting up routes");
		Future<Void> startFuture = Future.future();
		Cluster.joinCluster(vertx).compose( res -> {
			Future<Void> fut = Future.future();
			SecureResources.setKeycloakJsonMap(vertx).compose(p -> {
				Routers.routers(vertx);
				fut.complete();
			},fut);
			startFuture.complete();
		},startFuture);
	}	
}
