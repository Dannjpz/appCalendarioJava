package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JsonStorage {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
				@Override
				public void write(JsonWriter out, LocalDateTime value) throws IOException {
					if (value == null) {
						out.nullValue();
					} else {
						out.value(value.format(FORMATTER));
					}
				}

				@Override
				public LocalDateTime read(JsonReader in) throws IOException {
					String text = in.nextString();
					return LocalDateTime.parse(text, FORMATTER);
				}
			}).setPrettyPrinting().create();

	public static <T> List<T> load(String filePath, Type listType) {
		File file = new File(filePath);
		if (!file.exists()) {
			return new ArrayList<>();
		}
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
			List<T> result = GSON.fromJson(reader, listType);
			return (result != null) ? result : new ArrayList<>();
		} catch (IOException e) {
			System.err.println("Error leyendo " + filePath + ":");
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/** Guarda una lista de objetos en un archivo JSON. */
	public static <T> void save(String filePath, List<T> data) {
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath),
				StandardCharsets.UTF_8)) {
			GSON.toJson(data, writer);
		} catch (IOException e) {
			System.err.println("Error guardando " + filePath + ":");
			e.printStackTrace();
		}
	}
}