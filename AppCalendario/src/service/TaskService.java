package service;

import com.google.gson.reflect.TypeToken;
import model.Task;
import storage.JsonStorage;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskService {

	private static final String FILE_PATH = "tasks.json";
	private static final Type LIST_TYPE = new TypeToken<List<Task>>() {
	}.getType();

	private final List<Task> tasks;
	private final AtomicInteger idGenerator;

	public TaskService() {
		this.tasks = JsonStorage.load(FILE_PATH, LIST_TYPE);
		int maxId = 0;
		for (Task t : tasks) {
			if (t.getId() > maxId) {
				maxId = t.getId();
			}
		}
		this.idGenerator = new AtomicInteger(maxId + 1);
	}

	/** Crea y añade una tarea nueva */
	public Task addTask(String title, LocalDateTime reminderTime, Task.Priority priority, String description,
			model.Category category) {
		int id = idGenerator.getAndIncrement();
		Task task = new Task(id, title, reminderTime, priority, description, category);
		tasks.add(task);
		save();
		return task;
	}

	/** Devuelve todas las tareas (solo lectura) */
	public List<Task> getAllTasks() {
		return Collections.unmodifiableList(tasks);
	}

	/** Devuelve las tareas cuya hora ya pasó y que aún no se han recordado */
	public List<Task> getDueTasks(LocalDateTime now) {
		List<Task> result = new ArrayList<>();
		for (Task t : tasks) {
			if (!t.isCompleted() && !t.isReminded() && !now.isBefore(t.getReminderTime())) {
				result.add(t);
			}
		}
		return result;
	}

	/** Elimina una tarea por id */
	public boolean deleteTask(int id) {
		boolean removed = tasks.removeIf(t -> t.getId() == id);
		if (removed) {
			save();
		}
		return removed;
	}

	/**
	 * Persiste el estado actual en disco. Llamar tras cualquier mutación
	 * (completar, marcar recordada, etc.)
	 */
	public void save() {
		JsonStorage.save(FILE_PATH, tasks);
	}
}