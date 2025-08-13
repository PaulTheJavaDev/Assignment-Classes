package de.hup.home.plan;

import de.hup.home.logic.models.UserManager;
import de.hup.home.plan.enums.*;
import de.hup.home.logic.data.SaveLessonData;
import de.hup.home.plan.id.*;
import de.hup.home.assignment.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;

import static de.hup.home.plan.PlannerConstants.*;

public class StudentPlannerDashBoard {

    private int buttonIndex = 1;
    private JPanel weekPlanContainer;
    private final ArrayList<LessonBlock> lessonBlocks = new ArrayList<>();
    private final SaveLessonData saveLessonData = new SaveLessonData();
    private final LinkedHashMap<MenuIDs, JFrame> menuFrames = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, Lesson> lessonsMap = saveLessonData.loadLessonsForUser(UserManager.getInstance().getCurrentUser());

    public StudentPlannerDashBoard() {
        createMenuFrames();
        initializeMainDashboard();
    }

    // Main Dashboard initialization
    private void initializeMainDashboard() {
        JFrame mainDashboard = createBaseFrame();

        weekPlanContainer = createWeekPlanContainer();
        JPanel buttonPanel = createButtonPanel();
        addMenuButtons(buttonPanel);
        createWeekDayPlanner();

        mainDashboard.setLayout(new BorderLayout());
        mainDashboard.add(weekPlanContainer, BorderLayout.CENTER);
        mainDashboard.add(buttonPanel, BorderLayout.SOUTH);

        mainDashboard.setVisible(true);
    }

    // Main Background
    private JFrame createBaseFrame() {
        JFrame mainFrame = new JFrame("Main Dashboard");
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        mainFrame.setResizable(true);
        return mainFrame;
    }

    private JPanel createWeekPlanContainer() {
        JPanel panel = new JPanel(new GridLayout(1, WEEKDAYS_AMOUNT, 5, 0));
        panel.setBackground(new Color(0, 0, 0, 0)); // Transparent
        return panel;
    }

    // Holds the Menu Buttons
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBackground(new Color(0, 0, 0, 0));
        panel.add(Box.createHorizontalGlue());
        return panel;
    }

    private void addMenuButtons(JPanel buttonPanel) {
        for (MenuIDs id : MenuIDs.values()) {
            buttonPanel.add(createMenuButton(id));
            buttonPanel.add(Box.createHorizontalGlue());
        }
    }

    private JButton createMenuButton(MenuIDs id) {
        IDButton button = new IDButton(id, id.getDisplayName());
        button.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        button.addActionListener(_ -> showMenuFrame(id));
        return button;
    }

    private void showMenuFrame(@NotNull final MenuIDs id) {
        Optional<JFrame> frame = Optional.ofNullable(menuFrames.get(id));
        frame.ifPresent(f -> {
            f.setVisible(true);
            f.toFront();
        });
    }

    // Menu Frames
    private void createMenuFrames() {
        AssignmentUIManager mainFrame = new AssignmentUIManager();

        for (MenuIDs id : MenuIDs.values()) {
            JFrame frame = switch (id) {
                case HOMEWORK -> mainFrame.createHomeworkFrame();
                default -> new JFrame(id.getDisplayName());
            };
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(false);
            menuFrames.put(id, frame);
        }
    }

    // Week Plan
    private void createWeekDayPlanner() {
        int[][] dayConfig = {
                {4, 3}, // Monday
                {3, 2}, // Tuesday
                {4, 3}, // Wednesday
                {3, 2}, // Thursday
                {4, 3}  // Friday
        };

        buttonIndex = 1;
        for (int i = 0; i < WEEKDAYS_AMOUNT; i++) {
            int dayLessonAmount = dayConfig[i][0];
            int dayBreakAmount = dayConfig[i][1];
            createDayColumn(dayLessonAmount, dayBreakAmount);
        }
    }

    private void createDayColumn(final int lessonAmount, final int breakAmount) {
        JPanel dayPanel = new JPanel(new BlockLayoutManager());
        dayPanel.setOpaque(false);
        weekPlanContainer.add(dayPanel);

        for (int i = 0; i < lessonAmount; i++) {
            Lesson lesson = lessonsMap.getOrDefault(buttonIndex, Lesson.NONE);
            LessonBlock block = new LessonBlock(new LessonEditor(this), buttonIndex, lesson);
            block.setLesson(lesson);
            lessonBlocks.add(block);
            dayPanel.add(block);
            buttonIndex++;
            if (i < breakAmount) {
                dayPanel.add(new BreakBlock());
            }
        }
    }

    public void saveAllLessons() {
        LinkedHashMap<Integer, Lesson> toSave = new LinkedHashMap<>();
        for (LessonBlock block : lessonBlocks) {
            toSave.put(block.getIndex(), block.getLesson());
        }
        saveLessonData.saveLessonsForUser(UserManager.getInstance().getCurrentUser(), toSave);
    }
}
class BlockLayoutManager implements LayoutManager {

    @Override
    public void addLayoutComponent(String name, Component comp) {}
    @Override
    public void removeLayoutComponent(Component comp) {}
    @Override
    public Dimension preferredLayoutSize(Container parent) { return parent.getSize(); }
    @Override
    public Dimension minimumLayoutSize(Container parent) { return parent.getSize(); }

    @Override
    public void layoutContainer(Container parent) {
        int totalWeight = 0;
        for (Component comp : parent.getComponents()) {
            if (comp instanceof LessonBlock lb) {
                totalWeight += lb.getWeight();
            } else if (comp instanceof BreakBlock bb) {
                totalWeight += bb.getWeight();
            }
        }

        int y = 0;
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();

        for (Component comp : parent.getComponents()) {
            int compWeight = 1;
            if (comp instanceof LessonBlock lb) compWeight = lb.getWeight();
            else if (comp instanceof BreakBlock bb) compWeight = bb.getWeight();

            int compHeight = (int) ((compWeight / (double) totalWeight) * parentHeight);
            comp.setBounds(0, y, parentWidth, compHeight);
            y += compHeight;
        }
    }
}
