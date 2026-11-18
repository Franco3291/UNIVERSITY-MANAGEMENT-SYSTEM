package ke.co.tharaka.tums.ui;

import ke.co.tharaka.tums.model.AppModule;
import ke.co.tharaka.tums.model.Student;
import ke.co.tharaka.tums.model.UserAccount;
import ke.co.tharaka.tums.service.AcademicCatalogService;
import ke.co.tharaka.tums.service.StudentService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;

public final class DashboardFrame extends JFrame {
    private static final Color NAVY = new Color(24, 43, 69);
    private static final Color GREEN = new Color(24, 130, 88);
    private static final Color PAGE = new Color(245, 248, 250);
    private static final Color TEXT = new Color(43, 58, 73);

    private final UserAccount account;
    private final LoginFrame loginFrame;
    private final StudentService studentService;
    private final AcademicCatalogService academicCatalogService;
    private final JPanel contentCards = new JPanel(new CardLayout());
    private final Map<AppModule, JPanel> moduleScreens = new EnumMap<AppModule, JPanel>(AppModule.class);

    public DashboardFrame(
            UserAccount account,
            LoginFrame loginFrame,
            StudentService studentService,
            AcademicCatalogService academicCatalogService) {
        super("TUMS | Dashboard");
        this.account = account;
        this.loginFrame = loginFrame;
        this.studentService = studentService;
        this.academicCatalogService = academicCatalogService;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);
        add(createWorkspace(), BorderLayout.CENTER);
        showModule(AppModule.DASHBOARD);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        JLabel brand = new JLabel("TUMS");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        JLabel identity = new JLabel(account.getUsername() + "  |  "
                + account.getRole().getDisplayName() + "     ");
        identity.setForeground(new Color(225, 233, 239));
        JButton logout = new JButton("Log out");
        logout.setFocusPainted(false);
        logout.addActionListener(event -> logout());

        JPanel right = new JPanel(new BorderLayout(12, 0));
        right.setOpaque(false);
        right.add(identity, BorderLayout.CENTER);
        right.add(logout, BorderLayout.EAST);
        header.add(brand, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new GridLayout(0, 1, 0, 7));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));
        sidebar.setPreferredSize(new Dimension(225, 0));

        for (final AppModule module : AppModule.values()) {
            if (!module.isAllowedFor(account.getRole())) {
                continue;
            }
            JButton button = new JButton(module.getDisplayName());
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFocusPainted(false);
            button.setBackground(Color.WHITE);
            button.setBorder(BorderFactory.createEmptyBorder(11, 12, 11, 8));
            button.addActionListener(event -> showModule(module));
            sidebar.add(button);
        }
        return sidebar;
    }

    private JPanel createWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(PAGE);
        workspace.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        contentCards.setOpaque(false);

        for (AppModule module : AppModule.values()) {
            if (!module.isAllowedFor(account.getRole())) {
                continue;
            }
            JPanel screen;
            if (module == AppModule.DASHBOARD) {
                screen = createHomeScreen();
            } else if (module == AppModule.STUDENTS) {
                screen = new StudentManagementPanel(account, studentService);
            } else if (module == AppModule.STAFF_AND_COURSES) {
                screen = new AcademicCatalogPanel(account, academicCatalogService);
            } else {
                screen = createModulePlaceholder(module);
            }
            moduleScreens.put(module, screen);
            contentCards.add(screen, module.name());
        }

        workspace.add(contentCards, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel createHomeScreen() {
        JPanel home = new JPanel(new BorderLayout(0, 22));
        home.setOpaque(false);
        JPanel introduction = new JPanel(new BorderLayout(0, 7));
        introduction.setOpaque(false);
        JLabel heading = new JLabel("University overview");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        heading.setForeground(TEXT);
        JLabel greeting = new JLabel("Welcome, " + account.getUsername()
                + ". Your dashboard is ready for " + account.getRole().getDisplayName().toLowerCase() + " access.");
        greeting.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        greeting.setForeground(new Color(100, 113, 126));
        introduction.add(heading, BorderLayout.NORTH);
        introduction.add(greeting, BorderLayout.CENTER);

        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.add(createInfoCard("Account", account.getRole().getDisplayName(),
                "Authenticated database account"));
        cards.add(createInfoCard("Access", "Role-based",
                "Navigation is filtered by your assigned role"));
        cards.add(createInfoCard("Next phase", "Student records",
                "Search and manage authorized student records"));

        home.add(introduction, BorderLayout.NORTH);
        home.add(cards, BorderLayout.CENTER);
        return home;
    }

    private JPanel createInfoCard(String title, String value, String detail) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 237)),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(100, 113, 126));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        valueLabel.setForeground(title.equals("Account") ? GREEN : TEXT);
        JLabel detailLabel = new JLabel("<html>" + detail + "</html>");
        detailLabel.setForeground(new Color(100, 113, 126));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(detailLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createModulePlaceholder(AppModule module) {
        JPanel placeholder = new JPanel(new BorderLayout(0, 12));
        placeholder.setOpaque(false);
        JLabel heading = new JLabel(module.getDisplayName());
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        heading.setForeground(TEXT);
        JLabel message = new JLabel("<html>This module is planned but is not implemented yet.</html>");
        message.setForeground(new Color(100, 113, 126));
        placeholder.add(heading, BorderLayout.NORTH);
        placeholder.add(message, BorderLayout.CENTER);
        return placeholder;
    }

    private void showModule(AppModule module) {
        if (!module.isAllowedFor(account.getRole())) {
            return;
        }
        CardLayout layout = (CardLayout) contentCards.getLayout();
        layout.show(contentCards, module.name());
    }

    private void logout() {
        dispose();
        loginFrame.setVisible(true);
    }
}
