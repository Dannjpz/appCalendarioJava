package model;

import java.time.LocalDateTime;

public class Note {

	private final int id;
	private String title;
	private String content;
	private Category category;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Note(int id, String title, String content, Category category) {
		this.id = id;
		this.title = title;
		this.content = (content != null) ? content : "";
		this.category = category;
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
		this.updatedAt = LocalDateTime.now();
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = (content != null) ? content : "";
		this.updatedAt = LocalDateTime.now();
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
		this.updatedAt = LocalDateTime.now();
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}