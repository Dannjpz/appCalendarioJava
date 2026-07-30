package service;

import com.google.gson.reflect.TypeToken;
import model.Category;
import model.Project;
import storage.JsonStorage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectService {

	private static final String FILE_PATH = "projects.json";
	private static final Type LIST_TYPE = new TypeToken<List<Project>>() {
	}.getType();

	private static final String TASF_ROOT = "C:\\Proyectos\\TASF";
	private static final String GNP_ROOT = "C:\\Proyectos\\GNP";

	private final List<Project> projects;
	private final AtomicInteger idGenerator;

	public ProjectService() {
		this.projects = JsonStorage.load(FILE_PATH, LIST_TYPE);
		int maxId = 0;
		for (Project p : projects) {
			if (p.getId() > maxId) {
				maxId = p.getId();
			}
		}
		this.idGenerator = new AtomicInteger(maxId + 1);
	}

	/** Ruta raíz para una categoría (TASF o GNP). */
	public static String getRootPath(Category category) {
		return (category == Category.TASF) ? TASF_ROOT : GNP_ROOT;
	}

	/** Indica si la carpeta raíz de esa categoría existe en este equipo. */
	public static boolean isCategoryAvailable(Category category) {
		return new File(getRootPath(category)).isDirectory();
	}

	/**
	 * Ruta a una subcarpeta específica del proyecto (documentacion, ambientacion,
	 * codigo).
	 */
	public static String getSubfolderPath(Project project, String subfolder) {
		return getRootPath(project.getCategory()) + File.separator + project.getName() + File.separator + subfolder;
	}

	/** Crea el proyecto y su estructura de carpetas en disco. */
	public Project addProject(String name, Category category) throws IOException {
		int id = idGenerator.getAndIncrement();
		Project project = new Project(id, name, category);

		Path base = Paths.get(getRootPath(category), name);
		Files.createDirectories(base.resolve("documentacion"));
		Files.createDirectories(base.resolve("ambientacion"));
		Files.createDirectories(base.resolve("codigo"));

		projects.add(project);
		save();
		return project;
	}

	public List<Project> getAllProjects() {
		return Collections.unmodifiableList(projects);
	}

	public List<Project> getProjectsByCategory(Category category) {
		List<Project> result = new ArrayList<>();
		for (Project p : projects) {
			if (p.getCategory() == category) {
				result.add(p);
			}
		}
		return result;
	}

	/**
	 * Renombra el proyecto: actualiza el nombre y renombra la carpeta física
	 * correspondiente.
	 */
	public boolean renameProject(int id, String newName) throws IOException {
		for (Project p : projects) {
			if (p.getId() == id) {
				File oldFolder = new File(getRootPath(p.getCategory()), p.getName());
				File newFolder = new File(getRootPath(p.getCategory()), newName);
				if (oldFolder.exists() && !oldFolder.renameTo(newFolder)) {
					throw new IOException("No se pudo renombrar la carpeta física del proyecto.");
				}
				p.setName(newName);
				save();
				return true;
			}
		}
		return false;
	}

	/** Elimina el proyecto SOLO de la app (no borra la carpeta física en disco). */
	public boolean deleteProject(int id) {
		boolean removed = projects.removeIf(p -> p.getId() == id);
		if (removed) {
			save();
		}
		return removed;
	}

	public void save() {
		JsonStorage.save(FILE_PATH, projects);
	}
}