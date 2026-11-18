package ke.co.tharaka.tums.ui;

import ke.co.tharaka.tums.model.AcademicCatalogItem;
import ke.co.tharaka.tums.model.StaffMember;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.model.UserRole;
import ke.co.tharaka.tums.service.StaffService;

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
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StaffManagementPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(StaffManagementPanel.class.getName());
    private static final String[] COLUMNS = {
            "Staff number", "Full name", "Qualification", "Department", "Responsibility", "Phone", "Email"
    };

    private final UserAccount account;
    private final StaffService staffService;
    private final boolean editable;
    private final JTextField searchField = new JTextField(22);
    private final JTextField staffNumberField = new JTextField(18);
    private final JTextField nameField = new JTextField(22);
    private final JTextField qualificationField = new JTextField(20);
    private final JComboBox<AcademicCatalogItem> departmentField =
            new JComboBox<AcademicCatalogItem>();
    private final JTextField phoneField = new JTextField(16);
    private final JTextField emailField = new JTextField(22);
    private final JTextField responsibilityField = new JTextField(20);
    private final JButton searchButton = new JButton("Search");
    private final JButton addButton = new JButton("New staff member");
    private final JButton saveButton = new JButton("Save staff member");
    private final JButton archiveButton = new JButton("Archive");
    private final JButton clearButton = new JButton("Clear");
    private final JLabel status = new JLabel("Loading staff records...");
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable staffTable = new JTable(tableModel);
    private List<StaffMember> members = new ArrayList<StaffMember>();
    private long selectedId;

    public StaffManagementPanel(UserAccount account, StaffService staffService) {
        this.account = account;
        this.staffService = staffService;
        this.editable = account.getRole() == UserRole.ADMINISTRATOR;

        setLayout(new BorderLayout(0, 12));
        setBackground(new Color(245, 248, 250));
        add(createHeader(), BorderLayout.NORTH);
        add(createTable(), BorderLayout.CENTER);
        add(createEditor(), BorderLayout.SOUTH);
        configurePermissions();
        bindActions();
        reload();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);

        JLabel heading = new JLabel("Staff records");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        heading.setForeground(new Color(43, 58, 73));
        JLabel description = new JLabel(editable
                ? "Search, register, update, and archive university staff records."
                : "Search and view staff records. Editing is restricted to administrators.");
        description.setForeground(new Color(100, 113, 126));
        JPanel titleArea = new JPanel(new BorderLayout(0, 5));
        titleArea.setOpaque(false);
        titleArea.add(heading, BorderLayout.NORTH);
        titleArea.add(description, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        toolbar.add(searchField, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 0, 6, 0));
        actions.setOpaque(false);
        actions.add(searchButton);
        if (editable) {
            actions.add(addButton);
            actions.add(saveButton);
            actions.add(archiveButton);
            actions.add(clearButton);
        }
        toolbar.add(actions, BorderLayout.EAST);

        header.add(titleArea, BorderLayout.NORTH);
        header.add(toolbar, BorderLayout.CENTER);
        return header;
    }

    private JScrollPane createTable() {
        staffTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        staffTable.setRowHeight(27);
        staffTable.setAutoCreateRowSorter(true);
        staffTable.getTableHeader().setReorderingAllowed(false);
        staffTable.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(staffTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 237)));
        scroll.setPreferredSize(new Dimension(700, 230));
        return scroll;
    }

    private JPanel createEditor() {
        JPanel editor = new JPanel(new BorderLayout(0, 8));
        editor.setBackground(Color.WHITE);
        editor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 237)),
                BorderFactory.createEmptyBorder(11, 12, 11, 12)));

        JLabel heading = new JLabel(editable ? "Staff details" : "Selected staff member");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        JPanel fields = new JPanel(new GridLayout(0, 4, 9, 5));
        fields.setOpaque(false);
        addField(fields, "Staff number", staffNumberField);
        addField(fields, "Full name", nameField);
        addField(fields, "Qualification", qualificationField);
        addField(fields, "Department", departmentField);
        addField(fields, "Phone", phoneField);
        addField(fields, "Email", emailField);
        addField(fields, "Responsibility", responsibilityField);

        editor.add(heading, BorderLayout.NORTH);
        editor.add(fields, BorderLayout.CENTER);
        editor.add(status, BorderLayout.SOUTH);
        return editor;
    }

    private static void addField(JPanel fields, String label, java.awt.Component component) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(new Color(100, 113, 126));
        fields.add(fieldLabel);
        fields.add(component);
    }

    private void configurePermissions() {
        staffNumberField.setEditable(editable);
        nameField.setEditable(editable);
        qualificationField.setEditable(editable);
        departmentField.setEnabled(editable);
        phoneField.setEditable(editable);
        emailField.setEditable(editable);
        responsibilityField.setEditable(editable);
        addButton.setVisible(editable);
        saveButton.setVisible(editable);
        archiveButton.setVisible(editable);
        clearButton.setVisible(editable);
    }

    private void bindActions() {
        searchButton.addActionListener(event -> reload());
        searchField.addActionListener(event -> reload());
        addButton.addActionListener(event -> clearForm());
        clearButton.addActionListener(event -> clearForm());
        saveButton.addActionListener(event -> saveMember());
        archiveButton.addActionListener(event -> archiveSelected());
        staffTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedMember();
            }
        });
    }

    private void reload() {
        final String query = searchField.getText().trim();
        setBusy(true);
        runAsync("load staff records",
                () -> new StaffSnapshot(
                        staffService.search(account, query),
                        staffService.findDepartments(account)),
                snapshot -> {
                    members = snapshot.members;
                    departmentField.removeAllItems();
                    for (AcademicCatalogItem department : snapshot.departments) {
                        departmentField.addItem(department);
                    }
                    tableModel.setRowCount(0);
                    for (StaffMember member : members) {
                        tableModel.addRow(new Object[]{
                                member.getStaffNumber(),
                                member.getFullName(),
                                member.getQualification(),
                                member.getDepartmentName(),
                                member.getResponsibility(),
                                member.getPhone(),
                                member.getEmail()
                        });
                    }
                    status.setText(members.size() == 500
                            ? "500 staff records shown (maximum reached; refine your search)."
                            : members.size() + " active staff record(s).");
                    clearForm();
                },
                "Could not load staff records. Check the database connection and try again.");
    }

    private void showSelectedMember() {
        int selectedRow = staffTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int modelRow = staffTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= members.size()) {
            return;
        }
        StaffMember member = members.get(modelRow);
        selectedId = member.getId();
        staffNumberField.setText(member.getStaffNumber());
        nameField.setText(member.getFullName());
        qualificationField.setText(member.getQualification());
        selectDepartment(member.getDepartmentId());
        phoneField.setText(member.getPhone());
        emailField.setText(member.getEmail());
        responsibilityField.setText(member.getResponsibility());
        archiveButton.setEnabled(editable);
    }

    private void selectDepartment(long departmentId) {
        for (int index = 0; index < departmentField.getItemCount(); index++) {
            if (departmentField.getItemAt(index).getId() == departmentId) {
                departmentField.setSelectedIndex(index);
                return;
            }
        }
        departmentField.setSelectedIndex(-1);
    }

    private void saveMember() {
        AcademicCatalogItem department = (AcademicCatalogItem) departmentField.getSelectedItem();
        final StaffMember member = new StaffMember(
                selectedId,
                staffNumberField.getText(),
                nameField.getText(),
                qualificationField.getText(),
                department == null ? 0 : department.getId(),
                department == null ? "" : department.getName(),
                phoneField.getText(),
                emailField.getText(),
                responsibilityField.getText());
        final boolean creating = selectedId == 0;
        setBusy(true);
        runAsync(creating ? "create staff member" : "update staff member",
                () -> {
                    if (creating) {
                        return staffService.create(account, member);
                    }
                    staffService.update(account, member);
                    return member;
                },
                result -> {
                    JOptionPane.showMessageDialog(this,
                            creating ? "Staff record created." : "Staff record updated.",
                            "Staff records", JOptionPane.INFORMATION_MESSAGE);
                    reload();
                },
                "Could not save this staff record. The staff number may already exist; "
                        + "check the details and database connection.");
    }

    private void archiveSelected() {
        if (selectedId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a staff member to archive.",
                    "Staff records", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Archive this staff record? It will be hidden from the active staff list.",
                "Confirm archive", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        final long id = selectedId;
        setBusy(true);
        runAsync("archive staff member",
                () -> {
                    staffService.archive(account, id);
                    return id;
                },
                result -> reload(),
                "Could not archive this staff record. Refresh the list and try again.");
    }

    private void clearForm() {
        selectedId = 0;
        staffTable.clearSelection();
        staffNumberField.setText("");
        nameField.setText("");
        qualificationField.setText("");
        departmentField.setSelectedIndex(departmentField.getItemCount() > 0 ? 0 : -1);
        phoneField.setText("");
        emailField.setText("");
        responsibilityField.setText("");
        archiveButton.setEnabled(false);
    }

    private void setBusy(boolean busy) {
        searchButton.setEnabled(!busy);
        searchField.setEnabled(!busy);
        staffTable.setEnabled(!busy);
        addButton.setEnabled(!busy && editable);
        saveButton.setEnabled(!busy && editable);
        archiveButton.setEnabled(!busy && editable && selectedId > 0);
        clearButton.setEnabled(!busy);
    }

    private <T> void runAsync(
            String operation,
            Callable<T> task,
            Consumer<T> onSuccess,
            String defaultErrorMessage) {
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
                    LOGGER.log(Level.SEVERE, "Staff operation was interrupted: " + operation, exception);
                    showError(defaultErrorMessage);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    LOGGER.log(Level.SEVERE, "Staff operation failed: " + operation, cause);
                    showError(cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                            || cause instanceof SecurityException ? cause.getMessage() : defaultErrorMessage);
                }
            }
        }.execute();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Staff records", JOptionPane.ERROR_MESSAGE);
    }

    private static final class StaffSnapshot {
        private final List<StaffMember> members;
        private final List<AcademicCatalogItem> departments;

        private StaffSnapshot(List<StaffMember> members, List<AcademicCatalogItem> departments) {
            this.members = members;
            this.departments = departments;
        }
    }
}
