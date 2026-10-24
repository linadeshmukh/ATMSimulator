package atm;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class ATMGUI extends JFrame {

    private final ATMService service = new ATMService();

    private String accountNumber;
    private String holderName;

    private final Color navy = new Color(24, 42, 75);
    private final Color gold = new Color(220, 175, 70);

    public ATMGUI() {
        showLogin();
    }

    private void setupWindow(String title) {
        setTitle(title);
        setSize(550, 430);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);
    }

    private JButton makeButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(navy);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        return button;
    }

    private void showLogin() {
        setupWindow("ATM Simulation - Login");

        JPanel panel = new JPanel(new GridLayout(0, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(
                35, 65, 35, 65));
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel(
                "ATM SIMULATION", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 25));
        title.setForeground(navy);

        JTextField accountField = new JTextField();
        JPasswordField pinField = new JPasswordField();

        panel.add(title);
        panel.add(new JLabel("Account Number"));
        panel.add(accountField);
        panel.add(new JLabel("4-digit PIN"));
        panel.add(pinField);

        JButton loginButton = makeButton("LOGIN");
        panel.add(loginButton);

        loginButton.addActionListener(e -> {
            String account = accountField.getText().trim();
            String pin = new String(pinField.getPassword());

            if (!pin.matches("\\d{4}")) {
                JOptionPane.showMessageDialog(
                        this, "Enter a 4-digit PIN.");
                return;
            }

            try {
                String name = service.login(account, pin);

                if (name == null) {
                    JOptionPane.showMessageDialog(
                            this, "Invalid account or PIN.");
                } else {
                    accountNumber = account;
                    holderName = name;
                    showDashboard();
                }
            } catch (SQLException ex) {
                showError(ex);
            }
        });

        setContentPane(panel);
        setVisible(true);
    }

    private void showDashboard() {
        setupWindow("ATM Simulation - Dashboard");

        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(
                25, 35, 25, 35));
        panel.setBackground(Color.WHITE);

        JLabel heading = new JLabel(
                "Welcome, " + holderName,
                SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        heading.setForeground(navy);

        JLabel subtitle = new JLabel(
                "Account: " + accountNumber,
                SwingConstants.CENTER);

        JPanel top = new JPanel(new GridLayout(2, 1, 5, 5));
        top.setBackground(Color.WHITE);
        top.add(heading);
        top.add(subtitle);

        JPanel buttons = new JPanel(new GridLayout(3, 2, 12, 12));
        buttons.setBackground(Color.WHITE);

        JButton balance = makeButton("Check Balance");
        JButton deposit = makeButton("Deposit");
        JButton withdraw = makeButton("Withdraw");
        JButton history = makeButton("Transaction History");
        JButton refresh = makeButton("Refresh Balance");
        JButton logout = makeButton("Logout");

        buttons.add(balance);
        buttons.add(deposit);
        buttons.add(withdraw);
        buttons.add(history);
        buttons.add(refresh);
        buttons.add(logout);

        balance.addActionListener(e -> showBalance());
        deposit.addActionListener(e -> doMoneyOperation(true));
        withdraw.addActionListener(e -> doMoneyOperation(false));
        history.addActionListener(e -> showHistory());
        refresh.addActionListener(e -> showBalance());
        logout.addActionListener(e -> {
            accountNumber = null;
            holderName = null;
            showLogin();
        });

        JLabel footer = new JLabel(
                "Java Swing | JDBC | MySQL",
                SwingConstants.CENTER);
        footer.setForeground(navy);

        panel.add(top, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);

        setContentPane(panel);
        setVisible(true);
    }

    private void showBalance() {
        try {
            BigDecimal balance =
                    service.getBalance(accountNumber);

            JOptionPane.showMessageDialog(
                    this,
                    "Available Balance: ₹"
                            + balance.toPlainString(),
                    "Balance Inquiry",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void doMoneyOperation(boolean isDeposit) {
        String label = isDeposit
                ? "Enter deposit amount:"
                : "Enter withdrawal amount:";

        String input = JOptionPane.showInputDialog(this, label);

        if (input == null) {
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(input.trim());

            if (isDeposit) {
                service.deposit(accountNumber, amount);
            } else {
                service.withdraw(accountNumber, amount);
            }

            JOptionPane.showMessageDialog(
                    this,
                    (isDeposit ? "Deposit" : "Withdrawal")
                            + " successful.\nAmount: ₹"
                            + amount.toPlainString()
                            + "\nUpdated balance: ₹"
                            + service.getBalance(
                                    accountNumber).toPlainString());

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this, "Enter a valid numeric amount.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showHistory() {
        try {
            List<String[]> records =
                    service.getTransactions(accountNumber);

            String[] columns = {
                "ID", "Type", "Amount (₹)", "Date"
            };

            DefaultTableModel model =
                    new DefaultTableModel(columns, 0) {
                        @Override
                        public boolean isCellEditable(
                                int row, int column) {
                            return false;
                        }
                    };

            for (String[] record : records) {
                model.addRow(record);
            }

            JTable table = new JTable(model);
            table.setAutoResizeMode(
                    JTable.AUTO_RESIZE_ALL_COLUMNS);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(650, 250));

            JOptionPane.showMessageDialog(
                    this, scroll, "Transaction History",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                "Operation failed: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
