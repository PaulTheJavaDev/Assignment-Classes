package de.hup.home.assignment;

import de.hup.home.logic.data.Assignment;
import de.hup.home.logic.models.User;
import de.hup.home.logic.models.UserManager;
import de.hup.home.plan.enums.Lesson;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static de.hup.home.plan.PlannerConstants.pattern;

public class AssignmentRow extends JPanel {

    private Assignment data;
    private final AssignmentManager manager;
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(pattern);
    private final User currentUser = UserManager.getInstance().getCurrentUser();

    private final JLabel lessonLabel;
    private final JLabel dueDateLabel;
    private final JLabel notesLabel;
    private final JCheckBox completedCheckBox;

    public AssignmentRow(@NotNull final Assignment data, @NotNull final AssignmentManager manager) {
        this.data = data;
        this.manager = manager;

        setPreferredSize(new Dimension(400, 120));
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(10, 15, 10, 15)
        ));

        // Top: Lesson and Due Date
        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        topPanel.setOpaque(false);

        lessonLabel = new JLabel("Lesson: " + data.lesson().getDisplayName());
        dueDateLabel = new JLabel("Due: " + DATE_FORMAT.format(data.dueDate()));

        lessonLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        dueDateLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        topPanel.add(lessonLabel);
        topPanel.add(dueDateLabel);

        // Middle: Completed checkbox
        JPanel middlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        middlePanel.setOpaque(false);
        completedCheckBox = new JCheckBox("Completed: ");
        completedCheckBox.setSelected(data.completed());
        completedCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        completedCheckBox.setEnabled(false);
        middlePanel.add(completedCheckBox);

        //Bottom: Notes + Edit button
        JPanel notesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        notesPanel.setOpaque(false);
        notesLabel = new JLabel("Notes: " + data.notes());
        notesLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(_ -> openEditDialog());

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(_ -> {
            //remove the data from manager
            manager.removeAssignment(data);

            //remove this panel from the parent container
            Container parent = this.getParent();
            parent.remove(this);
            parent.revalidate();
            parent.repaint();

            manager.saveToCSVForUser(currentUser);
        });

        notesPanel.add(notesLabel);
        notesPanel.add(editButton);
        notesPanel.add(deleteButton);

        add(topPanel, BorderLayout.NORTH);
        add(middlePanel, BorderLayout.CENTER);
        add(notesPanel, BorderLayout.SOUTH);
    }

    private void openEditDialog() {
        JComboBox<Lesson> lessonCombo = new JComboBox<>(Lesson.values());
        lessonCombo.setSelectedItem(data.lesson());

        JTextField dateField = new JTextField(DATE_FORMAT.format(data.dueDate()));
        JTextField notesField = new JTextField(data.notes());
        JCheckBox completedBox = new JCheckBox("Completed", data.completed());

        JPanel editPanel = new JPanel(new GridLayout(4, 2));
        editPanel.add(new JLabel("Lesson:"));
        editPanel.add(lessonCombo);
        editPanel.add(new JLabel("Date:"));
        editPanel.add(dateField);
        editPanel.add(new JLabel("Notes:"));
        editPanel.add(notesField);
        editPanel.add(new JLabel(""));
        editPanel.add(completedBox);

        int result = JOptionPane.showConfirmDialog(this, editPanel, "Edit Assignment", JOptionPane.OK_CANCEL_OPTION);
        if (!(result == JOptionPane.OK_OPTION)) {
            return;
        }

        try {
            Lesson newLesson = (Lesson) lessonCombo.getSelectedItem();
            Date newDate = DATE_FORMAT.parse(dateField.getText());
            String newNotes = notesField.getText();
            boolean newCompleted = completedBox.isSelected();

            Assignment newData = new Assignment(newLesson, newDate, newCompleted, newNotes);

            boolean updated = manager.updateAssignment(this.data, newData);
            if (!updated) {
                manager.addAssignment(newData);
            }

            manager.saveToCSVForUser(currentUser);

            //updates the UI
            this.data = newData;
            lessonLabel.setText("Lesson: " + data.lesson().getDisplayName());
            dueDateLabel.setText("Due: " + DATE_FORMAT.format(data.dueDate()));
            notesLabel.setText("Notes: " + data.notes());
            completedCheckBox.setSelected(data.completed());
        } catch (ParseException _) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use " + pattern);
        }
    }
}
