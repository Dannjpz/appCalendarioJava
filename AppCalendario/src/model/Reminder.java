package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Reminder {

	private final int id;
	private String title;
	private LocalDateTime reminderTime;
	private Task.Priority priority;
	private Category category;
	private boolean completed;
	private boolean reminded;

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

	public Reminder(int id, String title, LocalDateTime reminderTime, Task.Priority priority, Category category) {
		this.id = id;
		this.title = title;
		this.reminderTime = reminderTime;
		this.priority = (priority != null) ? priority : Task.Priority.MEDIUM;
		this.category = category;
		this.completed = false;
		this.reminded = false;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public LocalDateTime getReminderTime() {
		return reminderTime;
	}

	public void setReminderTime(LocalDateTime reminderTime) {
		this.reminderTime = reminderTime;
	}

	public Task.Priority getPriority() {
		return priority;
	}

	public void setPriority(Task.Priority priority) {
		if (priority != null) {
			this.priority = priority;
		}
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public boolean isReminded() {
		return reminded;
	}

	public void setReminded(boolean reminded) {
		this.reminded = reminded;
	}

	public String toDisplayString() {
		String estado = completed ? "✔ completado" : "⏳ pendiente";
		String fecha = reminderTime.format(FORMATTER);
		return "[" + category + "] [" + id + "] " + fecha + " - " + title + " (" + estado + ")";
	}

	@Override
	public String toString() {
		return toDisplayString();
	}
}