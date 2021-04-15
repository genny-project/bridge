package life.genny.bridge;

import java.io.ByteArrayInputStream;

import javax.annotation.Priority;
import javax.validation.ValidationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

@Priority(4000)                                            // (1)
public class MyMapper implements 
             ResponseExceptionMapper<RuntimeException> {   // (2)
  @Override
  public RuntimeException toThrowable(Response response) {
    int status = response.getStatus();                     // (3)

    String msg = getBody(response); // see below

    RuntimeException re ;
    switch (status) {
      case 412: re = new ValidationException(msg);         // (4)
      break;
      default:
        re = new WebApplicationException(status);          // (5)
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
