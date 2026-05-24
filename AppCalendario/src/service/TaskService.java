package service;

import model.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskService {

	private final List<Task> tasks = new ArrayList<>();
	private final AtomicInteger idGenerator = new AtomicInteger(1);

	/** Crea y añade una tarea nueva */
	public Task addTask(String title, LocalDateTime reminderTime, Task.Priority priority, String description) {
		int id = idGenerator.getAndIncrement();
		Task task = new Task(id, title, reminderTime, priority, description);
		tasks.add(task);
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
}
