package app;

import model.Task;
import service.TaskService;
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
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

	private static TaskService taskService = new TaskService();

	// Referencia al frame principal para diálogos
	private static JFrame mainFrame;

	// Áreas de texto para vistas
	private static JTextArea listArea;
	private static JTextArea dayArea;

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
		mainFrame = frame;
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(950, 650);

		LocalDate today = LocalDate.now();

		// ---------- TAB 1: VISTA LISTA ----------
		listArea = new JTextArea();
		listArea.setEditable(false);
		JScrollPane listScroll = new JScrollPane(listArea);
		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.add(listScroll, BorderLayout.CENTER);

		// ---------- TAB 2: VISTA DÍA ----------
		dayArea = new JTextArea();
		dayArea.setEditable(false);
		JScrollPane dayScroll = new JScrollPane(dayArea);

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

		// ---------- TABS PRINCIPALES ----------
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Lista", listPanel);
		tabs.addTab("Día", dayPanel);
		tabs.addTab("Semana", weekPanel);
		tabs.addTab("Mes", monthPanel);

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
		startReminderScheduler(frame);
		startEmailWatcher();
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

		JTextArea descriptionArea = new JTextArea(4, 20);
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

		JPanel descPanel = new JPanel(new BorderLayout(5, 5));
		descPanel.add(new JLabel("Descripción (opcional):"), BorderLayout.NORTH);
		descPanel.add(descScroll, BorderLayout.CENTER);

		formPanel.add(titlePanel);
		formPanel.add(Box.createVerticalStrut(5));
		formPanel.add(priorityPanel);
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

		// 2) Fecha con calendario
		LocalDate initialDate = LocalDate.now();
		LocalDate selectedDate = DatePickerDialog.showDatePicker(frame, initialDate);
		if (selectedDate == null) {
			return; // canceló
		}

		// 3) Hora con spinner HH:mm
		Date now = new Date();
		JSpinner timeSpinner = new JSpinner(new SpinnerDateModel(now, null, null, Calendar.MINUTE));
		JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
		timeSpinner.setEditor(timeEditor);

		// --- FIX PARA QUE EL CURSOR SE COLOQUE DONDE CLICKEES ---
		JFormattedTextField tf = timeEditor.getTextField();
		tf.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);

		// Desactivar que seleccione todo automáticamente
		tf.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				SwingUtilities.invokeLater(() -> {
					int pos = tf.viewToModel(e.getPoint()); // Java 8
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

		LocalDateTime reminderTime = LocalDateTime.of(selectedDate, localTime);

		Task task = taskService.addTask(title, reminderTime, priority, description);

		JOptionPane.showMessageDialog(frame, "Tarea creada:\n" + task.toDisplayString(), "Tarea agregada",
				JOptionPane.INFORMATION_MESSAGE);

		refreshAllViews();
	}

	// --------- REFRESCAR VISTAS ---------

	private static void refreshAllViews() {
		refreshListView();
		refreshDayViewFromField();
		refreshWeekViewFromField();
		refreshMonthViewFromFields();
	}

	private static void refreshListView() {
		StringBuilder sb = new StringBuilder();
		for (Task t : taskService.getAllTasks()) {
			sb.append(t.toDisplayString()).append("\n");
		}
		listArea.setText(sb.toString());
	}

	private static void refreshDayViewFromField() {
		String text = dayDateField.getText().trim();
		if (text.isEmpty()) {
			dayArea.setText("");
			return;
		}

		LocalDate date;
		try {
			date = LocalDate.parse(text, DATE_FORMAT);
		} catch (DateTimeParseException ex) {
			dayArea.setText("Fecha inválida. Usa formato dd/MM/yyyy");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Tareas para el ").append(date.format(DATE_FORMAT)).append(":\n\n");

		boolean found = false;
		for (Task t : taskService.getAllTasks()) {
			if (t.getReminderTime().toLocalDate().equals(date)) {
				sb.append(" - ").append(t.toDisplayString()).append("\n");
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

		dayArea.setText(sb.toString());
	}

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
				if (t.getReminderTime().toLocalDate().equals(currentDate)) {
					html.append("• ").append(t.getTitle()).append("<br/>");
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
				if (t.getReminderTime().toLocalDate().equals(currentDate)) {
					html.append("• ").append(t.getTitle()).append("<br/>");
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
			if (t.getReminderTime().toLocalDate().equals(date)) {
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
		String host = "outlook.office365.com";
		int port = 993;
		String user = "pjimenez@academyplus.us";
		String pass = "3p0GOM76vXVv";
		
		EmailWatcher watcher = new EmailWatcher(host, port, user, pass, true);

		emailScheduler = Executors.newSingleThreadScheduledExecutor();
		emailScheduler.scheduleAtFixedRate(watcher::checkNewEmails, 10, 60, TimeUnit.SECONDS);
	}

	private static void checkReminders(JFrame frame) {
		LocalDateTime now = LocalDateTime.now();
		List<Task> dueTasks = taskService.getDueTasks(now);

		if (dueTasks.isEmpty()) {
			return;
		}

		for (Task t : dueTasks) {
			String mensaje = "Recordatorio: " + t.toDisplayString();
			WhatsAppClient.sendTextMessage(mensaje);
			t.setReminded(true);
		}

		SwingUtilities.invokeLater(() -> {
			for (Task t : dueTasks) {
				JOptionPane.showMessageDialog(frame, "¡Recordatorio!\n" + t.toDisplayString(), "Tarea pendiente",
						JOptionPane.INFORMATION_MESSAGE);
			}
			refreshAllViews();
		});
	}
}
