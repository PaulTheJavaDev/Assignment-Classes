package de.hup.home.assignment;

import de.hup.home.logic.data.Assignment;
import de.hup.home.logic.models.User;
import de.hup.home.logic.models.UserManager;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.List;

/**
 * AssignmentManager
 */
public class AssignmentManager {

    //TODO: using relative path
    private final String filePath = "src/de/hup/home/files/assignments.csv"; //universal file

    private static final List<Assignment> assignmentsList = new ArrayList<>();



    boolean addAssignment(Assignment data) {
        if (!isValid(data)) return false;
        assignmentsList.add(data);
        return true;
    }

    /**
     * Update an existing assignment (replace the first matching oldData).
     * Returns true if replaced, false if oldData not found.
     */
    boolean updateAssignment(@NotNull final Assignment oldData, @NotNull final Assignment newData) {
        if (!isValid(newData)) return false;
        int index = assignmentsList.indexOf(oldData);
        if (index >= 0) {
            assignmentsList.set(index, newData);
            return true;
        }
        return false;
    }

    //
    public void removeAssignment(@NotNull final Assignment data) {
        assignmentsList.remove(data);
        saveToCSVForUser(UserManager.getInstance().getCurrentUser());
    }

    public List<Assignment> getAssignments() {
        return Collections.unmodifiableList(assignmentsList);
    }

    //save current user's assignments into the universal file
    public void saveToCSVForUser(User user) {
        File file = new File(filePath);

        List<String> allLines = new ArrayList<>();
        String header = "username,lesson,date,completed,notes";

        //Only replaces the current users data
        if (file.exists()) {
            allLines.add(header);
            allLines.addAll(readOtherUsersLines(file, user));
        } else {
            allLines.add(header);
        }

        //add the current user's updated assignments
        allLines.addAll(assignmentsToCSV(user));

        //Write all lines back
        writeAllLines(file, allLines);
    }

    private List<String> readOtherUsersLines(File file, User user) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) { //skip original header
                    isHeader = false;
                    continue;
                }
                /*
                Big time important:
                lets say we have user alex and alexander.
                both rows start with 'alex' so a security issue: fixed here
                 */
                String[] parts = line.split(",", 2);
                if (!parts[0].equalsIgnoreCase(user.getUsername())) {
                    lines.add(line);
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return lines;
    }

    private List<String> assignmentsToCSV(User user) {
        List<String> csvLines = new ArrayList<>();
        for (Assignment data : assignmentsList) {
            if (isValid(data)) {
                csvLines.add(user.getUsername() + "," + data.toCSVLine());
            }
        }
        return csvLines;
    }

    private void writeAllLines(File file, List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //load assignments for a specific user from the universal file
    public void loadFromCSVForUser(User user) {
        assignmentsList.clear();
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                //skip header
                if (firstLine && line.trim().toLowerCase().startsWith("username,")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                //only load lines belonging to this user
                if (!line.startsWith(user.getUsername() + ",")) {
                    continue;
                }

                String[] parts = line.split(",", 2); //Username already known
                if (parts.length < 2) {
                    continue;
                }

                try {
                    Assignment data = Assignment.fromCSVLine(parts[1]);
                    if (isValid(data)) {
                        assignmentsList.add(data);
                    }
                } catch (ParseException | IllegalArgumentException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //basic validation rules
    private boolean isValid(@NotNull final Assignment data) {
        if (data.notes().trim().isEmpty() || data.notes().equalsIgnoreCase("Notes...")) return false;
        return !data.lesson().getDisplayName().equals("+");
    }
}
