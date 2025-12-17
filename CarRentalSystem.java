import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

// ===========================
// 1. OOP MODELS
// ===========================

abstract class User {
    protected String id;
    protected String name;
    protected String password;

    public User(String id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }
    
    public String getName() { return name; }
    public boolean checkPassword(String input) { return password.equals(input); }
    public String getId() { return id; }
}

class Customer extends User {
    private String contact;
    private String email;

    public Customer(String name, String password, String contact, String email) {
        super("C" + System.currentTimeMillis(), name, password);
        this.contact = contact;
        this.email = email;
    }

    // Constructor for loading from file (with existing ID)
    public Customer(String id, String name, String password, String contact, String email) {
        super(id, name, password);
        this.contact = contact;
        this.email = email;
    }

    public String getContact() { return contact; }
    public String getEmail() { return email; }
}

class Admin extends User {
    public Admin(String name, String password) {
        super("A001", name, password);
    }
}

class Car {
    private String carId;
    private String brand;
    private String model;
    private double price;
    private boolean isAvailable;
    private String currentRenterId; // Null if available

    public Car(String brand, String model, double price) {
        this.carId = "V" + (int)(Math.random() * 10000);
        this.brand = brand;
        this.model = model;
        this.price = price;
        this.isAvailable = true;
        this.currentRenterId = null;
    }

    // Getters
    public String getCarId() { return carId; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public double getPrice() { return price; }
    public boolean isAvailable() { return isAvailable; }
    public String getCurrentRenterId() { return currentRenterId; }

    // Logic
    public void rent(String userId) {
        this.isAvailable = false;
        this.currentRenterId = userId;
    }

    public void returnCar() {
        this.isAvailable = true;
        this.currentRenterId = null;
    }
}

// ===========================
// 2. CENTRAL DATA STORE
// ===========================
class RentalService {
    public static List<Car> fleet = new ArrayList<>();
    public static List<User> users = new ArrayList<>();
    public static User currentUser = null;
    private static final String CUSTOMERS_FILE = "customers.txt";

    static {
        // Seed Data
        users.add(new Admin("admin", "admin123"));
        // Load saved customers from file
        loadCustomers();
        // Add default customers only if file doesn't exist or is empty
        if (users.stream().noneMatch(u -> u instanceof Customer && u.getName().equals("John Doe"))) {
            users.add(new Customer("John Doe", "123", "999-111-2222", "john@example.com"));
        }
        if (users.stream().noneMatch(u -> u instanceof Customer && u.getName().equals("Alice Smith"))) {
            users.add(new Customer("Alice Smith", "123", "999-333-4444", "alice@example.com"));
        }

        fleet.add(new Car("Toyota", "Camry", 60.0));
        fleet.add(new Car("Honda", "Civic", 55.0));
        fleet.add(new Car("Tesla", "Model 3", 120.0));
        fleet.add(new Car("Ford", "Mustang", 90.0));
    }
    
    public static void addCar(String brand, String model, double price) {
        fleet.add(new Car(brand, model, price));
    }

    // Save all customers to file
    public static void saveCustomers() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CUSTOMERS_FILE))) {
            for (User user : users) {
                if (user instanceof Customer) {
                    Customer c = (Customer) user;
                    writer.println(c.getId() + "|" + c.getName() + "|" + c.password + "|" + 
                                 c.getContact() + "|" + c.getEmail());
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving customers: " + e.getMessage());
        }
    }

    // Load customers from file
    private static void loadCustomers() {
        if (!Files.exists(Paths.get(CUSTOMERS_FILE))) {
            return; // File doesn't exist yet, skip loading
        }
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(CUSTOMERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length == 5) {
                    String id = parts[0];
                    String name = parts[1];
                    String password = parts[2];
                    String contact = parts[3];
                    String email = parts[4];
                    users.add(new Customer(id, name, password, contact, email));
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading customers: " + e.getMessage());
        }
    }

    // Add a new customer and save to file
    public static void addCustomer(Customer customer) {
        users.add(customer);
        saveCustomers();
    }
}

// ===========================
// 3. GUI MAIN CLASS
// ===========================
public class CarRentalSystem extends JFrame {

    // Modern, minimalist palette
    final Color DARK_BLUE = new Color(22, 33, 62);
    final Color BRIGHT_BLUE = new Color(64, 115, 158);
    // Deeper, richer green accent for better contrast
    final Color GREEN_ACCENT = new Color(46, 139, 87);
    final Color RED_ACCENT = new Color(235, 87, 87);
    final Color OFF_WHITE = new Color(245, 247, 250);
    final Color SOFT_GOLD = new Color(208, 180, 123);

    // Typography
    final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 32);
    final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 16);
    final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    public CarRentalSystem() {
        setTitle("Car Rental Management Sys");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Add Screens
        mainPanel.add(new LoginPanel(), "Login");
        mainPanel.add(new AdminDashboard(), "Admin");
        mainPanel.add(new CustomerDashboard(), "Customer");

        add(mainPanel);
        cardLayout.show(mainPanel, "Login");
    }

    // --- UTILS: Custom Button Factory ---
    private JButton createBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(BUTTON_FONT);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // Simple illustration panel for the login screen (original abstract design)
    class IllustrationPanel extends JPanel {
        public IllustrationPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Gradient background
            GradientPaint gp = new GradientPaint(0, 0, DARK_BLUE, w, h, BRIGHT_BLUE);
            g2d.setPaint(gp);
            g2d.fillRoundRect(20, 20, w - 40, h - 40, 40, 40);

            // Abstract "car" shape
            int carWidth = (int) (w * 0.55);
            int carHeight = (int) (h * 0.22);
            int carX = (w - carWidth) / 2;
            int carY = (h - carHeight) / 2;

            g2d.setColor(new Color(255, 255, 255, 30));
            g2d.fillRoundRect(carX, carY, carWidth, carHeight, 40, 40);

            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.fillOval(carX + (int)(carWidth * 0.15), carY + (int)(carHeight * 0.55), 26, 26);
            g2d.fillOval(carX + (int)(carWidth * 0.65), carY + (int)(carHeight * 0.55), 26, 26);

            // Tagline
            g2d.setFont(TITLE_FONT);
            g2d.setColor(Color.WHITE);
            String title = "Car Rental Management System";
            FontMetrics fm = g2d.getFontMetrics();
            int tx = (w - fm.stringWidth(title)) / 2;
            int ty = carY - 25;
            g2d.drawString(title, tx, ty);

            g2d.setFont(SUBTITLE_FONT);
            g2d.setColor(new Color(245, 247, 250, 220));
            String sub1 = "Curated cars.";
            String sub2 = "Effortless journeys.";
            int sub1x = (w - g2d.getFontMetrics().stringWidth(sub1)) / 2;
            int sub2x = (w - g2d.getFontMetrics().stringWidth(sub2)) / 2;
            int sub1y = carY + carHeight + 30;
            int sub2y = sub1y + 22;
            g2d.drawString(sub1, sub1x, sub1y);
            g2d.drawString(sub2, sub2x, sub2y);

            g2d.dispose();
        }
    }

    // --- SCREEN 1: LOGIN ---
    class LoginPanel extends JPanel {
        JTextField userField = new JTextField(18);
        JPasswordField passField = new JPasswordField(18);

        public LoginPanel() {
            setLayout(new BorderLayout());
            setBackground(OFF_WHITE);

            // Left: illustration
            JPanel left = new IllustrationPanel();
            left.setPreferredSize(new Dimension(520, 0));
            add(left, BorderLayout.CENTER);

            // Right: form
            JPanel rightWrapper = new JPanel(new GridBagLayout());
            rightWrapper.setBackground(OFF_WHITE);
            rightWrapper.setBorder(new EmptyBorder(40, 40, 40, 40));

            JPanel form = new JPanel();
            form.setOpaque(false);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

            JLabel heading = new JLabel("Sign in to your space");
            heading.setFont(TITLE_FONT);
            heading.setForeground(DARK_BLUE);
            heading.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel sub = new JLabel("Choose admin or customer access below.");
            sub.setFont(SUBTITLE_FONT);
            sub.setForeground(new Color(120, 129, 149));
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel userRow = new JPanel(new BorderLayout(5, 5));
            userRow.setOpaque(false);
            JLabel uLabel = new JLabel("Username");
            uLabel.setFont(BODY_FONT);
            uLabel.setForeground(DARK_BLUE);
            userField.setFont(BODY_FONT);
            userField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(223, 228, 234)),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            userRow.add(uLabel, BorderLayout.NORTH);
            userRow.add(userField, BorderLayout.CENTER);

            JPanel passRow = new JPanel(new BorderLayout(5, 5));
            passRow.setOpaque(false);
            JLabel pLabel = new JLabel("Password");
            pLabel.setFont(BODY_FONT);
            pLabel.setForeground(DARK_BLUE);
            passField.setFont(BODY_FONT);
            passField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(223, 228, 234)),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            passRow.add(pLabel, BorderLayout.NORTH);
            passRow.add(passField, BorderLayout.CENTER);

            JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
            btnRow.setOpaque(false);
            JButton adminBtn = createBtn("Admin Login", SOFT_GOLD);
            JButton custBtn = createBtn("Customer Login", BRIGHT_BLUE);
            adminBtn.addActionListener(e -> attemptLogin(true));
            custBtn.addActionListener(e -> attemptLogin(false));
            btnRow.add(adminBtn);
            btnRow.add(custBtn);

            JButton regBtn = new JButton("<html><u>Create new customer account</u></html>");
            regBtn.setBorderPainted(false);
            regBtn.setContentAreaFilled(false);
            regBtn.setFocusPainted(false);
            regBtn.setFont(BODY_FONT);
            regBtn.setForeground(BRIGHT_BLUE);
            regBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            regBtn.addActionListener(e -> registerCustomer());
            regBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

            form.add(heading);
            form.add(Box.createVerticalStrut(6));
            form.add(sub);
            form.add(Box.createVerticalStrut(20));
            form.add(userRow);
            form.add(Box.createVerticalStrut(12));
            form.add(passRow);
            form.add(Box.createVerticalStrut(18));
            form.add(btnRow);
            form.add(Box.createVerticalStrut(16));
            form.add(regBtn);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            rightWrapper.add(form, gbc);

            add(rightWrapper, BorderLayout.EAST);
        }

        private void attemptLogin(boolean adminLogin) {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both username and password.");
                return;
            }

            for (User user : RentalService.users) {
                if (user.getName().equalsIgnoreCase(u) && user.checkPassword(p)) {
                    if (adminLogin && !(user instanceof Admin)) {
                        JOptionPane.showMessageDialog(this, "This account is not an admin account.");
                        return;
                    }
                    if (!adminLogin && !(user instanceof Customer)) {
                        JOptionPane.showMessageDialog(this, "Please use the Admin Login button for admin accounts.");
                        return;
                    }

                    RentalService.currentUser = user;
                    userField.setText("");
                    passField.setText("");

                    if (user instanceof Admin) cardLayout.show(mainPanel, "Admin");
                    else cardLayout.show(mainPanel, "Customer");
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, "Invalid credentials. Please try again.");
        }

        private void registerCustomer() {
             JTextField nameField = new JTextField(18);
             JTextField contactField = new JTextField(18);
             JTextField emailField = new JTextField(18);

             JPanel panel = new JPanel();
             panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
             panel.add(new JLabel("Full name:"));
             panel.add(nameField);
             panel.add(Box.createVerticalStrut(6));
             panel.add(new JLabel("Contact number:"));
             panel.add(contactField);
             panel.add(Box.createVerticalStrut(6));
             panel.add(new JLabel("Email address:"));
             panel.add(emailField);

             int result = JOptionPane.showConfirmDialog(
                     this,
                     panel,
                     "Create Customer Account",
                     JOptionPane.OK_CANCEL_OPTION,
                     JOptionPane.PLAIN_MESSAGE
             );

             if (result == JOptionPane.OK_OPTION) {
                 String name = nameField.getText().trim();
                 String contact = contactField.getText().trim();
                 String email = emailField.getText().trim();
                 String pass = JOptionPane.showInputDialog(this, "Set a password:");

                 if (!name.isEmpty() && !contact.isEmpty() && !email.isEmpty() && pass != null && !pass.trim().isEmpty()) {
                     Customer newCustomer = new Customer(name, pass.trim(), contact, email);
                     RentalService.addCustomer(newCustomer);
                     JOptionPane.showMessageDialog(this, "Account created! You can now login as customer.");
                 } else {
                     JOptionPane.showMessageDialog(this, "All fields are required.");
                 }
             }
        }
    }

    // --- SCREEN 2: ADMIN DASHBOARD ---
    class AdminDashboard extends JPanel {
        DefaultTableModel fleetModel;
        JTable fleetTable;
        JTextField brandF, modelF, priceF;

        public AdminDashboard() {
            setLayout(new BorderLayout());
            
            // Top Bar
            JPanel top = new JPanel(new BorderLayout());
            top.setBackground(DARK_BLUE);
            top.setBorder(new EmptyBorder(15, 20, 15, 20));
            
            JLabel title = new JLabel("Admin Control Panel");
            title.setForeground(Color.WHITE);
            title.setFont(SUBTITLE_FONT);
            
            JButton logout = createBtn("Logout", RED_ACCENT);
            logout.addActionListener(e -> cardLayout.show(mainPanel, "Login"));

            JPanel rightTopBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightTopBtns.setOpaque(false);
            rightTopBtns.add(logout);
            
            top.add(title, BorderLayout.WEST);
            top.add(rightTopBtns, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            // Center Content (Fleet Table)
            String[] cols = {"ID", "Brand", "Model", "Price/Day", "Status", "Renter ID"};
            fleetModel = new DefaultTableModel(cols, 0);
            fleetTable = new JTable(fleetModel);
            fleetTable.setFont(BODY_FONT);
            fleetTable.getTableHeader().setFont(BODY_FONT);
            fleetTable.setRowHeight(26);
            // Double-click row to view renter details
            fleetTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && fleetTable.getSelectedRow() != -1) {
                        showRenterDetails();
                    }
                }
            });
            add(new JScrollPane(fleetTable), BorderLayout.CENTER);

            // Bottom Panel (Add Car)
            JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
            bot.setBackground(OFF_WHITE);
            
            brandF = new JTextField(10);
            modelF = new JTextField(10);
            priceF = new JTextField(8);
            
            bot.add(new JLabel("Brand:")); bot.add(brandF);
            bot.add(new JLabel("Model:")); bot.add(modelF);
            bot.add(new JLabel("Price:")); bot.add(priceF);
            
            JButton addBtn = createBtn("Add Vehicle", GREEN_ACCENT);
            addBtn.addActionListener(e -> addNewCar());
            JButton removeBtn = createBtn("Remove Selected", DARK_BLUE);
            removeBtn.addActionListener(e -> removeSelectedCar());
            JButton viewRenterBtn = createBtn("View Renter Details", BRIGHT_BLUE);
            viewRenterBtn.addActionListener(e -> showRenterDetails());
            bot.add(addBtn);
            bot.add(removeBtn);
            bot.add(viewRenterBtn);
            
            add(bot, BorderLayout.SOUTH);
            
            // Refresh timer (Simulated live update)
            addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentShown(java.awt.event.ComponentEvent evt) { refreshTable(); }
            });
        }

        private void refreshTable() {
            fleetModel.setRowCount(0);
            for (Car c : RentalService.fleet) {
                String status = c.isAvailable() ? "Available" : "Rented";
                String renter = c.getCurrentRenterId() == null ? "-" : c.getCurrentRenterId();
                fleetModel.addRow(new Object[]{c.getCarId(), c.getBrand(), c.getModel(), "$" + c.getPrice(), status, renter});
            }
        }

        private void addNewCar() {
            try {
                String b = brandF.getText();
                String m = modelF.getText();
                double p = Double.parseDouble(priceF.getText());
                RentalService.addCar(b, m, p);
                refreshTable();
                brandF.setText(""); modelF.setText(""); priceF.setText("");
                JOptionPane.showMessageDialog(this, "Vehicle added to fleet.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Please provide valid brand, model and price.");
            }
        }

        private void removeSelectedCar() {
            int row = fleetTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a vehicle to remove.");
                return;
            }
            String carId = (String) fleetModel.getValueAt(row, 0);
            Car selected = RentalService.fleet.stream()
                    .filter(c -> c.getCarId().equals(carId))
                    .findFirst().orElse(null);
            if (selected == null) return;

            if (!selected.isAvailable()) {
                JOptionPane.showMessageDialog(this, "You can’t remove a car that is currently rented.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove this vehicle from the fleet?",
                    "Confirm Removal",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                RentalService.fleet.remove(selected);
                refreshTable();
            }
        }

        private void showRenterDetails() {
            int row = fleetTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a rented vehicle first.");
                return;
            }
            String status = (String) fleetModel.getValueAt(row, 4);
            String renterId = (String) fleetModel.getValueAt(row, 5);

            if (!"Rented".equalsIgnoreCase(status) || renterId == null || renterId.equals("-")) {
                JOptionPane.showMessageDialog(this, "The selected vehicle is not currently rented.");
                return;
            }

            User found = null;
            for (User u : RentalService.users) {
                if (u.getId().equals(renterId)) {
                    found = u;
                    break;
                }
            }

            if (found == null || !(found instanceof Customer)) {
                JOptionPane.showMessageDialog(this, "No customer details found for this renter ID.");
                return;
            }

            Customer c = (Customer) found;
            StringBuilder sb = new StringBuilder();
            sb.append("Renter ID: ").append(c.getId()).append("\n");
            sb.append("Name: ").append(c.getName()).append("\n");
            sb.append("Contact: ").append(c.getContact()).append("\n");
            sb.append("Email: ").append(c.getEmail()).append("\n");

            JOptionPane.showMessageDialog(this, sb.toString(), "Renter Details", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // --- SCREEN 3: CUSTOMER DASHBOARD ---
    class CustomerDashboard extends JPanel {
        DefaultTableModel availModel, myRentalsModel;
        JTabbedPane tabs;
        JLabel title;

        public CustomerDashboard() {
            setLayout(new BorderLayout());

            // Header
            JPanel top = new JPanel(new BorderLayout());
            top.setBackground(BRIGHT_BLUE);
            top.setBorder(new EmptyBorder(15, 20, 15, 20));
            
            title = new JLabel("Welcome, Valued Customer");
            title.setForeground(Color.WHITE);
            title.setFont(SUBTITLE_FONT);
            
            JButton logout = createBtn("Logout", RED_ACCENT);
            logout.addActionListener(e -> cardLayout.show(mainPanel, "Login"));
            
            top.add(title, BorderLayout.WEST);
            top.add(logout, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);

            // Tabs
            tabs = new JTabbedPane();
            
            // Tab 1: Available Cars
            JPanel rentPanel = new JPanel(new BorderLayout());
            availModel = new DefaultTableModel(new String[]{"ID", "Brand", "Model", "Price"}, 0);
            JTable rentTable = new JTable(availModel);
            rentTable.setFont(BODY_FONT);
            rentTable.getTableHeader().setFont(BODY_FONT);
            rentTable.setRowHeight(26);
            
            JButton rentBtn = createBtn("Rent Selected Car", GREEN_ACCENT);
            rentBtn.addActionListener(e -> rentAction(rentTable));
            
            rentPanel.add(new JScrollPane(rentTable), BorderLayout.CENTER);
            rentPanel.add(rentBtn, BorderLayout.SOUTH);

            // Tab 2: My Rentals
            JPanel myPanel = new JPanel(new BorderLayout());
            myRentalsModel = new DefaultTableModel(new String[]{"ID", "Brand", "Model", "Price"}, 0);
            JTable myTable = new JTable(myRentalsModel);
            myTable.setFont(BODY_FONT);
            myTable.getTableHeader().setFont(BODY_FONT);
            myTable.setRowHeight(26);
            
            JButton returnBtn = createBtn("Return Car", DARK_BLUE);
            returnBtn.addActionListener(e -> returnAction(myTable));
            
            myPanel.add(new JScrollPane(myTable), BorderLayout.CENTER);
            myPanel.add(returnBtn, BorderLayout.SOUTH);

            tabs.addTab("Rent a Car", rentPanel);
            tabs.addTab("My Rentals", myPanel);
            
            tabs.addChangeListener(e -> refreshData());
            add(tabs, BorderLayout.CENTER);
            
            addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentShown(java.awt.event.ComponentEvent evt) { refreshData(); }
            });
        }

        private void refreshData() {
            if (RentalService.currentUser != null) {
                title.setText("Welcome, " + RentalService.currentUser.getName());
            }
            // Populate Available Cars
            availModel.setRowCount(0);
            for (Car c : RentalService.fleet) {
                if (c.isAvailable()) {
                    availModel.addRow(new Object[]{c.getCarId(), c.getBrand(), c.getModel(), c.getPrice()});
                }
            }

            // Populate My Rentals
            myRentalsModel.setRowCount(0);
            if (RentalService.currentUser != null) {
                for (Car c : RentalService.fleet) {
                    if (!c.isAvailable() && c.getCurrentRenterId().equals(RentalService.currentUser.getId())) {
                        myRentalsModel.addRow(new Object[]{c.getCarId(), c.getBrand(), c.getModel(), c.getPrice()});
                    }
                }
            }
        }

        private void rentAction(JTable table) {
            int row = table.getSelectedRow();
            if (row == -1) return;
            
            String carId = (String) availModel.getValueAt(row, 0);
            Car selectedCar = RentalService.fleet.stream().filter(c -> c.getCarId().equals(carId)).findFirst().orElse(null);
            
            if (selectedCar != null) {
                selectedCar.rent(RentalService.currentUser.getId());
                JOptionPane.showMessageDialog(this, "Car Rented Successfully!");
                refreshData();
            }
        }

        private void returnAction(JTable table) {
            int row = table.getSelectedRow();
            if (row == -1) return;
            
            String carId = (String) myRentalsModel.getValueAt(row, 0);
            Car selectedCar = RentalService.fleet.stream().filter(c -> c.getCarId().equals(carId)).findFirst().orElse(null);
            
            if (selectedCar != null) {
                selectedCar.returnCar();
                JOptionPane.showMessageDialog(this, "Car Returned. Thank you!");
                refreshData();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CarRentalSystem().setVisible(true));
    }
}