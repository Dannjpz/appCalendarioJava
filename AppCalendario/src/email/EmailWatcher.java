package email;

import whatsapp.WhatsAppClient;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Properties;

public class EmailWatcher {

	private final String host;
	private final int port;
	private final String username;
	private final String password;
	private final boolean useSSL;

	// Para no procesar correos muy viejos: empezamos "hace 5 minutos"
	private LocalDateTime lastCheckTime = LocalDateTime.now().minusMinutes(5);

	public EmailWatcher(String host, int port, String username, String password, boolean useSSL) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.useSSL = useSSL;
	}

	public void checkNewEmails() {
		try {
			Properties props = new Properties();
			if (useSSL) {
				props.put("mail.store.protocol", "imaps");
			} else {
				props.put("mail.store.protocol", "imap");
			}

			Session session = Session.getInstance(props);
			Store store = session.getStore(useSSL ? "imaps" : "imap");

			// Conexión al servidor de correo
			store.connect(host, port, username, password);

			Folder inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_ONLY);

			Message[] messages = inbox.getMessages();
			if (messages.length == 0) {
				inbox.close(false);
				store.close();
				return;
			}

			LocalDateTime newLastCheck = lastCheckTime;

			// Recorremos de más nuevo a más viejo
			for (int i = messages.length - 1; i >= 0; i--) {
				Message msg = messages[i];
				Date receivedDate = msg.getReceivedDate();
				if (receivedDate == null) {
					continue;
				}

				LocalDateTime receivedLdt = receivedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

				if (receivedLdt.isAfter(lastCheckTime)) {
					// Es un correo nuevo desde la última revisión
					notifyViaWhatsApp(msg);

					if (receivedLdt.isAfter(newLastCheck)) {
						newLastCheck = receivedLdt;
					}
				} else {
					// Si ya llegamos a correos más antiguos que el último check, podemos parar
					break;
				}
			}

			lastCheckTime = newLastCheck;

			inbox.close(false);
			store.close();
		} catch (Exception e) {
			System.err.println("Error revisando correos:");
			e.printStackTrace();
		}
	}

	private void notifyViaWhatsApp(Message msg) {
		try {
			Address[] from = msg.getFrom();
			String fromText = "(sin remitente)";
			if (from != null && from.length > 0) {
				if (from[0] instanceof InternetAddress) {
					InternetAddress ia = (InternetAddress) from[0];
					String name = ia.getPersonal();
					String email = ia.getAddress();
					if (name != null && !name.isEmpty()) {
						fromText = name + " <" + email + ">";
					} else {
						fromText = email;
					}
				} else {
					fromText = from[0].toString();
				}
			}

			String subject = msg.getSubject();
			if (subject == null)
				subject = "(sin asunto)";

			// Aquí el mensaje que te llegará a WhatsApp
			String mensaje = "📩 Nuevo correo en tu bandeja\n" + "De: " + fromText + "\n" + "Asunto: " + subject;

			WhatsAppClient.sendTextMessage(mensaje);
		} catch (Exception e) {
			System.err.println("Error al preparar notificación de WhatsApp:");
			e.printStackTrace();
		}
	}
}
