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
		String originalStr = "hello";
		System.out.println("Original String=["+originalStr+"]");

		try {
			String encoded = EBCHandlers.compress3(originalStr);
			
			System.out.println("encoded=["+encoded+"]");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		String in = "KLUv/SAIQQAAYUdWc2JHOD0=";
		byte[] bytes = Base64.getDecoder().decode(in);
		 byte[] ob = new byte[(int)Zstd.decompressedSize(bytes)];
	     Zstd.decompress(ob, bytes);
	     byte[] decoded = Base64.getDecoder().decode(ob);
	     System.out.println("decompressed=["+new String(decoded)+"]");
	}
	
}
