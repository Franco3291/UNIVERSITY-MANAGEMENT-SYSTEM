package ke.co.tharaka.tums.ui;

import ke.co.tharaka.tums.model.Student;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.model.UserRole;
import ke.co.tharaka.tums.service.StudentService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StudentManagementPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(StudentManagementPanel.class.getName());
    private static final Color TEXT = new Color(43, 58, 73);
    private static final String[] GENDERS = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"};
    private static final String[] COLUMNS = {
            "Admission number", "Full name", "Gender", "Programme", "Department", "Level", "Email"
    };

    private final UserAccount account;
    private final StudentService studentService;
    private final boolean canManage;
    private final JTextField searchField = new JTextField(22);
    private final JTextField admissionField = new JTextField(16);
    private final JTextField nameField = new JTextField(22);
    private final JComboBox<String> genderField = new JComboBox<String>(GENDERS);
    private final JTextField phoneField = new JTextField(16);
    private final JTextField emailField = new JTextField(22);
    private final JTextField programmeField = new JTextField(22);
    private final JTextField departmentField = new JTextField(22);
    private final JTextField levelField = new JTextField(12);
    private final JLabel recordCount = new JLabel("Loading student records...");
    private final JButton searchButton = new JButton("Search");
    private final JButton addButton = new JButton("New student");
    private final JButton saveButton = new JButton("Save student");
    private final JButton archiveButton = new JButton("Archive");
    private final JButton clearButton = new JButton("Clear");
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable studentTable = new JTable(tableModel);
    private List<Student> students = new ArrayList<Student>();
    private long selectedStudentId;

    public StudentManagementPanel(UserAccount account, StudentService studentService) {
        this.account = account;
        this.studentService = studentService;
        this.canManage = account.getRole() == UserRole.ADMINISTRATOR;

        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(245, 248, 250));
        add(createHeader(), BorderLayout.NORTH);
        add(createTable(), BorderLayout.CENTER);
        add(createEditor(), BorderLayout.SOUTH);
        configurePermissions();
        bindActions();
        loadStudents();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        JLabel title = new JLabel("Student records");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        title.setForeground(TEXT);
        JLabel subtitle = new JLabel(canManage
                ? "Search, register, update, and archive student records."
                : "Search and view student records. Editing is restricted to administrators.");
        subtitle.setForeground(new Color(100, 113, 126));

        JPanel titleArea = new JPanel(new BorderLayout(0, 5));
        titleArea.setOpaque(false);
        titleArea.add(title, BorderLayout.NORTH);
        titleArea.add(subtitle, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new BorderLayout(9, 0));
        toolbar.setOpaque(false);
        toolbar.add(searchField, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 0, 7, 0));
        actions.setOpaque(false);
        actions.add(searchButton);
        actions.add(addButton);
        actions.add(saveButton);
        actions.add(archiveButton);
        actions.add(clearButton);
        toolbar.add(actions, BorderLayout.EAST);

        header.add(titleArea, BorderLayout.NORTH);
        header.add(toolbar, BorderLayout.CENTER);
        return header;
    }

    private JScrollPane createTable() {
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setRowHeight(27);
        studentTable.setAutoCreateRowSorter(true);
        studentTable.getTableHeader().setReorderingAllowed(false);
        studentTable.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 237)));
        scrollPane.setPreferredSize(new Dimension(700, 230));
        return scrollPane;
    }

    private JPanel createEditor() {
        JPanel editor = new JPanel(new BorderLayout(0, 7));
        editor.setBackground(Color.WHITE);
        editor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 237)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel title = new JLabel(canManage ? "Student details" : "Selected student");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        title.setForeground(TEXT);
        JPanel fields = new JPanel(new GridLayout(0, 4, 9, 4));
        fields.setOpaque(false);
        addField(fields, "Admission number", admissionField);
        addField(fields, "Full name", nameField);
        addField(fields, "Gender", genderField);
        addField(fields, "Phone", phoneField);
        addField(fields, "Email", emailField);
        addField(fields, "Academic level", levelField);
        addField(fields, "Programme", programmeField);
        addField(fields, "Department", departmentField);

        editor.add(title, BorderLayout.NORTH);
        editor.add(fields, BorderLayout.CENTER);
        editor.add(recordCount, BorderLayout.SOUTH);
        return editor;
    }

    private static void addField(JPanel fields, String label, java.awt.Component field) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setHorizontalAlignment(SwingConstants.LEFT);
        fieldLabel.setForeground(new Color(100, 113, 126));
        fields.add(fieldLabel);
        fields.add(field);
    }

    private void configurePermissions() {
        boolean editable = canManage;
        admissionField.setEditable(editable);
        nameField.setEditable(editable);
        genderField.setEnabled(editable);
        phoneField.setEditable(editable);
        emailField.setEditable(editable);
        programmeField.setEditable(editable);
        departmentField.setEditable(editable);
        levelField.setEditable(editable);
        addButton.setVisible(editable);
        saveButton.setVisible(editable);
        archiveButton.setVisible(editable);
    }

    private void bindActions() {
        searchButton.addActionListener(event -> loadStudents());
        searchField.addActionListener(event -> loadStudents());
        clearButton.addActionListener(event -> clearForm());
        addButton.addActionListener(event -> clearForm());
        saveButton.addActionListener(event -> saveStudent());
        archiveButton.addActionListener(event -> archiveSelected());
        studentTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedStudent();
            }
        });
    }

    private void loadStudents() {
        final String query = searchField.getText().trim();
        setBusy(true);
        runAsync("load student records",
                () -> studentService.search(account, query),
                result -> {
                    students = result;
                    tableModel.setRowCount(0);
                    for (Student student : students) {
                        tableModel.addRow(new Object[]{
                                student.getAdmissionNumber(),
                                student.getFullName(),
                                displayGender(student.getGender()),
                                student.getProgramme(),
                                student.getDepartment(),
                                student.getAcademicLevel(),
                                student.getEmail()
                        });
                    }
                    recordCount.setText(students.size() + (students.size() == 500
                            ? " records shown (maximum reached; refine your search)."
                            : " record(s) found."));
                    clearForm();
                },
                "Could not load student records. Check the database connection and try again.");
    }

    private void showSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int modelRow = studentTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= students.size()) {
            return;
        }
        Student student = students.get(modelRow);
        selectedStudentId = student.getId();
        admissionField.setText(student.getAdmissionNumber());
        nameField.setText(student.getFullName());
        genderField.setSelectedItem(student.getGender());
        phoneField.setText(student.getPhone());
        emailField.setText(student.getEmail());
        programmeField.setText(student.getProgramme());
        departmentField.setText(student.getDepartment());
        levelField.setText(student.getAcademicLevel());
    }

    private void saveStudent() {
        final Student student = new Student(
                selectedStudentId,
                admissionField.getText(),
                nameField.getText(),
                (String) genderField.getSelectedItem(),
                phoneField.getText(),
                emailField.getText(),
                programmeField.getText(),
                departmentField.getText(),
                levelField.getText());
        final boolean creating = selectedStudentId == 0;
        setBusy(true);
        runAsync(creating ? "create student" : "update student",
                () -> {
                    if (creating) {
                        return studentService.create(account, student);
                    }
                    studentService.update(account, student);
                    return student;
                },
                saved -> {
                    selectedStudentId = saved.getId();
                    JOptionPane.showMessageDialog(this,
                            creating ? "Student record created." : "Student record updated.",
                            "Student records", JOptionPane.INFORMATION_MESSAGE);
                    loadStudents();
                },
                "Could not save the student record. The admission number may already exist; "
                        + "check the details and database connection.");
    }

    private void archiveSelected() {
        if (selectedStudentId == 0) {
            JOptionPane.showMessageDialog(this, "Select a student record to archive.",
                    "Student records", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Archive this student record? It will be hidden from the active student list.",
                "Confirm archive", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        final long id = selectedStudentId;
        setBusy(true);
        runAsync("archive student",
                () -> {
                    studentService.archive(account, id);
                    return id;
                },
                result -> loadStudents(),
                "Could not archive the student record. Refresh the list and try again.");
    }

    private void clearForm() {
        selectedStudentId = 0;
        studentTable.clearSelection();
        admissionField.setText("");
        nameField.setText("");
        genderField.setSelectedIndex(0);
        phoneField.setText("");
        emailField.setText("");
        programmeField.setText("");
        departmentField.setText("");
        levelField.setText("");
        saveButton.setText("Save student");
        archiveButton.setEnabled(false);
        admissionField.setEditable(canManage);
    }

    private void setBusy(boolean busy) {
        searchButton.setEnabled(!busy);
        searchField.setEnabled(!busy);
        addButton.setEnabled(!busy && canManage);
        saveButton.setEnabled(!busy && canManage);
        archiveButton.setEnabled(!busy && canManage && selectedStudentId > 0);
        clearButton.setEnabled(!busy);
        studentTable.setEnabled(!busy);
    }

    private <T> void runAsync(
            String operation,
            Callable<T> task,
            Consumer<T> onSuccess,
            String databaseErrorMessage) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    LOGGER.log(Level.SEVERE, "Student operation was interrupted: " + operation, exception);
                    JOptionPane.showMessageDialog(StudentManagementPanel.this, databaseErrorMessage,
                            "Student records", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    LOGGER.log(Level.SEVERE, "Student operation failed: " + operation, cause);
                    String message = cause instanceof IllegalArgumentException
                            || cause instanceof IllegalStateException
                            || cause instanceof SecurityException
                            ? cause.getMessage()
                            : databaseErrorMessage;
                    JOptionPane.showMessageDialog(StudentManagementPanel.this, message,
                            "Student records", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static String displayGender(String gender) {
        return gender.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
    }
}
