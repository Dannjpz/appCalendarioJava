package ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;

public class DatePickerDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LocalDate selectedDate;
	private YearMonth currentMonth;
	private JPanel daysPanel;
	private JLabel monthLabel;

	public DatePickerDialog(JFrame parent, LocalDate initialDate) {
		super(parent, "Seleccionar fecha", true);
		LocalDate baseDate = (initialDate != null) ? initialDate : LocalDate.now();
		this.currentMonth = YearMonth.from(baseDate);
		this.selectedDate = baseDate;

		buildUI();
		updateCalendar();

		pack();
		setLocationRelativeTo(parent);
	}

	private void buildUI() {
		setLayout(new BorderLayout(5, 5));

		// --------- Barra superior con flechas de mes y etiqueta ---------
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton prevButton = new JButton("<");
		JButton nextButton = new JButton(">");

		monthLabel = new JLabel("", SwingConstants.CENTER);

		prevButton.addActionListener(e -> {
			currentMonth = currentMonth.minusMonths(1);
			updateCalendar();
		});

		nextButton.addActionListener(e -> {
			currentMonth = currentMonth.plusMonths(1);
			updateCalendar();
		});

		topPanel.add(prevButton);
		topPanel.add(monthLabel);
		topPanel.add(nextButton);

		// --------- Encabezado Lun–Dom ---------
		JPanel headerRow = new JPanel(new GridLayout(1, 7));
		String[] dayNames = { "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom" };
		for (String dn : dayNames) {
			JLabel lbl = new JLabel(dn, SwingConstants.CENTER);
			lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
			headerRow.add(lbl);
		}

		// --------- Panel de días del mes ---------
		daysPanel = new JPanel(new GridLayout(0, 7, 3, 3));

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(headerRow, BorderLayout.NORTH);
		centerPanel.add(daysPanel, BorderLayout.CENTER);

		// --------- Botón cancelar ---------
		JButton cancelButton = new JButton("Cancelar");
		cancelButton.addActionListener(e -> {
			selectedDate = null;
			dispose();
		});

		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottomPanel.add(cancelButton);

		add(topPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void updateCalendar() {
		daysPanel.removeAll();

		monthLabel.setText(currentMonth.getMonthValue() + "/" + currentMonth.getYear());

		LocalDate firstDay = currentMonth.atDay(1);
		int firstDow = firstDay.getDayOfWeek().getValue();
		int daysInMonth = currentMonth.lengthOfMonth();

		// Huecos antes del primer día para alinear el calendario
		for (int i = 1; i < firstDow; i++) {
			daysPanel.add(new JLabel(""));
		}

		// Botoncito por cada día
		for (int day = 1; day <= daysInMonth; day++) {
			LocalDate date = currentMonth.atDay(day);
			JButton dayButton = new JButton(String.valueOf(day));
			dayButton.setMargin(new Insets(2, 2, 2, 2));

			if (date.equals(selectedDate)) {
				dayButton.setBackground(new Color(173, 216, 230));
				dayButton.setOpaque(true);
				dayButton.setBorderPainted(true);
			}

			dayButton.addActionListener(e -> {
				selectedDate = date;
				dispose();
			});

			daysPanel.add(dayButton);
		}

		daysPanel.revalidate();
		daysPanel.repaint();
		pack();
	}

	public LocalDate getSelectedDate() {
		return selectedDate;
	}

	public static LocalDate showDatePicker(JFrame parent, LocalDate initialDate) {
		DatePickerDialog dialog = new DatePickerDialog(parent, initialDate);
		dialog.setVisible(true);
		return dialog.getSelectedDate();
	}
}
