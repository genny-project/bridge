package life.genny.bridge.live.data;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

/**
 * InternalProducer --- Kafka smalltye producer objects to send to internal consumers backends such
 * as wildfly-rulesservice.
 *
 * @author hello@gada.io
 */
@ApplicationScoped
public class InternalProducer {

  @Inject
  @Channel("eventsout")
  Emitter<String> events;

  public Emitter<String> getToEvents() {
    return events;
  }

  @Inject
  @Channel("dataout")
  Emitter<String> data;

  public Emitter<String> getToData() {
    return data;
  }
}
