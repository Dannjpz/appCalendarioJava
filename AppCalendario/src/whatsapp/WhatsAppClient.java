package whatsapp;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WhatsAppClient {

	// TODO: reemplaza estos valores cuando tengas tu configuración real
	private static final String ACCESS_TOKEN = "EAAR0Bf9FadABRKdg4FCMZCO6PZAgT0iQMwUzkU5tzmB26jauIKi66DvIH3a2bUWbnKB7PdNortuleAvazs0f1H7Gt95JKp1NS30DtJqtqf2mMToKi9ZCgVuCWqos3gDPTblj7MFH6FQQXywqLpo5mZBVKgrrDCZA97cDHHxyYlxVdvZA0SXhK3LDlMZBXJGAiHNtqL9kw1ltZBlRs6G7nHcizV8WYzIG7j1XALUcfuWNAA1nr9ZCT7vDukLZAnnXBsDfIU9wwI2EhOExvPZCOtH8SaQ";
	private static final String PHONE_NUMBER_ID = "1133039249882181";
	private static final String RECIPIENT_NUMBER = "522228739509";

	/**
	 * Envía un mensaje de texto simple por WhatsApp usando WhatsApp Cloud API.
	 */
	public static void sendTextMessage(String body) {
		try {
			String urlString = "https://graph.facebook.com/v25.0/" + PHONE_NUMBER_ID + "/messages";
			URL url = new URL(urlString);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setDoOutput(true);

			String safeBody = escapeForJson(body);

			String jsonPayload = "{" + "\"messaging_product\":\"whatsapp\"," + "\"to\":\"" + RECIPIENT_NUMBER + "\","
					+ "\"type\":\"text\"," + "\"text\":{\"body\":\"" + safeBody + "\"}" + "}";

			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = jsonPayload.getBytes("UTF-8");
				os.write(input);
			}

			int status = conn.getResponseCode();

			java.io.InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

			java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A");
			String response = scanner.hasNext() ? scanner.next() : "";

			if (status / 100 != 2) {
				System.err.println("❌ Error WhatsApp. HTTP: " + status);
				System.err.println("Respuesta: " + response);
			} else {
				System.out.println("✅ Mensaje enviado correctamente");
				System.out.println("Respuesta: " + response);
			}

			conn.disconnect();
		} catch (Exception e) {
			System.err.println("Error enviando mensaje por WhatsApp:");
			e.printStackTrace();
		}
	}

	/**
	 * Escapa comillas para evitar romper el JSON simple.
	 */
	private static String escapeForJson(String text) {
		if (text == null)
			return "";
		return text.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
