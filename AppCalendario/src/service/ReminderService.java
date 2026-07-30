package service;

import com.google.gson.reflect.TypeToken;
import model.Category;
import model.Reminder;
import model.Task;
import storage.JsonStorage;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ReminderService {

	private static final String FILE_PATH = "reminders.json";
	private static final Type LIST_TYPE = new TypeToken<List<Reminder>>() {
	}.getType();

	private final List<Reminder> reminders;
	private final AtomicInteger idGenerator;

	public ReminderService() {
		this.reminders = JsonStorage.load(FILE_PATH, LIST_TYPE);
		int maxId = 0;
		for (Reminder r : reminders) {
			if (r.getId() > maxId) {
				maxId = r.getId();
			}
		}
		this.idGenerator = new AtomicInteger(maxId + 1);
	}

	/** Crea y añade un recordatorio nuevo */
	public Reminder addReminder(String title, LocalDateTime reminderTime, Task.Priority priority, Category category) {
		int id = idGenerator.getAndIncrement();
		Reminder reminder = new Reminder(id, title, reminderTime, priority, category);
		reminders.add(reminder);
		save();
		return reminder;
	}

	/** Devuelve todos los recordatorios (solo lectura) */
	public List<Reminder> getAllReminders() {
		return Collections.unmodifiableList(reminders);
	}

	/**
	 * Devuelve los recordatorios cuya hora ya pasó y que aún no se han recordado
	 */
	public List<Reminder> getDueReminders(LocalDateTime now) {
		List<Reminder> result = new ArrayList<>();
		for (Reminder r : reminders) {
			if (!r.isCompleted() && !r.isReminded() && !now.isBefore(r.getReminderTime())) {
				result.add(r);
			}
		}
		return result;
	}

	/** Persiste el estado actual en disco. Llamar tras cualquier mutación. */
	public void save() {
		JsonStorage.save(FILE_PATH, reminders);
	}
}