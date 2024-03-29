package life.genny.bridge.client.exception;

import java.io.ByteArrayInputStream;
import javax.annotation.Priority;
import javax.validation.ValidationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * ResponseException --- Get the corresponding web exception
 * returned by the server
 *
 * @author    hello@gada.io
 */
@Priority(4000)                                            
public class ResponseException implements 
             ResponseExceptionMapper<RuntimeException> {   
  @Override
  public RuntimeException toThrowable(Response response) {
    int status = response.getStatus();                    

    String msg = getBody(response); 

    RuntimeException re ;
    switch (status) {
      case 412: re = new ValidationException(msg);         
      break;
      default:
        re = new WebApplicationException(status);         
    }
    return re;
	}
	private String getBody(Response response) {
		ByteArrayInputStream is = (ByteArrayInputStream) response.getEntity();
		byte[] bytes = new byte[is.available()];
		is.read(bytes,0,is.available());
		String body = new String(bytes);
		return body;
	}
}
