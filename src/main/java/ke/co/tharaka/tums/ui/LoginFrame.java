package ke.co.tharaka.tums.ui;

import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.service.AcademicCatalogService;
import ke.co.tharaka.tums.service.AuthenticationService;
import ke.co.tharaka.tums.service.StudentService;
import ke.co.tharaka.tums.service.StaffService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoginFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());
    private static final Color NAVY = new Color(24, 43, 69);
    private static final Color GREEN = new Color(24, 130, 88);

    private final AuthenticationService authenticationService;
    private final StudentService studentService;
    private final StaffService staffService;
    private final AcademicCatalogService academicCatalogService;
    private final JTextField usernameField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JButton loginButton = new JButton("Sign in");

    public LoginFrame(
            AuthenticationService authenticationService,
            StudentService studentService,
            StaffService staffService,
            AcademicCatalogService academicCatalogService) {
        super("TUMS | Sign in");
        this.authenticationService = authenticationService;
        this.studentService = studentService;
        this.staffService = staffService;
        this.academicCatalogService = academicCatalogService;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(420, 400));
        setSize(470, 440);
        setLocationRelativeTo(null);
        setContentPane(createContent());
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(event -> signIn());
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 248, 250));

        JPanel brand = new JPanel(new BorderLayout());
        brand.setBackground(NAVY);
        brand.setBorder(BorderFactory.createEmptyBorder(26, 34, 26, 34));
        JLabel title = new JLabel("THARAKA UNIVERSITY");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        JLabel subtitle = new JLabel("Management System");
        subtitle.setForeground(new Color(205, 218, 228));
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        JPanel brandText = new JPanel(new BorderLayout(0, 5));
        brandText.setOpaque(false);
        brandText.add(title, BorderLayout.NORTH);
        brandText.add(subtitle, BorderLayout.SOUTH);
        brand.add(brandText, BorderLayout.CENTER);
        root.add(brand, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(245, 248, 250));
        form.setBorder(BorderFactory.createEmptyBorder(24, 42, 30, 42));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 0, 5, 0);

        JLabel heading = new JLabel("Welcome back");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        heading.setForeground(NAVY);
        form.add(heading, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(20, 0, 4, 0);
        form.add(new JLabel("Username"), constraints);
        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 8, 0);
        form.add(usernameField, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(8, 0, 4, 0);
        form.add(new JLabel("Password"), constraints);
        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 16, 0);
        form.add(passwordField, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 0, 0, 0);
        loginButton.setBackground(GREEN);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(0, 40));
        form.add(loginButton, constraints);

        JLabel note = new JLabel("Sign in with your authorized TUMS account.");
        note.setForeground(new Color(100, 113, 126));
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        constraints.gridy++;
        constraints.insets = new Insets(14, 0, 0, 0);
        form.add(note, constraints);

        root.add(form, BorderLayout.CENTER);
        return root;
    }

    private void signIn() {
        final String username = usernameField.getText().trim();
        final char[] password = passwordField.getPassword();
        if (username.isEmpty() || password.length == 0) {
            Arrays.fill(password, '\0');
            JOptionPane.showMessageDialog(this, "Enter both your username and password.",
                    "Sign in", JOptionPane.WARNING_MESSAGE);
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");
        new SwingWorker<UserAccount, Void>() {
            @Override
            protected UserAccount doInBackground() throws SQLException {
                try {
                    return authenticationService.authenticate(username, password);
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                loginButton.setText("Sign in");
                passwordField.setText("");
                try {
                    UserAccount account = get();
                    if (account == null) {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "The username or password is incorrect, or the account is inactive.",
                                "Sign in failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    new DashboardFrame(account, LoginFrame.this, studentService, staffService,
                            academicCatalogService)
                            .setVisible(true);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    LOGGER.log(Level.SEVERE, "Sign-in was interrupted.", exception);
                    showDatabaseError();
                } catch (ExecutionException exception) {
                    LOGGER.log(Level.SEVERE, "Sign-in failed.", exception.getCause());
                    showDatabaseError();
                }
            }
        }.execute();
    }

    private void showDatabaseError() {
        JOptionPane.showMessageDialog(this,
                "Unable to sign in. Check that MySQL is running and TUMS_DB_URL, "
                        + "TUMS_DB_USER, and TUMS_DB_PASSWORD are configured.",
                "Sign-in error", JOptionPane.ERROR_MESSAGE);
    }
}
