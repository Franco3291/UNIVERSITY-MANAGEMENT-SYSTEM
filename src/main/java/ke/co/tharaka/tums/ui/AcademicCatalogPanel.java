package ke.co.tharaka.tums.ui;

import ke.co.tharaka.tums.dao.AcademicCatalogDao.Kind;
import ke.co.tharaka.tums.model.AcademicCatalogItem;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.model.UserRole;
import ke.co.tharaka.tums.service.AcademicCatalogService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AcademicCatalogPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(AcademicCatalogPanel.class.getName());
    private static final Color TEXT = new Color(43, 58, 73);

    private final JTabbedPane tabs = new JTabbedPane();

    public AcademicCatalogPanel(UserAccount account, AcademicCatalogService service) {
        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(245, 248, 250));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JLabel heading = new JLabel("Departments, programmes and courses");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        heading.setForeground(TEXT);
        JLabel subtitle = new JLabel(
                "Manage the academic structure. Lecturers may view the catalogue; only administrators may edit it.");
        subtitle.setForeground(new Color(100, 113, 126));
        JPanel header = new JPanel(new BorderLayout(0, 5));
        header.setOpaque(false);
        header.add(heading, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);

        tabs.addTab("Departments", new CatalogTab(account, service, Kind.DEPARTMENT));
        tabs.addTab("Programmes", new CatalogTab(account, service, Kind.PROGRAMME));
        tabs.addTab("Courses", new CatalogTab(account, service, Kind.COURSE));
        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private static final class CatalogTab extends JPanel {
        private final UserAccount account;
        private final AcademicCatalogService service;
        private final Kind kind;
        private final boolean editable;
        private final JTextField searchField = new JTextField(22);
        private final JButton searchButton = new JButton("Search");
        private final JButton addButton = new JButton("Add");
        private final JButton editButton = new JButton("Edit");
        private final JButton archiveButton = new JButton("Archive");
        private final JButton refreshButton = new JButton("Refresh");
        private final JLabel status = new JLabel("Loading...");
        private final DefaultTableModel tableModel;
        private final JTable table;
        private List<AcademicCatalogItem> records = new ArrayList<AcademicCatalogItem>();

        private CatalogTab(UserAccount account, AcademicCatalogService service, Kind kind) {
            this.account = account;
            this.service = service;
            this.kind = kind;
            this.editable = account.getRole() == UserRole.ADMINISTRATOR;
            this.tableModel = new DefaultTableModel(columns(kind), 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            this.table = new JTable(tableModel);

            setLayout(new BorderLayout(0, 10));
            setBackground(new Color(245, 248, 250));
            setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
            add(createToolbar(), BorderLayout.NORTH);
            add(createTable(), BorderLayout.CENTER);
            add(status, BorderLayout.SOUTH);
            addActions();
            editButton.setEnabled(false);
            archiveButton.setEnabled(false);
            reload();
        }

        private JPanel createToolbar() {
            JPanel toolbar = new JPanel(new BorderLayout(8, 0));
            toolbar.setOpaque(false);
            toolbar.add(searchField, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new GridLayout(1, 0, 6, 0));
            buttons.setOpaque(false);
            buttons.add(searchButton);
            buttons.add(refreshButton);
            if (editable) {
                buttons.add(addButton);
                buttons.add(editButton);
                buttons.add(archiveButton);
            }
            toolbar.add(buttons, BorderLayout.EAST);
            return toolbar;
        }

        private JScrollPane createTable() {
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setRowHeight(26);
            table.setAutoCreateRowSorter(true);
            table.getTableHeader().setReorderingAllowed(false);
            table.setFillsViewportHeight(true);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 237)));
            return scroll;
        }

        private void addActions() {
            searchButton.addActionListener(event -> reload());
            refreshButton.addActionListener(event -> {
                searchField.setText("");
                reload();
            });
            searchField.addActionListener(event -> reload());
            addButton.addActionListener(event -> editRecord(null));
            editButton.addActionListener(event -> editRecord(selectedRecord()));
            archiveButton.addActionListener(event -> archiveSelected());
            table.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting()) {
                    boolean selected = selectedRecord() != null;
                    editButton.setEnabled(!isBusy() && editable && selected);
                    archiveButton.setEnabled(!isBusy() && editable && selected);
                }
            });
        }

        private void reload() {
            final String query = searchField.getText().trim();
            setBusy(true);
            runAsync("load " + label(kind),
                    () -> {
                        List<AcademicCatalogItem> rows = service.search(account, kind, query);
                        List<AcademicCatalogItem> departments = kind == Kind.PROGRAMME
                                || kind == Kind.COURSE
                                ? service.search(account, Kind.DEPARTMENT, "")
                                : Collections.<AcademicCatalogItem>emptyList();
                        List<AcademicCatalogItem> programmes = kind == Kind.COURSE
                                ? service.search(account, Kind.PROGRAMME, "")
                                : Collections.<AcademicCatalogItem>emptyList();
                        return new CatalogSnapshot(rows, departments, programmes);
                    },
                    snapshot -> {
                        lastSnapshot = snapshot;
                        records = snapshot.records;
                        tableModel.setRowCount(0);
                        for (AcademicCatalogItem item : records) {
                            tableModel.addRow(rowValues(item));
                        }
                        status.setText(records.size() == 500
                                ? "500 records shown (maximum reached; refine your search)."
                                : records.size() + " active " + label(kind) + "(s).");
                        editButton.setEnabled(false);
                        archiveButton.setEnabled(false);
                    },
                    "Could not load the academic catalogue. Check the database connection and try again.");
        }

        private void editRecord(AcademicCatalogItem existing) {
            List<AcademicCatalogItem> parentOptions = kind == Kind.PROGRAMME
                    ? latestDepartments()
                    : kind == Kind.COURSE ? latestProgrammes() : Collections.<AcademicCatalogItem>emptyList();
            if (kind != Kind.DEPARTMENT && parentOptions.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        kind == Kind.PROGRAMME
                                ? "Create an active department before adding a programme."
                                : "Create an active programme before adding a course.",
                        "Academic catalogue", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JTextField code = new JTextField(existing == null ? "" : existing.getCode(), 22);
            JTextField name = new JTextField(existing == null ? "" : existing.getName(), 22);
            JTextArea description = new JTextArea(existing == null ? "" : existing.getDescription(), 3, 22);
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            JComboBox<AcademicCatalogItem> parent = new JComboBox<AcademicCatalogItem>(
                    parentOptions.toArray(new AcademicCatalogItem[parentOptions.size()]));
            JTextField creditHours = new JTextField(existing == null ? "3"
                    : Integer.toString(existing.getCreditHours()), 5);
            if (existing != null && kind != Kind.DEPARTMENT) {
                for (int index = 0; index < parent.getItemCount(); index++) {
                    if (parent.getItemAt(index).getId() == existing.getParentId()) {
                        parent.setSelectedIndex(index);
                        break;
                    }
                }
            }

            JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
            fields.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            fields.add(new JLabel(codeLabel()));
            fields.add(code);
            fields.add(new JLabel("Name"));
            fields.add(name);
            if (kind != Kind.DEPARTMENT) {
                fields.add(new JLabel(kind == Kind.PROGRAMME ? "Department" : "Programme"));
                fields.add(parent);
            }
            if (kind == Kind.COURSE) {
                fields.add(new JLabel("Credit hours (1-12)"));
                fields.add(creditHours);
            }
            fields.add(new JLabel("Description"));
            fields.add(new JScrollPane(description));

            int choice = JOptionPane.showConfirmDialog(this, fields,
                    (existing == null ? "Add " : "Edit ") + label(kind),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            final long id = existing == null ? 0 : existing.getId();
            final long parentId = kind == Kind.DEPARTMENT ? 0
                    : ((AcademicCatalogItem) parent.getSelectedItem()).getId();
            final String parentName = kind == Kind.DEPARTMENT ? ""
                    : ((AcademicCatalogItem) parent.getSelectedItem()).getName();
            final int credits;
            try {
                credits = kind == Kind.COURSE ? Integer.parseInt(creditHours.getText().trim()) : 0;
            } catch (NumberFormatException exception) {
                JOptionPane.showMessageDialog(this, "Enter a whole number for course credit hours.",
                        "Academic catalogue", JOptionPane.WARNING_MESSAGE);
                return;
            }
            final AcademicCatalogItem item = new AcademicCatalogItem(
                    id, parentId, parentName, code.getText(), name.getText(),
                    description.getText(), credits);

            setBusy(true);
            runAsync(existing == null ? "create " + label(kind) : "update " + label(kind),
                    () -> {
                        if (existing == null) {
                            return service.create(account, kind, item);
                        }
                        service.update(account, kind, item);
                        return item;
                    },
                    result -> reload(),
                    "Could not save this " + label(kind)
                            + ". Check the fields, code uniqueness, and database connection.");
        }

        private List<AcademicCatalogItem> latestDepartments() {
            return latestParents(Kind.DEPARTMENT);
        }

        private List<AcademicCatalogItem> latestProgrammes() {
            return latestParents(Kind.PROGRAMME);
        }

        private List<AcademicCatalogItem> latestParents(Kind parentKind) {
            if (kind == Kind.PROGRAMME && parentKind == Kind.DEPARTMENT) {
                return lastSnapshot == null ? Collections.<AcademicCatalogItem>emptyList()
                        : lastSnapshot.departments;
            }
            if (kind == Kind.COURSE && parentKind == Kind.PROGRAMME) {
                return lastSnapshot == null ? Collections.<AcademicCatalogItem>emptyList()
                        : lastSnapshot.programmes;
            }
            return Collections.emptyList();
        }

        private CatalogSnapshot lastSnapshot;

        private AcademicCatalogItem selectedRecord() {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                return null;
            }
            int modelRow = table.convertRowIndexToModel(selectedRow);
            return modelRow < 0 || modelRow >= records.size() ? null : records.get(modelRow);
        }

        private void archiveSelected() {
            AcademicCatalogItem selected = selectedRecord();
            if (selected == null) {
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Archive " + selected.getCode() + " — " + selected.getName()
                            + "? It will be hidden from the active catalogue.",
                    "Confirm archive", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            setBusy(true);
            runAsync("archive " + label(kind),
                    () -> {
                        service.archive(account, kind, selected.getId());
                        return selected.getId();
                    },
                    result -> reload(),
                    "Could not archive this " + label(kind)
                            + ". Archive its active child records first, then try again.");
        }

        private <T> void runAsync(
                String operation,
                Callable<T> task,
                Consumer<T> onSuccess,
                String defaultError) {
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
                        LOGGER.log(Level.SEVERE, "Academic catalog operation interrupted: " + operation, exception);
                        showError(defaultError);
                    } catch (ExecutionException exception) {
                        Throwable cause = exception.getCause();
                        LOGGER.log(Level.SEVERE, "Academic catalog operation failed: " + operation, cause);
                        showError(cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                                || cause instanceof SecurityException ? cause.getMessage() : defaultError);
                    }
                }
            }.execute();
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Academic catalogue", JOptionPane.ERROR_MESSAGE);
        }

        private boolean isBusy() {
            return !searchButton.isEnabled();
        }

        private void setBusy(boolean busy) {
            searchButton.setEnabled(!busy);
            refreshButton.setEnabled(!busy);
            searchField.setEnabled(!busy);
            addButton.setEnabled(!busy && editable);
            editButton.setEnabled(!busy && editable && selectedRecord() != null);
            archiveButton.setEnabled(!busy && editable && selectedRecord() != null);
            table.setEnabled(!busy);
        }

        private Object[] rowValues(AcademicCatalogItem item) {
            if (kind == Kind.DEPARTMENT) {
                return new Object[]{item.getCode(), item.getName(), item.getDescription()};
            }
            if (kind == Kind.PROGRAMME) {
                return new Object[]{item.getCode(), item.getName(), item.getParentName(), item.getDescription()};
            }
            return new Object[]{item.getCode(), item.getName(), item.getParentName(),
                    item.getCreditHours(), item.getDescription()};
        }

        private String codeLabel() {
            switch (kind) {
                case DEPARTMENT:
                    return "Department code";
                case PROGRAMME:
                    return "Programme code";
                case COURSE:
                    return "Course code";
                default:
                    throw new IllegalStateException("Unsupported academic catalog type.");
            }
        }

        private static Object[] columns(Kind kind) {
            if (kind == Kind.DEPARTMENT) {
                return new Object[]{"Department code", "Department name", "Description"};
            }
            if (kind == Kind.PROGRAMME) {
                return new Object[]{"Programme code", "Programme name", "Department", "Description"};
            }
            return new Object[]{"Course code", "Course name", "Programme", "Credit hours", "Description"};
        }

        private static String label(Kind kind) {
            return kind.name().toLowerCase(java.util.Locale.ROOT);
        }

        private final class CatalogSnapshot {
            private final List<AcademicCatalogItem> records;
            private final List<AcademicCatalogItem> departments;
            private final List<AcademicCatalogItem> programmes;

            private CatalogSnapshot(
                    List<AcademicCatalogItem> records,
                    List<AcademicCatalogItem> departments,
                    List<AcademicCatalogItem> programmes) {
                this.records = records;
                this.departments = departments;
                this.programmes = programmes;
            }
        }
    }
}
