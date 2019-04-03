package life.genny.bridge;

import java.io.ByteArrayOutputStream;
import com.github.luben.zstd.Zstd;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.google.gson.Gson;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.MessageProducer;
import life.genny.channel.Consumer;
import life.genny.qwanda.message.QBulkPullMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.VertxUtils;

public class EBCHandlers {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	private static Boolean bulkPull = (System.getenv("BULKPULL") != null)
			? "TRUE".equalsIgnoreCase(System.getenv("BULKPULL"))
			: false;

	public static void registerHandlers() {

		Consumer.getFromWebCmds().subscribe(arg -> {
			// log.info("EVENT-BUS CMD >> WEBSOCKET CMD :"+incomingCmd);
			String incomingCmd = arg.body().toString();

			if (!incomingCmd.contains("<body>Unauthorized</body>")) {
				sendToClientSessions(incomingCmd, true);
			}
		});

		Consumer.getFromWebData().subscribe(arg -> {
			String incomingData = arg.body().toString();
			final JsonObject json = new JsonObject(incomingData); // Buffer.buffer(arg.toString().toString()).toJsonObject();
			log.info("EVENT-BUS DATA >> WEBSOCKET DATA2:" + json.getString("data_type") + ": size->" + json.size());

			if (!incomingData.contains("<body>Unauthorized</body>")) {
				sendToClientSessions(incomingData, false);
			}
		});
	}

	/**
	 * @param incomingCmd
	 * @throws IOException
	 */
	public static void sendToClientSessions(String incomingCmd, boolean sessionOnly) {
		// ugly, but remove the outer array
		if (incomingCmd.startsWith("[")) {
			incomingCmd = incomingCmd.replaceFirst("\\[", "");
			incomingCmd = incomingCmd.substring(0, incomingCmd.length() - 1);
		}

		final JsonObject json = new JsonObject(incomingCmd); // Buffer.buffer(arg.toString().toString()).toJsonObject();
		log.info("EVENT-BUS CMD  >> WEBSOCKET CMD :" + json.getString("cmd_type") + ":" + json.getString("code")
				+ (GennySettings.zipMode ? "ZIPPED" : " "));

		if (json.getString("token") != null) {
			// check token
			JsonArray recipientJsonArray = null;
			JSONObject tokenJSON = KeycloakUtils.getDecodedToken(json.getString("token"));
			String uname = QwandaUtils.getNormalisedUsername(tokenJSON.getString("preferred_username"));
			String userCode = "PER_" + uname.toUpperCase();

			if ((!json.containsKey("recipientCodeArray")) || (json.getJsonArray("recipientCodeArray").isEmpty())) {
				recipientJsonArray = new JsonArray();

				recipientJsonArray.add(userCode);
			} else {
				recipientJsonArray = json.getJsonArray("recipientCodeArray");
			}

			json.remove("token"); // do not show the token
			json.remove("recipientCodeArray"); // do not show the other recipients
			JsonObject cleanJson = null; //

			cleanJson = json; // removePrivates(json, tokenJSON, sessionOnly, userCode);
			if (cleanJson == null) {
				log.error("null json");
			}
			if (bulkPull) {
				QBulkPullMessage msg = BaseEntityUtils.createQBulkPullMessage(cleanJson);
				cleanJson = new JsonObject(JsonUtils.toJson(msg));
			}

			int originalSize = cleanJson.toString().length();
			if (GennySettings.zipMode) {
				long startTime = System.nanoTime();
				try {

					log.info("ZIPPING!");
					;
					if ("TRUE".equalsIgnoreCase(System.getenv("MODE_ZIP"))) {
						String js = compressAndEncodeString(cleanJson.toString());
						cleanJson = new JsonObject();
						cleanJson.put("zip", js);
					} else if ("TRUE".equalsIgnoreCase(System.getenv("MODE_GZIP"))) {
						String js = compress3(cleanJson.toString());
						
						//System.out.println("encoded["+js);
						cleanJson = new JsonObject();
						cleanJson.put("zip",js);
					} else if ("TRUE".equalsIgnoreCase(System.getenv("MODE_GZIP64"))) {
						byte[] js = zipped(cleanJson.toString());
						cleanJson = new JsonObject();
						cleanJson.put("zip", js);
					} else {
						String js = compress(cleanJson.toString());
						cleanJson = new JsonObject();
						cleanJson.put("zip", js);
					}

				} catch (Exception e) {
					log.error("CANNOT Compress json");

				}
				
				long endTime = System.nanoTime();
				double difference = (endTime - startTime) / 1e6; // get ms
				int finalSize = cleanJson.toString().length();
				log.info("Sending "+originalSize+" bytes  compressed to " + finalSize + " bytes "+((int)(((double)finalSize*100)/((double)originalSize)))+" % in "+difference+"ms");
			}

			
			
			if (sessionOnly) {
				String sessionState = tokenJSON.getString("session_state");
				MessageProducer<JsonObject> msgProducer = VertxUtils.getMessageProducer(sessionState);
				if (msgProducer != null) {
					
					msgProducer.write(cleanJson).end();
				//	log.info("Sent to " + sessionState );
				}
			} else {
				for (int i = 0; i < recipientJsonArray.size(); i++) {
					String recipientCode = recipientJsonArray.getString(i);
					// Get all the sessionStates for this user

					Set<String> sessionStates = VertxUtils.getSetString("", "SessionStates", recipientCode);

					if (((sessionStates != null) && (!sessionStates.isEmpty()))) {

						// sessionStates.add(tokenJSON.getString("session_state")); // commenting this
						// one, since current
						// user was getting added to the
						// toast recipients
						log.info("User:" + recipientCode + " with " + sessionStates.size() + " sessions");
						for (String sessionState : sessionStates) {

							MessageProducer<JsonObject> msgProducer = VertxUtils.getMessageProducer(sessionState);
							// final MessageProducer<JsonObject> msgProducer =
							// Vertx.currentContext().owner().eventBus().publisher(sessionState);
							if (msgProducer != null) {

								msgProducer.write(cleanJson).end();
							}

						}
					} else {
						// no sessions for this user!
						// need to remove them from subscriptions ...
						log.error("Remove " + recipientCode + " from subscriptions , they have no sessions");
					}
				}
			}

		} else {
			log.error("Cmd with Unauthorised cmd recieved");
		}
	}

	public static String compress(String str) throws IOException {
		if (str == null || str.length() == 0) {
			return str;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(str.getBytes());
		gzip.close();
		String outStr = out.toString("UTF-8");
		return outStr;
	}

	public static byte[] zipped(final String str) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		Base64OutputStream base64OutputStream = new Base64OutputStream(byteStream);
		GZIPOutputStream gzip = new GZIPOutputStream(base64OutputStream);
		OutputStreamWriter writer = new OutputStreamWriter(gzip);
		Gson gson = new Gson();
		gson.toJson(str, writer);
		writer.flush();
		gzip.finish();
		writer.close();
		return byteStream.toByteArray();
	}

	public static String compressAndEncodeString(String str) {
		DeflaterOutputStream def = null;
		String compressed = null;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			// create deflater without header
			def = new DeflaterOutputStream(out, new Deflater(Deflater.BEST_COMPRESSION, true));
			def.write(str.getBytes());
			def.close();
			compressed = out.toString("UTF-8");
		} catch (Exception e) {
			System.out.println("could not compress data: " + e);
		}
		return compressed;
	}

	public static byte[] compress2(String data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
		GZIPOutputStream gzip = new GZIPOutputStream(bos);
		gzip.write(data.getBytes());
		gzip.close();
		byte[] compressed = bos.toByteArray();
		bos.close();
		return compressed;
	}

	public static String compress3(String data) throws IOException {
		byte[] encodedBytes = Base64.getEncoder().encode(data.getBytes());
		byte[] bytes = Zstd.compress(encodedBytes); // 40 181 47 253
		String encoded = Base64.getEncoder().encodeToString(bytes);

		return encoded;
	}

	public static String decompress(final String base64compressedString) {
		byte[] bytes = Base64.getDecoder().decode(base64compressedString);
		byte[] ob = new byte[(int) Zstd.decompressedSize(bytes)];
		Zstd.decompress(ob, bytes);
		byte[] decoded = Base64.getDecoder().decode(ob);
		return new String(decoded);
	}
}
