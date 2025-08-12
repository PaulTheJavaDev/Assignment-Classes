package de.hup.home.assignment;

import de.hup.home.logic.data.Assignment;
import de.hup.home.logic.models.User;
import de.hup.home.logic.models.UserManager;
import de.hup.home.plan.PlannerConstants;
import de.hup.home.plan.enums.Lesson;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.*;
import java.util.*;

import static de.hup.home.plan.PlannerConstants.*;

public class AssignmentCreator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(PlannerConstants.pattern);

    private final JTextField dueDateField = new JTextField(PlannerConstants.pattern);
    private final JComboBox<Lesson> lessonOptions = new JComboBox<>();
    private final JCheckBox completedBox = new JCheckBox("Completed");
    private final JTextField notesField = new JTextField();
    private final AssignmentManager manager = new AssignmentManager();

    private JPanel listPanel;
    private JFrame parentFrame;

    public JFrame createHomeworkFrame() {
        parentFrame = new JFrame("Assignments");
        parentFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        parentFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        parentFrame.setResizable(false);
        parentFrame.setLayout(new AssignmentLayoutManager());

        listPanel = createAssignmentsListPanel();
        parentFrame.add(createAssignmentsScrollPane(listPanel)); //above_x make a sort button
        parentFrame.add(createAssignmentInputPanel());
        parentFrame.add(createSortButton());
        loadAssignmentsForCurrentUser();
        return parentFrame;
    }

    //saves the content of the JScroller
    private JPanel createAssignmentsListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }



    private void sortPanelComponents(String criteria, JPanel panel) {



    }

    private JScrollPane createAssignmentsScrollPane(JPanel listPanel) {
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBounds(VIEW_ALL_ASSIGNMENTS_X, VIEW_ALL_ASSIGNMENTS_Y,
                VIEW_ALL_ASSIGNMENTS_WIDTH, VIEW_ALL_ASSIGNMENTS_HEIGHT);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        return scrollPane;
    }

    private JPanel createAssignmentInputPanel() {
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(220, 220, 220));
        panel.setBounds(CREATE_NEW_ASSIGNMENT_X, CREATE_NEW_ASSIGNMENT_Y,
                CREATE_NEW_ASSIGNMENT_WITH, CREATE_NEW_ASSIGNMENT_HEIGHT);

        panel.add(createAssignmentHeader());
        configureLessonOptions();
        configureField(dueDateField, DUE_DATE_X, DUE_DATE_Y, DUE_DATE_WIDTH, DUE_DATE_HEIGHT);
        configureField(notesField, NOTES_X, NOTES_Y, NOTES_WIDTH, NOTES_HEIGHT);
        configureField(completedBox, CHECKBOX_X, CHECKBOX_Y, CHECKBOX_WIDTH, CHECKBOX_HEIGHT);

        panel.add(lessonOptions);
        panel.add(dueDateField);
        panel.add(notesField);
        panel.add(completedBox);
        panel.add(createSubmitButton());
        panel.add(createSaveButton());

        return panel;
    }

    private void configureLessonOptions() {
        lessonOptions.setModel(new DefaultComboBoxModel<>(Lesson.values()));
        lessonOptions.setBounds(LESSON_OPTIONS_X, LESSON_OPTIONS_Y,
                LESSON_OPTIONS_WIDTH, LESSON_OPTIONS_HEIGHT);
        lessonOptions.setFocusable(false);
        lessonOptions.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = (JLabel) new DefaultListCellRenderer()
                    .getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setText(value.getDisplayName());
            return label;
        });
    }

    private void configureField(JComponent field, int x, int y, int width, int height) {
        field.setBounds(x, y, width, height);
    }

    private JLabel createAssignmentHeader() {
        JLabel header = new JLabel("Create Assignment");
        header.setFont(new Font("Arial", Font.BOLD, 20));
        header.setBounds(ASSIGNMENT_HEADER_X, ASSIGNMENT_HEADER_Y,
                ASSIGNMENT_HEADER_WIDTH, ASSIGNMENT_HEADER_HEIGHT);
        return header;
    }

    private JButton createSubmitButton() {
        JButton button = new JButton("Submit");
        button.setBounds(SUBMIT_BUTTON_X, SUBMIT_BUTTON_Y,
                SUBMIT_BUTTON_WIDTH, SUBMIT_BUTTON_HEIGHT);
        button.addActionListener(_ -> handleSubmit());
        return button;
    }

    private void handleSubmit() {
        Optional<Lesson> selectedLesson = Optional.ofNullable((Lesson) lessonOptions.getSelectedItem());
        if (selectedLesson.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame, "Please select a lesson.");
            return;
        }

        String dateText = dueDateField.getText();
        if (!isValidDate(dateText)) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Invalid date format. Please use: " + PlannerConstants.pattern);
            return;
        }

        Date dueDate = java.sql.Date.valueOf(LocalDate.parse(dateText, DATE_FORMATTER));
        Assignment data = new Assignment(selectedLesson.get(), dueDate,
                completedBox.isSelected(), notesField.getText());

        if (!manager.addAssignment(data)) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Invalid Input: Lesson.");
            return;
        }

        getCurrentUser().ifPresent(user -> {
            manager.saveToCSVForUser(user);
            listPanel.add(new AssignmentRow(data, manager));
        });

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JButton createSaveButton() {
        JButton button = new JButton("Save");
        button.setBounds(SUBMIT_BUTTON_X + 100, SUBMIT_BUTTON_Y, 80, 25);
        button.addActionListener(_ -> getCurrentUser().ifPresent(user -> {
            manager.saveToCSVForUser(user);
            JOptionPane.showMessageDialog(parentFrame, "Saved successfully!");
        }));
        return button;
    }

    private void loadAssignmentsForCurrentUser() {
        getCurrentUser().ifPresent(user -> {
            manager.loadFromCSVForUser(user);
            manager.getAssignments().forEach(data ->
                    listPanel.add(new AssignmentRow(data, manager)));
            listPanel.revalidate();
            listPanel.repaint();
        });
    }

    private Optional<User> getCurrentUser() {
        return Optional.ofNullable(UserManager.getInstance().getCurrentUser());
    }

    private boolean isValidDate(String input) {
        try {
            LocalDate date = LocalDate.parse(input, DATE_FORMATTER);
            int currentYear = LocalDate.now().getYear();
            return date.getYear() >= currentYear;
        } catch (DateTimeParseException e) {
            System.out.println(e);
            return false;
        }
    }

}
