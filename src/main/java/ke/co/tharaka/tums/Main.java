package ke.co.tharaka.tums;

import ke.co.tharaka.tums.config.AppConfig;
import ke.co.tharaka.tums.dao.JdbcAcademicCatalogDao;
import ke.co.tharaka.tums.dao.JdbcStudentDao;
import ke.co.tharaka.tums.dao.JdbcUserDao;
import ke.co.tharaka.tums.service.AdminBootstrapService;
import ke.co.tharaka.tums.service.AcademicCatalogService;
import ke.co.tharaka.tums.service.AuthenticationService;
import ke.co.tharaka.tums.service.StudentService;
import ke.co.tharaka.tums.ui.LoginFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.Console;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length == 1 && "--create-admin".equals(args[0])) {
            createAdministrator();
            return;
        }

        if (args.length > 0) {
            System.err.println("Usage: TUMS [--create-admin]");
            return;
        }

        try {
            final AppConfig config = AppConfig.fromEnvironment(System.getenv());
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception exception) {
                        LOGGER.log(Level.WARNING, "Could not apply the system look and feel.", exception);
                    }
                    AuthenticationService authenticationService =
                            new AuthenticationService(new JdbcUserDao(config));
                    StudentService studentService = new StudentService(new JdbcStudentDao(config));
                    AcademicCatalogService academicCatalogService = new AcademicCatalogService(
                            new JdbcAcademicCatalogDao(config));
                    new LoginFrame(authenticationService, studentService, academicCatalogService).setVisible(true);
                }
            });
        } catch (IllegalArgumentException exception) {
            LOGGER.log(Level.SEVERE, "TUMS configuration is invalid.", exception);
            JOptionPane.showMessageDialog(null, exception.getMessage(), "TUMS configuration",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void createAdministrator() {
        Console console = System.console();
        if (console == null) {
            System.err.println("Administrator setup requires a terminal so the password can be entered securely.");
            return;
        }

        char[] password = null;
        char[] confirmation = null;
        try {
            AppConfig config = AppConfig.fromEnvironment(System.getenv());
            String username = console.readLine("Administrator username: ");
            password = console.readPassword("Administrator password (at least 12 characters): ");
            confirmation = console.readPassword("Confirm administrator password: ");
            if (password == null || confirmation == null) {
                System.err.println("Administrator setup was cancelled.");
                return;
            }
            if (!java.util.Arrays.equals(password, confirmation)) {
                System.err.println("The passwords do not match.");
                return;
            }

            new AdminBootstrapService(config).createFirstAdministrator(username, password);
            console.printf("Administrator account created.%n");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            LOGGER.log(Level.WARNING, "Administrator setup validation failed.", exception);
            System.err.println(exception.getMessage());
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Administrator setup failed.", exception);
            System.err.println("Administrator setup failed. Check the database configuration and setup instructions.");
        } finally {
            if (password != null) {
                java.util.Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                java.util.Arrays.fill(confirmation, '\0');
            }
        }
    }
}
