package life.genny.bridge;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import com.github.luben.zstd.Zstd;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
//import life.genny.channel.Consumer;
import life.genny.models.GennyToken;
import life.genny.qwandautils.GennySettings;
import life.genny.security.EncryptionUtils;
import life.genny.utils.VertxUtils;

public class EBCHandlers {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	//public static void registerHandlers() {

		//Consumer.getFromDirect().handler(arg -> {
			//String incomingCmd = arg.body().toString();
			//final JsonObject json = new JsonObject(incomingCmd); // Buffer.buffer(arg.toString().toString()).toJsonObject();
			//GennyToken userToken = new GennyToken("userToken", json.getString("token"));

			//bridgelog(userToken, json, ":target->" + Consumer.directIP, incomingCmd.length());

			//if (!incomingCmd.contains("<body>Unauthorized</body>")) {
				//sendToClientSessions(userToken, json, true);
			//}
		//}).exceptionHandler(exception -> {
			//log.info("Caught Exception handling mesage from direct");
			//log.error(exception.getStackTrace());
		//});

		@Incoming("webcmds")
		public void getFromWebCmds(String arg){
			//String incomingCmd = arg.body().toString();
			String incomingCmd = arg;
			if ("{}".equals(incomingCmd)) {
				log.error("Received empty {} in webcmds");
				return;
			}
			// final JsonObject json = new JsonObject(incomingCmd); //
			final JsonObject json = Buffer.buffer(incomingCmd).toJsonObject();
			if (json == null) {
				log.error("Json input is null!");
			} else {
				if (!StringUtils.isBlank(json.getString("token"))) {
					GennyToken userToken = new GennyToken("userToken", json.getString("token"));

					if ("Attribute".equals(json.getString("data_type"))) {
						JsonArray items = json.getJsonArray("items");
						if (!((items == null) || (items.isEmpty()))) {
							JsonObject attribute = items.getJsonObject(0);
							String code = attribute.getString("code");
							bridgelog(userToken, json, code, incomingCmd.length());
						}

					} else if ("BaseEntity".equals(json.getString("data_type"))) {
						JsonArray items = json.getJsonArray("items");
						if (!((items == null) || (items.isEmpty()))) {
							JsonObject be = items.getJsonObject(0);
							String code = be.getString("code");
							bridgelog(userToken, json, code, incomingCmd.length());
						}

					} else if ("CMD_BULKASK".equals(json.getString("cmd_type"))) {
						JsonObject asks = json.getJsonObject("asks");
						JsonArray items = asks.getJsonArray("items");
						if (!((items == null) || (items.isEmpty()))) {
							JsonObject ask = items.getJsonObject(0);
							String targetCode = ask.getString("targetCode");
							String questionCode = ask.getString("questionCode");
							bridgelog(userToken, json, ":target->" + targetCode + ":" + questionCode,
									incomingCmd.length());
						}

					} else if ("Ask".equals(json.getString("data_type"))) {
//					JsonArray items = json.getJsonArray("items");
//					
//					JsonObject ask = null;
//					
//					ask = items.getJsonObject(0);
//					String code = ask.getString("questionCode");
//					bridgelog(userToken,json,code,incomingCmd.length());

					} else if ("QBulkMessage".equals(json.getString("data_type"))) {
//					JsonArray items = json.getJsonArray("items");
//					JsonObject ask = items.getJsonObject(0);
						String code = "bulk"; // ask.getString("code");
						bridgelog(userToken, json, code, incomingCmd.length());

					} else {
						bridgelog(userToken, json, "UNKNOWN", incomingCmd.length());

					}
					if (!incomingCmd.contains("<body>Unauthorized</body>")) {
						sendToClientSessions(userToken, json, true);
					}
				} else {
					log.error("Null token sent to bridge");
				}
			}

		}
		//.exceptionHandler(exception -> {
			//log.info("Caught Exception handling mesage from webcmds");
			//log.error(exception.getStackTrace());
		//});

		@Incoming("webdata")
		public void getFromWebData(String arg)  {
			//String incomingData = arg.body().toString();
			String incomingData = arg;
			final JsonObject json = new JsonObject(incomingData); // Buffer.buffer(arg.toString().toString()).toJsonObject();
			GennyToken userToken = new GennyToken("userToken", json.getString("token"));

			bridgelog(userToken, json, userToken.getUserCode(), incomingData.length());
			if (!incomingData.contains("<body>Unauthorized</body>")) {
				sendToClientSessions(userToken, json, false);
			}
		}
		//.exceptionHandler(exception -> {
			//log.info("Caught Exception handling mesage from webdata");
			//log.error(exception.getStackTrace());
		//});

	private static void bridgelog(final GennyToken userToken, final JsonObject msg, final String code,
			Integer messageLength) {
		try {
			log.info("EVENT-BUS CMD  >> WEBSOCKET CMD  : " + userToken.getString("session_state") + " :"
					+ StringUtils.rightPad(msg.getString("data_type"), 12, " ") + ": size="
					+ StringUtils.rightPad(messageLength + "", 6, " ") + " Code=" + StringUtils.rightPad(code, 32, " ")
					+ " :[" + userToken.getUserCode() + "] ");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param incomingCmd
	 * @throws IOException
	 */
	public static void sendToClientSessions(final GennyToken userToken, final JsonObject json, boolean sessionOnly) {

		JsonArray recipientJsonArray = new JsonArray();
		;

		if ((!json.containsKey("recipientCodeArray")) || (json.getJsonArray("recipientCodeArray").isEmpty())) {
			recipientJsonArray.add(userToken.getUserCode());
		} else {
			// encrypt
			JsonArray unencryptedJsonArray = json.getJsonArray("recipientCodeArray");
			for (int i = 0; i < unencryptedJsonArray.size(); i++) {
				String channelCode = unencryptedJsonArray.getString(i);
				String securityKey = GennySettings.defaultServiceKey;
				String encryptedChannel = EncryptionUtils.getEncryptedString(channelCode, securityKey, userToken);
				recipientJsonArray.add(encryptedChannel);
			}
		}

		json.remove("token"); // do not show the token
		json.remove("recipientCodeArray"); // do not show the other recipients
		JsonObject cleanJson = null; //

		cleanJson = json; // removePrivates(json, tokenJSON, sessionOnly, userCode);
		if (cleanJson == null) {
			log.error("null json");
		}

		String txt = cleanJson.toString();
//			if (bulkPull) {
//				QBulkPullMessage msg = BaseEntityUtils.createQBulkPullMessage(cleanJson);
//				cleanJson = new JsonObject(JsonUtils.toJson(msg));
//			}

		int originalSize = cleanJson.toString().length();

		try {
			if (originalSize > GennySettings.zipMinimumThresholdBytes) { // 2^19-1
				long startTime = System.nanoTime();
				// log.info("ZIPPING!");
				;
				if (GennySettings.zipMode) {
					String js = compressAndEncodeString(cleanJson.toString());
					cleanJson = new JsonObject();
					cleanJson.put("zip", js);
				} else if (GennySettings.gzipMode) {
					String js = compress3(cleanJson.toString());

					// log.info("encoded["+js);
					cleanJson = new JsonObject();
					cleanJson.put("zip", js);
				} else if (GennySettings.gzip64Mode) {
					byte[] js = zipped(cleanJson.toString());
					cleanJson = new JsonObject();
					cleanJson.put("zip", js);
				} else {
					String js = compress(cleanJson.toString());
					cleanJson = new JsonObject();
					cleanJson.put("zip", js);
				}

				long endTime = System.nanoTime();
				double difference = (endTime - startTime) / 1e6; // get ms
				int finalSize = cleanJson.toString().length();
				log.info("Sending ZIPPED " + originalSize + " bytes  compressed to " + finalSize
						+ " bytes with threshold = " + GennySettings.zipMinimumThresholdBytes + " "
						+ ((int) (((double) finalSize * 100) / ((double) originalSize))) + "% in " + difference + "ms");
			}
		} catch (Exception e) {
			log.error("CANNOT Compress json");

		}

		if (sessionOnly) {
			String sessionState = userToken.getString("session_state");
			sendToSession(sessionState, cleanJson);
		} else {
			for (int i = 0; i < recipientJsonArray.size(); i++) {
				String channelCode = recipientJsonArray.getString(i);
				if (!channelCode.startsWith("PER_")) {
					sendToSession(channelCode, cleanJson);
				} else {
//				// Get all the sessionStates for this user
//
					Set<String> sessionStates = VertxUtils.getSetString(userToken.getRealm(), "SessionStates", channelCode);

					if (((sessionStates != null) && (!sessionStates.isEmpty()))) {

						for (String sessionState : sessionStates) {
							sendToSession(sessionState, cleanJson);
						}
					} else {
						sendToSession(userToken.getString("session_state"), cleanJson); // have to send to something
						// no sessions for this user!
						// need to remove them from subscriptions ...
						// log.error("Remove " + recipientCode + " from subscriptions , they have no
						// sessions");
					}
				}
			}
		}

	}// 0kZ0TI4tetjQHG84U/VdwA

	private static void sendToSession(String channel, JsonObject cleanJson) {
		MessageProducer<JsonObject> msgProducer = VertxUtils.getMessageProducer(channel);
		if (msgProducer == null) {
			msgProducer = Vertx.currentContext().owner().eventBus().publisher(channel);
			VertxUtils.putMessageProducer(channel, msgProducer); // save
		}
		if (msgProducer != null) {
			if (msgProducer.writeQueueFull()) {
				log.error("WEBSOCKET >> producer buffer is full hence message cannot be sent");
				msgProducer.write(cleanJson);
			} else {
				msgProducer.write(cleanJson);
			}
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
			log.info("could not compress data: " + e);
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
