package life.genny.bridge.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QDataB2BMessage extends QDataMessage {
	
	
	private static final long serialVersionUID = 1L;

	  @JsonProperty
	  private GennyItem[] items;
	  private static final String DATATYPE_ITEM = GennyItem.class.getSimpleName();


	  public QDataB2BMessage(final GennyItem[] items) {
	    super(DATATYPE_ITEM);
	    setItems(items);
	  }

	  public GennyItem[] getItems() {
	    return items;
	  }

	  public void setItems(final GennyItem[] items) {
	    this.items = items;
	  }
	
}
