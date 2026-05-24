package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Task {

	public enum Priority {
		LOW,
		MEDIUM, HIGH
	}

	private final int id;
	private String title;
	private LocalDateTime reminderTime;
	private boolean completed;
	private boolean reminded;
	private Priority priority;
	private String description;

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	public Task(int id, String title, LocalDateTime reminderTime, Priority priority, String description) {
		this.id = id;
		this.title = title;
		this.reminderTime = reminderTime;
		this.completed = false;
		this.reminded = false;
		this.priority = (priority != null) ? priority : Priority.MEDIUM;
		this.description = (description != null) ? description : "";
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public LocalDateTime getReminderTime() {
		return reminderTime;
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

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		if (priority != null) {
			this.priority = priority;
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = (description != null) ? description : "";
	}

	private String priorityLabel() {
		switch (priority) {
		case HIGH:
			return "Alta";
		case LOW:
			return "Baja";
		default:
			return "Media";
		}
	}

	public String toDisplayString() {
		String estado = completed ? "✔ completada" : "⏳ pendiente";
		String fecha = reminderTime.format(FORMATTER);
		return "[" + id + "] " + fecha + " - " + title + " (Prioridad: " + priorityLabel() + ", " + estado + ")";
	}

	@Override
	public String toString() {
		return toDisplayString();
	}
}
