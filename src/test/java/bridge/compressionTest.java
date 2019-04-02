package bridge;

import java.io.IOException;
import java.util.Base64;

import org.junit.Test;

import com.github.luben.zstd.Zstd;

import life.genny.bridge.EBCHandlers;

public class compressionTest {


	@Test
	public void compression2Test()
	{
		System.out.println("This is a bridge test");
		byte[] encodedBytes = Base64.getEncoder().encode("hello".getBytes());
		byte[] bytes =   Zstd.compress(encodedBytes);			// 40 181 47 253
		String encoded = Base64.getEncoder().encodeToString(bytes);
		
		System.out.println("encoded=["+encoded+"]");
	}
	
}
