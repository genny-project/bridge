package bridge;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Base64;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.github.luben.zstd.Zstd;

import life.genny.bridge.EBCHandlers;

public class compressionTest {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	@Test
	public void compression2Test()
	{
		String originalStr = "hello";
		log.info("Original String=["+originalStr+"]");
		String encoded = "";
		try {
			encoded = EBCHandlers.compress3(originalStr);
			
			log.info("encoded=["+encoded+"]");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
	//	String in = "KLUv/WAEAuUQAAamdiMAp84Av+egZWahwQPKL0m8eU9pKU3ubjATXNGlqqqqqqqKEmkAbQBsABveusS/sbmMnuGIcohjqSEP1mY3JSt7c2MCb8L+DyzxEcu/feDNNsn3442WaKWY4bhhlr7BnMNmt0qV6IrdsFsVX7Euqs0AR4d4zRr+RjWoYsOICWCX+/FYH0SN4h1hMszPcrzcDfoRBFbXWtWgjOMBMNUwmc3tadH3Nrv5PQl3IdiTaYFf7stinUQmSqGMw62DJwLAVI9L1IRLQF7UqweZx4MSFDlglXPYzF5ujDJOfxTVpTWIZ4rqsUh9vWZxj89HVzQk0kz4ZKpO1VV4evPRftQs/dKFCIMADon/DteBh+sUmkT9Jod4NGkYJ/0dMIgXZJ0VpgX3+IwaS/XxHXia/wVmHGH+mEQDOz7j+a+IJDf0I+mhZdC32uxgYlCYFh1lnJ1kfRCZKK2BzS3KSnYCdem8+BDb7LBidVHThsnoTrcCAhGDV2yp5xWky+0tn70JzAjKrYIl5O3JFfek/kxUh8j6w3L3yKSzLJuGKWmvKzYjarNrMIhHW5UsSvSgeUOpOjhsdlFuiN1mB5/Bs616vKIdkYnqlNGPKKtukYsCnC3MatZFs1ZAn4U5BIxBARYAZwgOwuwQgZ+GtLPFWV7mw2WxM8L015r1chtf7cJ3W3K2arW6Xf6JtUcWvmpzCZG3KpidrAAhf6t/fzSCGQ==";
		String in = "KLUv/SAIQQAAYUdWc2JHOD0=";
		byte[] bytes = Base64.getDecoder().decode(in);
		// 77, 67, 119, 119, 76, 68, 65
		 byte[] ob = new byte[(int)Zstd.decompressedSize(bytes)];
	     Zstd.decompress(ob, bytes);
	     byte[] decoded = Base64.getDecoder().decode(ob);
	     log.info("decompressed=["+new String(decoded)+"]");
	}
	
}
