package service;

import com.google.gson.reflect.TypeToken;
import model.Category;
import model.Note;
import storage.JsonStorage;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class NoteService {

	private static final String FILE_PATH = "notes.json";
	private static final Type LIST_TYPE = new TypeToken<List<Note>>() {
	}.getType();

	private final List<Note> notes;
	private final AtomicInteger idGenerator;

	public NoteService() {
		this.notes = JsonStorage.load(FILE_PATH, LIST_TYPE);
		int maxId = 0;
		for (Note n : notes) {
			if (n.getId() > maxId) {
				maxId = n.getId();
			}
		}
		this.idGenerator = new AtomicInteger(maxId + 1);
	}

	/** Crea y añade una nota nueva */
	public Note addNote(String title, String content, Category category) {
		int id = idGenerator.getAndIncrement();
		Note note = new Note(id, title, content, category);
		notes.add(note);
		save();
		return note;
	}

	/** Devuelve todas las notas (solo lectura) */
	public List<Note> getAllNotes() {
		return Collections.unmodifiableList(notes);
	}

	/** Devuelve las notas filtradas por categoría */
	public List<Note> getNotesByCategory(Category category) {
		List<Note> result = new ArrayList<>();
		for (Note n : notes) {
			if (n.getCategory() == category) {
				result.add(n);
			}
		}
		return result;
	}

	/** Actualiza título y contenido de una nota existente por id */
	public boolean updateNote(int id, String title, String content) {
		Optional<Note> found = notes.stream().filter(n -> n.getId() == id).findFirst();
		if (found.isPresent()) {
			Note n = found.get();
			n.setTitle(title);
			n.setContent(content);
			save();
			return true;
		}
		return false;
	}

	/** Elimina una nota por id */
	public boolean deleteNote(int id) {
		boolean removed = notes.removeIf(n -> n.getId() == id);
		if (removed) {
			save();
		}
		return removed;
	}

	/** Persiste el estado actual en disco. */
	public void save() {
		JsonStorage.save(FILE_PATH, notes);
	}
}