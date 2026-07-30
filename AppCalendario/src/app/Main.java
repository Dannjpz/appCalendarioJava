package app;

import model.Category;
import model.Note;
import model.Project;
import model.Reminder;
import model.Task;
import service.NoteService;
import service.ProjectService;
import service.ReminderService;
import service.TaskService;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import ui.DatePickerDialog;
import whatsapp.WhatsAppClient;

import javax.swing.*;

import email.EmailWatcher;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import javax.swing.DefaultListModel;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CannotRedoException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

	private static TaskService taskService = new TaskService();
	private static ReminderService reminderService = new ReminderService();
	private static NoteService noteService = new NoteService();
	private static ProjectService projectService = new ProjectService();

	// Referencia al frame principal para diálogos
	private static JFrame mainFrame;

	// Áreas de texto para vistas
	private static JPanel listContainer;
	private static JPanel dayContainer;
	private static JTextArea reminderListArea;

	// Notas
	private static DefaultListModel<Note> notesListModel;
	private static JList<Note> notesJList;
	private static JTextField noteTitleField;
	private static JTextArea noteContentArea;
	private static JComboBox<String> noteFilterCombo;
	private static Note selectedNote; // nota actualmente cargada en el editor
	private static boolean loadingNote = false; // evita que la carga programática dispare autoguardado
	private static Timer noteAutoSaveTimer;

	// Proyectos
	private static JPanel projectsTasfContainer;
	private static JPanel projectsGnpContainer;

	// Campos para filtros
	private static JTextField dayDateField;
	private static JTextField weekDateField;
	private static JTextField monthField;
	private static JTextField yearField;

	// Paneles de calendario
	private static JPanel weekDaysPanel;
	private static JPanel monthDaysPanel;

	// Scheduler de recordatorios
	private static ScheduledExecutorService scheduler;
	private static ScheduledExecutorService emailScheduler;

	// Formatos
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	public static void main(String[] args) {
		SwingUtilities.invokeLater(Main::createAndShowGUI);
	}

	private static void createAndShowGUI() {
		WhatsAppClient.sendTextMessage("Prueba desde Java");
		JFrame frame = new JFrame("App Calendario");
		try {
			java.awt.image.BufferedImage iconImage = javax.imageio.ImageIO.read(new File("png.png"));
			if (iconImage == null) {
				System.err.println("El ícono no se pudo leer (formato no reconocido o archivo corrupto): png.png");
			} else {
				frame.setIconImage(iconImage);
				System.out
						.println("Ícono cargado correctamente: " + iconImage.getWidth() + "x" + iconImage.getHeight());
			}
		} catch (IOException ex) {
			System.err.println("No se encontró el archivo de ícono: " + ex.getMessage());
		}
		mainFrame = frame;
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try {
			frame.setIconImage(new ImageIcon("icono.png").getImage());
		} catch (Exception ex) {
			System.err.println("No se pudo cargar el ícono de la app: " + ex.getMessage());
		}
		frame.setSize(950, 650);

		LocalDate today = LocalDate.now();

		// ---------- TAB 1: VISTA LISTA ----------
		listContainer = new JPanel();
		listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
		JScrollPane listScroll = new JScrollPane(listContainer);
		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.add(listScroll, BorderLayout.CENTER);

		// ---------- TAB 2: VISTA DÍA ----------
		dayContainer = new JPanel();
		dayContainer.setLayout(new BoxLayout(dayContainer, BoxLayout.Y_AXIS));
		JScrollPane dayScroll = new JScrollPane(dayContainer);

		dayDateField = new JTextField(10);
		dayDateField.setText(today.format(DATE_FORMAT));
		JButton dayFilterButton = new JButton("Mostrar");
		dayFilterButton.addActionListener(e -> refreshDayViewFromField());

		JPanel dayTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
		dayTop.add(new JLabel("Fecha (dd/MM/yyyy): "));
		dayTop.add(dayDateField);
		dayTop.add(dayFilterButton);

		JPanel dayPanel = new JPanel(new BorderLayout());
		dayPanel.add(dayTop, BorderLayout.NORTH);
		dayPanel.add(dayScroll, BorderLayout.CENTER);

		// ---------- TAB 3: VISTA SEMANA ----------
		weekDateField = new JTextField(10);
		weekDateField.setText(today.format(DATE_FORMAT));
		JButton weekFilterButton = new JButton("Mostrar");
		weekFilterButton.addActionListener(e -> refreshWeekViewFromField());

		JPanel weekTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
		weekTop.add(new JLabel("Fecha (dd/MM/yyyy) dentro de la semana: "));
		weekTop.add(weekDateField);
		weekTop.add(weekFilterButton);

		JPanel weekHeaderRow = new JPanel(new GridLayout(1, 7));
		String[] dayNames = { "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom" };
		for (String dn : dayNames) {
			JLabel lbl = new JLabel(dn, SwingConstants.CENTER);
			lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
			weekHeaderRow.add(lbl);
		}

		weekDaysPanel = new JPanel(new GridLayout(1, 7, 5, 5));
		JScrollPane weekScroll = new JScrollPane(weekDaysPanel);

		JPanel weekCenter = new JPanel(new BorderLayout());
		weekCenter.add(weekHeaderRow, BorderLayout.NORTH);
		weekCenter.add(weekScroll, BorderLayout.CENTER);

		JPanel weekPanel = new JPanel(new BorderLayout());
		weekPanel.add(weekTop, BorderLayout.NORTH);
		weekPanel.add(weekCenter, BorderLayout.CENTER);

		// ---------- TAB 4: VISTA MES ----------
		monthField = new JTextField(3);
		yearField = new JTextField(5);
		monthField.setText(String.valueOf(today.getMonthValue()));
		yearField.setText(String.valueOf(today.getYear()));
		JButton monthFilterButton = new JButton("Mostrar");
		monthFilterButton.addActionListener(e -> refreshMonthViewFromFields());

		JPanel monthTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
		monthTop.add(new JLabel("Mes (1-12): "));
		monthTop.add(monthField);
		monthTop.add(new JLabel("Año: "));
		monthTop.add(yearField);
		monthTop.add(monthFilterButton);

		JPanel monthHeaderRow = new JPanel(new GridLayout(1, 7));
		for (String dn : dayNames) {
			JLabel lbl = new JLabel(dn, SwingConstants.CENTER);
			lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
			monthHeaderRow.add(lbl);
		}

		monthDaysPanel = new JPanel(new GridLayout(0, 7, 5, 5));
		JScrollPane monthScroll = new JScrollPane(monthDaysPanel);

		JPanel monthCenter = new JPanel(new BorderLayout());
		monthCenter.add(monthHeaderRow, BorderLayout.NORTH);
		monthCenter.add(monthScroll, BorderLayout.CENTER);

		JPanel monthPanel = new JPanel(new BorderLayout());
		monthPanel.add(monthTop, BorderLayout.NORTH);
		monthPanel.add(monthCenter, BorderLayout.CENTER);

		// ---------- TAB 5: RECORDATORIOS ----------
		reminderListArea = new JTextArea();
		reminderListArea.setEditable(false);
		JScrollPane reminderScroll = new JScrollPane(reminderListArea);

		JButton addReminderButton = new JButton("Agregar recordatorio");
		addReminderButton.addActionListener(e -> showAddReminderDialog(frame));

		JPanel reminderTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
		reminderTop.add(addReminderButton);

		JPanel reminderPanel = new JPanel(new BorderLayout());
		reminderPanel.add(reminderTop, BorderLayout.NORTH);
		reminderPanel.add(reminderScroll, BorderLayout.CENTER);

		// ---------- TAB 6: NOTAS ----------
		notesListModel = new DefaultListModel<>();
		notesJList = new JList<>(notesListModel);
		notesJList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		notesJList.setCellRenderer(new javax.swing.ListCellRenderer<Note>() {
			private final JLabel label = new JLabel();

			@Override
			public Component getListCellRendererComponent(JList<? extends Note> list, Note value, int index,
					boolean isSelected, boolean cellHasFocus) {
				label.setOpaque(true);
				label.setText("[" + value.getCategory() + "] " + value.getTitle());
				label.setForeground(
						value.getCategory() == Category.TASF ? new Color(21, 101, 192) : new Color(106, 27, 154));
				label.setBackground(isSelected ? new Color(220, 220, 220) : Color.WHITE);
				return label;
			}
		});
		notesJList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				loadSelectedNoteIntoEditor();
			}
		});
		JScrollPane notesListScroll = new JScrollPane(notesJList);
		notesListScroll.setPreferredSize(new Dimension(220, 0));

		String[] filterOptions = { "Todas", "TASF", "GNP" };
		noteFilterCombo = new JComboBox<>(filterOptions);
		noteFilterCombo.addActionListener(e -> refreshNotesList());

		JButton newNoteButton = new JButton("Nueva nota");
		newNoteButton.addActionListener(e -> showNewNoteDialog(frame));

		JPanel notesLeftTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
		notesLeftTop.add(new JLabel("Filtrar:"));
		notesLeftTop.add(noteFilterCombo);
		notesLeftTop.add(newNoteButton);

		JPanel notesLeftPanel = new JPanel(new BorderLayout());
		notesLeftPanel.add(notesLeftTop, BorderLayout.NORTH);
		notesLeftPanel.add(notesListScroll, BorderLayout.CENTER);

		noteTitleField = new JTextField();
		noteContentArea = new JTextArea();
		noteContentArea.setLineWrap(true);
		noteContentArea.setWrapStyleWord(true);
		JScrollPane noteContentScroll = new JScrollPane(noteContentArea);

		setupUndoRedo(noteTitleField);
		setupUndoRedo(noteContentArea);

		noteAutoSaveTimer = new Timer(600, e -> autoSaveNote());
		noteAutoSaveTimer.setRepeats(false);

		DocumentListener autoSaveListener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				scheduleAutoSave();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				scheduleAutoSave();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				scheduleAutoSave();
			}
		};
		noteTitleField.getDocument().addDocumentListener(autoSaveListener);
		noteContentArea.getDocument().addDocumentListener(autoSaveListener);

		JButton deleteNoteButton = new JButton("Eliminar nota");
		deleteNoteButton.addActionListener(e -> deleteSelectedNote());

		JLabel autoSaveHint = new JLabel("Los cambios se guardan automáticamente");
		autoSaveHint.setFont(autoSaveHint.getFont().deriveFont(Font.ITALIC, 11f));

		JPanel noteEditorButtons = new JPanel(new BorderLayout());
		noteEditorButtons.add(autoSaveHint, BorderLayout.WEST);
		JPanel deleteWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		deleteWrap.add(deleteNoteButton);
		noteEditorButtons.add(deleteWrap, BorderLayout.EAST);

		JPanel noteEditorTop = new JPanel(new BorderLayout(5, 5));
		noteEditorTop.add(new JLabel("Título:"), BorderLayout.WEST);
		noteEditorTop.add(noteTitleField, BorderLayout.CENTER);

		JPanel noteEditorPanel = new JPanel(new BorderLayout(5, 5));
		noteEditorPanel.add(noteEditorTop, BorderLayout.NORTH);
		noteEditorPanel.add(noteContentScroll, BorderLayout.CENTER);
		noteEditorPanel.add(noteEditorButtons, BorderLayout.SOUTH);

		JPanel notesPanel = new JPanel(new BorderLayout(5, 5));
		notesPanel.add(notesLeftPanel, BorderLayout.WEST);
		notesPanel.add(noteEditorPanel, BorderLayout.CENTER);

		// ---------- TABS PRINCIPALES ----------
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Lista", listPanel);
		tabs.addTab("Día", dayPanel);
		tabs.addTab("Semana", weekPanel);
		tabs.addTab("Mes", monthPanel);
		tabs.addTab("Recordatorios", reminderPanel);
		tabs.addTab("Notas", notesPanel);

		// ---------- TAB 7: PROYECTOS ----------
		JPanel projectsPanel = new JPanel(new GridLayout(1, 0, 10, 0));

		if (ProjectService.isCategoryAvailable(Category.TASF)) {
			projectsTasfContainer = new JPanel();
			projectsTasfContainer.setLayout(new BoxLayout(projectsTasfContainer, BoxLayout.Y_AXIS));
			JScrollPane tasfScroll = new JScrollPane(projectsTasfContainer);

			JButton newTasfButton = new JButton("Nuevo proyecto TASF");
			newTasfButton.addActionListener(e -> showNewProjectDialog(Category.TASF));

			JPanel tasfTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
			tasfTop.add(new JLabel("Proyectos TASF"));
			tasfTop.add(newTasfButton);

			JPanel tasfPanel = new JPanel(new BorderLayout());
			tasfPanel.add(tasfTop, BorderLayout.NORTH);
			tasfPanel.add(tasfScroll, BorderLayout.CENTER);

			projectsPanel.add(tasfPanel);
		}

		if (ProjectService.isCategoryAvailable(Category.GNP)) {
			projectsGnpContainer = new JPanel();
			projectsGnpContainer.setLayout(new BoxLayout(projectsGnpContainer, BoxLayout.Y_AXIS));
			JScrollPane gnpScroll = new JScrollPane(projectsGnpContainer);

			JButton newGnpButton = new JButton("Nuevo proyecto GNP");
			newGnpButton.addActionListener(e -> showNewProjectDialog(Category.GNP));

			JPanel gnpTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
			gnpTop.add(new JLabel("Proyectos GNP"));
			gnpTop.add(newGnpButton);

			JPanel gnpPanel = new JPanel(new BorderLayout());
			gnpPanel.add(gnpTop, BorderLayout.NORTH);
			gnpPanel.add(gnpScroll, BorderLayout.CENTER);

			projectsPanel.add(gnpPanel);
		}

		tabs.addTab("Proyectos", projectsPanel);
		// ---------- BOTÓN PARA AGREGAR TAREA ----------
		JButton addButton = new JButton("Agregar tarea");
		addButton.addActionListener(e -> showAddTaskDialog(frame));

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(addButton);

		frame.setLayout(new BorderLayout());
		frame.add(topPanel, BorderLayout.NORTH);
		frame.add(tabs, BorderLayout.CENTER);

		// Apagar el scheduler al cerrar
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (scheduler != null && !scheduler.isShutdown()) {
					scheduler.shutdownNow();
				}
				if (emailScheduler != null && !emailScheduler.isShutdown()) {
					emailScheduler.shutdownNow();
				}
			}
		});

		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		refreshAllViews();
		refreshNotesList();
		refreshProjectsView();
		startReminderScheduler(frame);
//		startEmailWatcher();
	}

	// --------- CREAR TAREA (título + prioridad + descripción + fecha + hora)
	// ---------

	private static void showAddTaskDialog(JFrame frame) {
		// --- Panel de título, prioridad y descripción ---

		JTextField titleField = new JTextField(20);

		String[] priorityOptions = { "Alta", "Media", "Baja" };
		JComboBox<String> priorityCombo = new JComboBox<>(priorityOptions);
		// Preseleccionado: Media
		priorityCombo.setSelectedItem("Media");

		String[] categoryOptions = { "TASF", "GNP" };
		JComboBox<String> categoryCombo = new JComboBox<>(categoryOptions);

		JTextArea descriptionArea = new JTextArea(4, 20);
		setupUndoRedo(descriptionArea);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		JScrollPane descScroll = new JScrollPane(descriptionArea);

		JPanel formPanel = new JPanel();
		formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

		JPanel titlePanel = new JPanel(new BorderLayout(5, 5));
		titlePanel.add(new JLabel("Título de la tarea:"), BorderLayout.WEST);
		titlePanel.add(titleField, BorderLayout.CENTER);

		JPanel priorityPanel = new JPanel(new BorderLayout(5, 5));
		priorityPanel.add(new JLabel("Prioridad:"), BorderLayout.WEST);
		priorityPanel.add(priorityCombo, BorderLayout.CENTER);

		JPanel categoryPanel = new JPanel(new BorderLayout(5, 5));
		categoryPanel.add(new JLabel("Categoría:"), BorderLayout.WEST);
		categoryPanel.add(categoryCombo, BorderLayout.CENTER);

		JPanel descPanel = new JPanel(new BorderLayout(5, 5));
		descPanel.add(new JLabel("Descripción (opcional):"), BorderLayout.NORTH);
		descPanel.add(descScroll, BorderLayout.CENTER);

		formPanel.add(titlePanel);
		formPanel.add(Box.createVerticalStrut(5));
		formPanel.add(priorityPanel);
		formPanel.add(Box.createVerticalStrut(5));
		formPanel.add(categoryPanel);
		formPanel.add(Box.createVerticalStrut(5));
		formPanel.add(descPanel);

		JOptionPane titleOptionPane = new JOptionPane(formPanel, JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);

		JDialog titleDialog = titleOptionPane.createDialog(frame, "Nueva tarea - Detalles");

		titleDialog.addWindowFocusListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowGainedFocus(java.awt.event.WindowEvent e) {
				titleField.requestFocusInWindow();
			}
		});

		titleDialog.setVisible(true);

		Object titleValue = titleOptionPane.getValue();
		if (titleValue == null || !(titleValue instanceof Integer)) {
			return; // Cerrado sin OK
		}

		int titleResult = (Integer) titleValue;
		if (titleResult != JOptionPane.OK_OPTION) {
			return; // Cancelado
		}

		String title = titleField.getText().trim();
		if (title.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "El título no puede estar vacío.");
			return;
		}

		String priorityStr = (String) priorityCombo.getSelectedItem();
		Task.Priority priority = Task.Priority.MEDIUM; // default
		if ("Alta".equals(priorityStr)) {
			priority = Task.Priority.HIGH;
		} else if ("Baja".equals(priorityStr)) {
			priority = Task.Priority.LOW;
		}

		String description = descriptionArea.getText();
		if (description == null) {
			description = "";
		}

		String categoryStr = (String) categoryCombo.getSelectedItem();
		Category category = "GNP".equals(categoryStr) ? Category.GNP : Category.TASF;

		// 2) Fecha con calendario
		LocalDate initialDate = LocalDate.now();
		LocalDate selectedDate = DatePickerDialog.showDatePicker(frame, initialDate);
		if (selectedDate == null) {
			return; // canceló
		}

		LocalDateTime reminderTime;

		if (selectedDate.equals(LocalDate.now())) {
			// Si es para hoy, se notifica de inmediato: no hace falta pedir la hora.
			reminderTime = LocalDateTime.now().withSecond(0).withNano(0);
		} else {
			// Hora con spinner HH:mm
			Date now = new Date();
			JSpinner timeSpinner = new JSpinner(new SpinnerDateModel(now, null, null, Calendar.MINUTE));
			JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "hh:mm a");
			timeSpinner.setEditor(timeEditor);

			JFormattedTextField tf = timeEditor.getTextField();
			tf.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);

			tf.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					SwingUtilities.invokeLater(() -> {
						int pos = tf.viewToModel(e.getPoint());
						if (pos >= 0) {
							tf.setCaretPosition(pos);
						}
					});
				}
			});

			JPanel timePanel = new JPanel(new GridLayout(1, 2, 5, 5));
			timePanel.add(new JLabel("Hora (HH:mm):"));
			timePanel.add(timeSpinner);

			int timeResult = JOptionPane.showConfirmDialog(frame, timePanel, "Nueva tarea - Hora",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

			if (timeResult != JOptionPane.OK_OPTION) {
				return;
			}

			Date timeValue = (Date) timeSpinner.getValue();
			LocalTime localTime = timeValue.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();

			reminderTime = LocalDateTime.of(selectedDate, localTime);
		}

		Task task = taskService.addTask(title, reminderTime, priority, description, category);

		JOptionPane.showMessageDialog(frame, "Tarea creada:\n" + task.toDisplayString(), "Tarea agregada",
				JOptionPane.INFORMATION_MESSAGE);

		refreshAllViews();
	}

	// --------- CREAR RECORDATORIO (título + prioridad + categoría + fecha + hora)

	private static void showAddReminderDialog(JFrame frame) {
		JTextField titleField = new JTextField(20);

		String[] priorityOptions = { "Alta", "Media", "Baja" };
		JComboBox<String> priorityCombo = new JComboBox<>(priorityOptions);
		priorityCombo.setSelectedItem("Media");

		String[] categoryOptions = { "TASF", "GNP" };
		JComboBox<String> categoryCombo = new JComboBox<>(categoryOptions);

		JPanel formPanel = new JPanel();
		formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

		JPanel titlePanel = new JPanel(new BorderLayout(5, 5));
		titlePanel.add(new JLabel("Título del recordatorio:"), BorderLayout.WEST);
		titlePanel.add(titleField, BorderLayout.CENTER);

		JPanel priorityPanel = new JPanel(new BorderLayout(5, 5));
		priorityPanel.add(new JLabel("Prioridad:"), BorderLayout.WEST);
		priorityPanel.add(priorityCombo, BorderLayout.CENTER);

		JPanel categoryPanel = new JPanel(new BorderLayout(5, 5));
		categoryPanel.add(new JLabel("Categoría:"), BorderLayout.WEST);
		categoryPanel.add(categoryCombo, BorderLayout.CENTER);

		formPanel.add(titlePanel);
		formPanel.add(Box.createVerticalStrut(5));
		formPanel.add(priorityPanel);
		formPanel.add(Box.createVerticalStrut(5));
		formPanel.add(categoryPanel);

		JOptionPane titleOptionPane = new JOptionPane(formPanel, JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);

		JDialog titleDialog = titleOptionPane.createDialog(frame, "Nuevo recordatorio - Detalles");

		titleDialog.addWindowFocusListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowGainedFocus(java.awt.event.WindowEvent e) {
				titleField.requestFocusInWindow();
			}
		});

		titleDialog.setVisible(true);

		Object titleValue = titleOptionPane.getValue();
		if (titleValue == null || !(titleValue instanceof Integer)) {
			return;
		}

		int titleResult = (Integer) titleValue;
		if (titleResult != JOptionPane.OK_OPTION) {
			return;
		}

		String title = titleField.getText().trim();
		if (title.isEmpty()) {
			JOptionPane.showMessageDialog(frame, "El título no puede estar vacío.");
			return;
		}

		String priorityStr = (String) priorityCombo.getSelectedItem();
		Task.Priority priority = Task.Priority.MEDIUM;
		if ("Alta".equals(priorityStr)) {
			priority = Task.Priority.HIGH;
		} else if ("Baja".equals(priorityStr)) {
			priority = Task.Priority.LOW;
		}

		String categoryStr = (String) categoryCombo.getSelectedItem();
		Category category = "GNP".equals(categoryStr) ? Category.GNP : Category.TASF;

		// Fecha con calendario
		LocalDate initialDate = LocalDate.now();
		LocalDate selectedDate = DatePickerDialog.showDatePicker(frame, initialDate);
		if (selectedDate == null) {
			return;
		}

		LocalDateTime reminderTime;

		if (selectedDate.equals(LocalDate.now())) {
			// Si es para hoy, se notifica de inmediato: no hace falta pedir la hora.
			reminderTime = LocalDateTime.now().withSecond(0).withNano(0);
		} else {
			// Hora con spinner HH:mm
			Date now = new Date();
			JSpinner timeSpinner = new JSpinner(new SpinnerDateModel(now, null, null, Calendar.MINUTE));
			JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "hh:mm a");
			timeSpinner.setEditor(timeEditor);

			JFormattedTextField tf = timeEditor.getTextField();
			tf.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);

			tf.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					SwingUtilities.invokeLater(() -> {
						int pos = tf.viewToModel(e.getPoint());
						if (pos >= 0) {
							tf.setCaretPosition(pos);
						}
					});
				}
			});

			JPanel timePanel = new JPanel(new GridLayout(1, 2, 5, 5));
			timePanel.add(new JLabel("Hora (HH:mm):"));
			timePanel.add(timeSpinner);

			int timeResult = JOptionPane.showConfirmDialog(frame, timePanel, "Nueva tarea - Hora",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

			if (timeResult != JOptionPane.OK_OPTION) {
				return;
			}

			Date timeValue = (Date) timeSpinner.getValue();
			LocalTime localTime = timeValue.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();

			reminderTime = LocalDateTime.of(selectedDate, localTime);
		}

		Reminder reminder = reminderService.addReminder(title, reminderTime, priority, category);

		JOptionPane.showMessageDialog(frame, "Recordatorio creado:\n" + reminder.toDisplayString(),
				"Recordatorio agregado", JOptionPane.INFORMATION_MESSAGE);

		refreshAllViews();
	}

	// --------- FILA DE TAREA (Lista y Día) ---------

	private static JPanel createTaskRow(Task t) {
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
				BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		row.setBackground(t.isCompleted() ? new Color(230, 230, 230) : getColorForPriority(t.getPriority()));
		row.setOpaque(true);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);

		String plainText = t.toDisplayString().replaceFirst("^\\[" + t.getCategory() + "\\]\\s*", "");

		StringBuilder html = new StringBuilder("<html>");
		if (t.isCompleted()) {
			html.append("<strike>");
		}
		html.append(categoryTagHtml(t.getCategory())).append(" ").append(plainText);
		if (t.getProgressPercent() > 0) {
			html.append(" - Avance: ").append(t.getProgressPercent()).append("%");
		}
		if (t.isCompleted()) {
			html.append("</strike>");
		}
		if (t.getDescription() != null && !t.getDescription().isEmpty()) {
			html.append("<br/><i>").append(t.getDescription()).append("</i>");
		}
		if (t.getNotes() != null && !t.getNotes().isEmpty()) {
			html.append("<br/><span style='color:gray;'>Nota: ").append(t.getNotes()).append("</span>");
		}
		html.append("</html>");

		JLabel textLabel = new JLabel(html.toString());
		textLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		textLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				showEditTaskDialog(t);
			}
		});

		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		buttonsPanel.setOpaque(false);

		if (!t.isCompleted()) {
			JButton completeButton = new JButton("✓ Completar");
			completeButton.addActionListener(e -> {
				t.setCompleted(true);
				taskService.save();
				refreshAllViews();
			});
			buttonsPanel.add(completeButton);
		}

		JButton deleteButton = new JButton("✕ Eliminar");
		deleteButton.addActionListener(e -> {
			int confirm = JOptionPane.showConfirmDialog(mainFrame, "¿Eliminar la tarea \"" + t.getTitle() + "\"?",
					"Confirmar eliminación", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				taskService.deleteTask(t.getId());
				refreshAllViews();
			}
		});
		buttonsPanel.add(deleteButton);

		row.add(textLabel, BorderLayout.CENTER);
		row.add(buttonsPanel, BorderLayout.EAST);

		return row;
	}

	private static void showEditTaskDialog(Task t) {
		JSlider progressSlider = new JSlider(0, 100, t.getProgressPercent());
		progressSlider.setMajorTickSpacing(25);
		progressSlider.setPaintTicks(true);
		progressSlider.setPaintLabels(true);

		JTextArea notesArea = new JTextArea(t.getNotes(), 4, 20);
		setupUndoRedo(notesArea);
		notesArea.setLineWrap(true);
		notesArea.setWrapStyleWord(true);
		JScrollPane notesScroll = new JScrollPane(notesArea);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(new JLabel("Avance (%):"));
		panel.add(progressSlider);
		panel.add(Box.createVerticalStrut(8));
		panel.add(new JLabel("Notas / dudas:"));
		panel.add(notesScroll);

		int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Editar: " + t.getTitle(),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			t.setProgressPercent(progressSlider.getValue());
			t.setNotes(notesArea.getText());
			taskService.save();
			refreshAllViews();
		}
	}

	// --------- REFRESCAR VISTAS ---------

	private static void refreshAllViews() {
		refreshListView();
		refreshDayViewFromField();
		refreshWeekViewFromField();
		refreshMonthViewFromFields();
		refreshReminderListView();
	}

	private static void refreshReminderListView() {
		StringBuilder sb = new StringBuilder();
		for (Reminder r : reminderService.getAllReminders()) {
			sb.append(r.toDisplayString()).append("\n");
		}
		reminderListArea.setText(sb.toString());
	}

	private static void refreshListView() {
		listContainer.removeAll();
		boolean found = false;
		for (Task t : taskService.getAllTasks()) {
			listContainer.add(createTaskRow(t));
			found = true;
		}
		if (!found) {
			listContainer.add(new JLabel("No hay tareas."));
		}
		listContainer.revalidate();
		listContainer.repaint();
	}

	private static void refreshDayViewFromField() {
		String text = dayDateField.getText().trim();
		dayContainer.removeAll();

		if (text.isEmpty()) {
			dayContainer.revalidate();
			dayContainer.repaint();
			return;
		}

		LocalDate date;
		try {
			date = LocalDate.parse(text, DATE_FORMAT);
		} catch (DateTimeParseException ex) {
			dayContainer.add(new JLabel("Fecha inválida. Usa formato dd/MM/yyyy"));
			dayContainer.revalidate();
			dayContainer.repaint();
			return;
		}

		boolean found = false;
		for (Task t : taskService.getAllTasks()) {
			if (t.getReminderTime().toLocalDate().equals(date)) {
				dayContainer.add(createTaskRow(t));
				found = true;
			}
		}
		if (!found) {
			dayContainer.add(new JLabel("No hay tareas para este día."));
		}

		dayContainer.revalidate();
		dayContainer.repaint();
	}

	// ---- Semana ----
	// ---- Semana ----

	private static void refreshWeekViewFromField() {
		String text = weekDateField.getText().trim();
		if (text.isEmpty()) {
			weekDaysPanel.removeAll();
			weekDaysPanel.revalidate();
			weekDaysPanel.repaint();
			return;
		}

		LocalDate anyDay;
		try {
			anyDay = LocalDate.parse(text, DATE_FORMAT);
		} catch (DateTimeParseException ex) {
			weekDaysPanel.removeAll();
			weekDaysPanel.add(new JLabel("Fecha inválida. Usa formato dd/MM/yyyy.", SwingConstants.CENTER));
			weekDaysPanel.revalidate();
			weekDaysPanel.repaint();
			return;
		}

		DayOfWeek dow = anyDay.getDayOfWeek();
		int shift = dow.getValue() - 1; // cuántos días restar para llegar a lunes
		LocalDate monday = anyDay.minusDays(shift);

		refreshWeekGrid(monday);
	}

	private static void refreshWeekGrid(LocalDate weekStart) {
		weekDaysPanel.removeAll();

		for (int i = 0; i < 7; i++) {
			LocalDate currentDate = weekStart.plusDays(i);

			StringBuilder html = new StringBuilder("<html>");
			html.append(currentDate.format(DATE_FORMAT)).append("<br/>");

			Task.Priority highestPriority = null;
			int count = 0;

			for (Task t : taskService.getAllTasks()) {
				if (!t.isCompleted() && t.getReminderTime().toLocalDate().equals(currentDate)) {
					html.append("• ").append(categoryTagHtml(t.getCategory())).append(" ").append(t.getTitle())
							.append("<br/>");
					highestPriority = maxPriority(highestPriority, t.getPriority());
					count++;
					if (count >= 3) {
						html.append("...<br/>");
						break;
					}
				}
			}

			html.append("</html>");

			JButton dayButton = new JButton(html.toString());
			dayButton.setHorizontalAlignment(SwingConstants.LEFT);
			dayButton.setVerticalAlignment(SwingConstants.TOP);
			dayButton.addActionListener(e -> showTasksDialogForDate(currentDate));

			dayButton.setBackground(getColorForPriority(highestPriority));
			dayButton.setOpaque(true);
			dayButton.setBorderPainted(true);

			weekDaysPanel.add(dayButton);
		}

		weekDaysPanel.revalidate();
		weekDaysPanel.repaint();
	}

	// ---- Mes ----

	private static void refreshMonthViewFromFields() {
		String monthText = monthField.getText().trim();
		String yearText = yearField.getText().trim();

		int month, year;
		try {
			month = Integer.parseInt(monthText);
			year = Integer.parseInt(yearText);
			if (month < 1 || month > 12)
				throw new NumberFormatException();
		} catch (NumberFormatException ex) {
			monthDaysPanel.removeAll();
			monthDaysPanel.add(new JLabel("Mes/Año inválidos. Mes 1-12, año numérico.", SwingConstants.CENTER));
			monthDaysPanel.revalidate();
			monthDaysPanel.repaint();
			return;
		}

		YearMonth ym = YearMonth.of(year, month);
		refreshMonthGrid(ym);
	}

	private static void refreshMonthGrid(YearMonth ym) {
		monthDaysPanel.removeAll();

		LocalDate firstDay = ym.atDay(1);
		int firstDow = firstDay.getDayOfWeek().getValue();
		int daysInMonth = ym.lengthOfMonth();

		for (int i = 1; i < firstDow; i++) {
			monthDaysPanel.add(new JLabel(""));
		}

		for (int day = 1; day <= daysInMonth; day++) {
			LocalDate currentDate = ym.atDay(day);

			StringBuilder html = new StringBuilder("<html>");
			html.append(currentDate.format(DATE_FORMAT)).append("<br/>");

			Task.Priority highestPriority = null;
			int count = 0;

			for (Task t : taskService.getAllTasks()) {
				if (!t.isCompleted() && t.getReminderTime().toLocalDate().equals(currentDate)) {
					html.append("• ").append(categoryTagHtml(t.getCategory())).append(" ").append(t.getTitle());
					if (t.getProgressPercent() > 0) {
						html.append(" (").append(t.getProgressPercent()).append("%)");
					}
					html.append("<br/>");
					highestPriority = maxPriority(highestPriority, t.getPriority());
					count++;
					if (count >= 3) {
						html.append("...<br/>");
						break;
					}
				}
			}

			html.append("</html>");

			JButton dayButton = new JButton(html.toString());
			dayButton.setHorizontalAlignment(SwingConstants.LEFT);
			dayButton.setVerticalAlignment(SwingConstants.TOP);
			dayButton.addActionListener(e -> showTasksDialogForDate(currentDate));

			dayButton.setBackground(getColorForPriority(highestPriority));
			dayButton.setOpaque(true);
			dayButton.setBorderPainted(true);

			monthDaysPanel.add(dayButton);
		}

		monthDaysPanel.revalidate();
		monthDaysPanel.repaint();
	}

	private static void showTasksDialogForDate(LocalDate date) {
		StringBuilder sb = new StringBuilder();
		sb.append("Tareas para el ").append(date.format(DATE_FORMAT)).append(":\n\n");

		boolean found = false;
		for (Task t : taskService.getAllTasks()) {
			if (!t.isCompleted() && t.getReminderTime().toLocalDate().equals(date)) {
				sb.append("• ").append(t.toDisplayString()).append("\n");
				if (t.getDescription() != null && !t.getDescription().isEmpty()) {
					sb.append("   Descripción: ").append(t.getDescription()).append("\n");
				}
				sb.append("\n");
				found = true;
			}
		}

		if (!found) {
			sb.append("No hay tareas para este día.");
		}

		JOptionPane.showMessageDialog(mainFrame, sb.toString(), "Tareas del día", JOptionPane.INFORMATION_MESSAGE);
	}

	// --------- PRIORIDAD → COLOR ---------

	private static Task.Priority maxPriority(Task.Priority a, Task.Priority b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		// Orden: HIGH > MEDIUM > LOW
		if (a == Task.Priority.HIGH || b == Task.Priority.HIGH)
			return Task.Priority.HIGH;
		if (a == Task.Priority.MEDIUM || b == Task.Priority.MEDIUM)
			return Task.Priority.MEDIUM;
		return Task.Priority.LOW;
	}

	private static String categoryTagHtml(Category category) {
		if (category == null) {
			return "";
		}
		String color = (category == Category.TASF) ? "#1565C0" : "#6A1B9A"; // azul TASF, morado GNP
		return "<font color='" + color + "'><b>[" + category + "]</b></font>";
	}

	private static Color getColorForPriority(Task.Priority priority) {
		if (priority == null) {
			return Color.WHITE;
		}
		switch (priority) {
		case HIGH:
			return new Color(255, 204, 204); // rojo muy suave
		case MEDIUM:
			return new Color(255, 249, 196); // amarillo suave
		case LOW:
		default:
			return new Color(204, 255, 204); // verde suave
		}
	}

	// --------- SCHEDULER Y RECORDATORIOS ---------

	private static void startReminderScheduler(JFrame frame) {
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> checkReminders(frame), 5, 10, TimeUnit.SECONDS);
	}

	private static void startEmailWatcher() {
		java.util.Properties props = new java.util.Properties();
		try (java.io.InputStream in = new java.io.FileInputStream("config.properties")) {
			props.load(in);
		} catch (java.io.IOException e) {
			throw new RuntimeException(
					"No se pudo cargar config.properties. Copia config.properties.example y complétalo.", e);
		}

		EmailWatcher tasfWatcher = buildEmailWatcher(props, "email.tasf.", "TASF");
		EmailWatcher gnpWatcher = buildEmailWatcher(props, "email.gnp.", "GNP");

		// Un solo scheduler con 2 hilos: cada cuenta corre en su propio hilo, sin
		// bloquearse entre sí
		emailScheduler = Executors.newScheduledThreadPool(2);
		emailScheduler.scheduleAtFixedRate(tasfWatcher::checkNewEmails, 10, 60, TimeUnit.SECONDS);
		emailScheduler.scheduleAtFixedRate(gnpWatcher::checkNewEmails, 15, 60, TimeUnit.SECONDS);
	}

	private static EmailWatcher buildEmailWatcher(java.util.Properties props, String prefix, String label) {
		String host = props.getProperty(prefix + "host");
		int port = Integer.parseInt(props.getProperty(prefix + "port"));
		String user = props.getProperty(prefix + "user");
		String pass = props.getProperty(prefix + "password");
		boolean useSSL = Boolean.parseBoolean(props.getProperty(prefix + "ssl"));
		return new EmailWatcher(host, port, user, pass, useSSL, label);
	}

	/**
	 * Determina si una tarea/recordatorio debe notificarse ya: - Su fecha es hoy o
	 * ya pasó, o - Su fecha es mañana y ya son las 23:00 o más tarde (aviso
	 * nocturno / creación tardía).
	 */
	private static boolean shouldNotifyNow(LocalDateTime itemDateTime) {
		LocalDate itemDate = itemDateTime.toLocalDate();
		LocalDate today = LocalDate.now();

		if (!itemDate.isAfter(today)) {
			return true; // hoy o ya vencida
		}
		if (itemDate.isEqual(today.plusDays(1)) && LocalTime.now().isAfter(LocalTime.of(23, 0))) {
			return true; // mañana, ya pasaron las 11pm de hoy
		}
		return false;
	}

	private static void checkReminders(JFrame frame) {
		List<Task> dueTasks = new ArrayList<>();
		for (Task t : taskService.getAllTasks()) {
			if (!t.isCompleted() && !t.isReminded() && shouldNotifyNow(t.getReminderTime())) {
				dueTasks.add(t);
			}
		}

		List<Reminder> dueReminders = new ArrayList<>();
		for (Reminder r : reminderService.getAllReminders()) {
			if (!r.isCompleted() && !r.isReminded() && shouldNotifyNow(r.getReminderTime())) {
				dueReminders.add(r);
			}
		}

		if (dueTasks.isEmpty() && dueReminders.isEmpty()) {
			return;
		}

		StringBuilder mensaje = new StringBuilder("📋 Recordatorio de tareas/pendientes:\n\n");
		for (Task t : dueTasks) {
			mensaje.append("• ").append(t.toDisplayString()).append("\n");
			t.setReminded(true);
		}
		for (Reminder r : dueReminders) {
			mensaje.append("• ").append(r.toDisplayString()).append("\n");
			r.setReminded(true);
		}
		WhatsAppClient.sendTextMessage(mensaje.toString());

		if (!dueTasks.isEmpty()) {
			taskService.save();
		}
		if (!dueReminders.isEmpty()) {
			reminderService.save();
		}

		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(frame, mensaje.toString(), "Recordatorio", JOptionPane.INFORMATION_MESSAGE);
			refreshAllViews();
		});
	}

	// --------- NOTAS: CRUD ---------

	private static void showNewNoteDialog(JFrame frame) {
		String[] categoryOptions = { "TASF", "GNP" };
		JComboBox<String> categoryCombo = new JComboBox<>(categoryOptions);

		int result = JOptionPane.showConfirmDialog(frame, categoryCombo, "Nueva nota - Selecciona categoría",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (result != JOptionPane.OK_OPTION) {
			return;
		}

		String categoryStr = (String) categoryCombo.getSelectedItem();
		Category category = "GNP".equals(categoryStr) ? Category.GNP : Category.TASF;

		Note note = noteService.addNote("Nueva nota", "", category);
		refreshNotesList();
		notesJList.setSelectedValue(note, true);
	}

	private static void loadSelectedNoteIntoEditor() {
		loadingNote = true;
		Note note = notesJList.getSelectedValue();
		selectedNote = note;
		if (note == null) {
			noteTitleField.setText("");
			noteContentArea.setText("");
			noteTitleField.setEnabled(false);
			noteContentArea.setEnabled(false);
		} else {
			noteTitleField.setText(note.getTitle());
			noteContentArea.setText(note.getContent());
			noteTitleField.setEnabled(true);
			noteContentArea.setEnabled(true);
		}
		loadingNote = false;
	}

	private static void setupUndoRedo(javax.swing.text.JTextComponent component) {
		UndoManager undoManager = new UndoManager();
		component.getDocument().addUndoableEditListener(undoManager);

		component.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
		component.getActionMap().put("Undo", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				try {
					if (undoManager.canUndo()) {
						undoManager.undo();
					}
				} catch (CannotUndoException ex) {
					// no hay nada que deshacer, se ignora
				}
			}
		});

		component.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
		component.getActionMap().put("Redo", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				try {
					if (undoManager.canRedo()) {
						undoManager.redo();
					}
				} catch (CannotRedoException ex) {
					// no hay nada que rehacer, se ignora
				}
			}
		});
	}

	private static void scheduleAutoSave() {
		if (loadingNote || selectedNote == null) {
			return; // evita guardar mientras se carga una nota programáticamente o si no hay nota
					// activa
		}
		noteAutoSaveTimer.restart();
	}

	private static void autoSaveNote() {
		if (selectedNote == null) {
			return;
		}
		String title = noteTitleField.getText().trim();
		if (title.isEmpty()) {
			return; // no autoguarda con título vacío, evita notas "fantasma"
		}
		noteService.updateNote(selectedNote.getId(), title, noteContentArea.getText());
		// Refrescamos solo el modelo de la lista (para reflejar el nuevo título), sin
		// recargar el editor
		int index = notesListModel.indexOf(selectedNote);
		if (index >= 0) {
			notesListModel.set(index, selectedNote);
		}
	}

	private static void deleteSelectedNote() {
		if (selectedNote == null) {
			JOptionPane.showMessageDialog(mainFrame, "Selecciona una nota primero.");
			return;
		}
		int confirm = JOptionPane.showConfirmDialog(mainFrame, "¿Eliminar la nota \"" + selectedNote.getTitle() + "\"?",
				"Confirmar eliminación", JOptionPane.YES_NO_OPTION);
		if (confirm == JOptionPane.YES_OPTION) {
			noteService.deleteNote(selectedNote.getId());
			selectedNote = null;
			refreshNotesList();
		}
	}

	private static void refreshNotesList() {
		String filter = (String) noteFilterCombo.getSelectedItem();
		List<Note> notes;
		if ("TASF".equals(filter)) {
			notes = noteService.getNotesByCategory(Category.TASF);
		} else if ("GNP".equals(filter)) {
			notes = noteService.getNotesByCategory(Category.GNP);
		} else {
			notes = noteService.getAllNotes();
		}

		notesListModel.clear();
		for (Note n : notes) {
			notesListModel.addElement(n);
		}
		loadSelectedNoteIntoEditor();
	}

	// --------- PROYECTOS: CRUD ---------

	private static JPanel createProjectRow(Project p) {
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
				BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

		JPanel topRow = new JPanel(new BorderLayout());
		JLabel nameLabel = new JLabel(p.getName());
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

		JButton editButton = new JButton("Editar");
		editButton.addActionListener(e -> showRenameProjectDialog(p));

		JButton deleteButton = new JButton("Eliminar");
		deleteButton.addActionListener(e -> {
			int confirm = JOptionPane.showConfirmDialog(mainFrame,
					"¿Quitar el proyecto \"" + p.getName() + "\" de la app?\n(La carpeta en disco NO se eliminará)",
					"Confirmar eliminación", JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				projectService.deleteProject(p.getId());
				refreshProjectsView();
			}
		});

		JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		topButtons.add(editButton);
		topButtons.add(deleteButton);

		topRow.add(nameLabel, BorderLayout.WEST);
		topRow.add(topButtons, BorderLayout.EAST);

		JPanel folderButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		folderButtons.add(createOpenFolderButton(p, "Documentación", "documentacion"));
		folderButtons.add(createOpenFolderButton(p, "Ambientación", "ambientacion"));
		folderButtons.add(createOpenFolderButton(p, "Código", "codigo"));

		row.add(topRow);
		row.add(folderButtons);

		return row;
	}

	private static JButton createOpenFolderButton(Project p, String label, String subfolder) {
		JButton button = new JButton(label);
		button.addActionListener(e -> {
			String path = ProjectService.getSubfolderPath(p, subfolder);
			File folder = new File(path);
			if (!folder.exists()) {
				JOptionPane.showMessageDialog(mainFrame, "No se encontró la carpeta:\n" + path);
				return;
			}
			try {
				Desktop.getDesktop().open(folder);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(mainFrame, "No se pudo abrir la carpeta:\n" + ex.getMessage());
			}
		});
		return button;
	}

	private static void showNewProjectDialog(Category category) {
		String name = JOptionPane.showInputDialog(mainFrame, "Nombre del nuevo proyecto (" + category + "):");
		if (name == null || name.trim().isEmpty()) {
			return;
		}
		try {
			projectService.addProject(name.trim(), category);
			refreshProjectsView();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(mainFrame, "No se pudo crear el proyecto:\n" + ex.getMessage());
		}
	}

	private static void showRenameProjectDialog(Project p) {
		String newName = JOptionPane.showInputDialog(mainFrame, "Nuevo nombre del proyecto:", p.getName());
		if (newName == null || newName.trim().isEmpty() || newName.trim().equals(p.getName())) {
			return;
		}
		try {
			projectService.renameProject(p.getId(), newName.trim());
			refreshProjectsView();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(mainFrame, "No se pudo renombrar el proyecto:\n" + ex.getMessage());
		}
	}

	private static void refreshProjectsView() {
		if (projectsTasfContainer != null) {
			projectsTasfContainer.removeAll();
			List<Project> tasfProjects = projectService.getProjectsByCategory(Category.TASF);
			if (tasfProjects.isEmpty()) {
				projectsTasfContainer.add(new JLabel("No hay proyectos TASF."));
			} else {
				for (Project p : tasfProjects) {
					projectsTasfContainer.add(createProjectRow(p));
				}
			}
			projectsTasfContainer.revalidate();
			projectsTasfContainer.repaint();
		}
		if (projectsGnpContainer != null) {
			projectsGnpContainer.removeAll();
			List<Project> gnpProjects = projectService.getProjectsByCategory(Category.GNP);
			if (gnpProjects.isEmpty()) {
				projectsGnpContainer.add(new JLabel("No hay proyectos GNP."));
			} else {
				for (Project p : gnpProjects) {
					projectsGnpContainer.add(createProjectRow(p));
				}
			}
			projectsGnpContainer.revalidate();
			projectsGnpContainer.repaint();
		}
	}
}
