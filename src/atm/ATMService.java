package atm;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ATMService {

    public String login(String account, String pin)
            throws SQLException {

        String sql = "SELECT holder_name FROM accounts "
                   + "WHERE account_number = ? AND pin = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, account);
            ps.setString(2, pin);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("holder_name");
                }
            }
        }

        return null;
    }

    public BigDecimal getBalance(String account)
            throws SQLException {

        String sql = "SELECT balance FROM accounts "
                   + "WHERE account_number = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, account);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
            }
        }

        throw new SQLException("Account not found.");
    }

    public void deposit(String account, BigDecimal amount)
            throws SQLException {

        validateAmount(amount);

        String update = "UPDATE accounts SET balance = "
                      + "balance + ? WHERE account_number = ?";

        String insert = "INSERT INTO transactions "
                      + "(account_number, transaction_type, amount) "
                      + "VALUES (?, 'DEPOSIT', ?)";

        executeMoneyTransaction(
                account, amount, update, insert, false);
    }

    public void withdraw(String account, BigDecimal amount)
            throws SQLException {

        validateAmount(amount);

        String update = "UPDATE accounts SET balance = "
                      + "balance - ? WHERE account_number = ? "
                      + "AND balance >= ?";

        String insert = "INSERT INTO transactions "
                      + "(account_number, transaction_type, amount) "
                      + "VALUES (?, 'WITHDRAWAL', ?)";

        executeMoneyTransaction(
                account, amount, update, insert, true);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Enter an amount greater than zero "
                    + "with at most two decimal places.");
        }
    }

    private void executeMoneyTransaction(
            String account, BigDecimal amount,
            String update, String insert, boolean withdrawal)
            throws SQLException {

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {
                // Lock the account row to prevent concurrent
                // balance changes during this operation.
                String lock = "SELECT balance FROM accounts "
                            + "WHERE account_number = ? FOR UPDATE";

                try (PreparedStatement ps =
                             con.prepareStatement(lock)) {

                    ps.setString(1, account);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Account not found.");
                        }

                        BigDecimal balance =
                                rs.getBigDecimal("balance");

                        if (withdrawal
                                && balance.compareTo(amount) < 0) {
                            throw new SQLException(
                                    "Insufficient balance.");
                        }
                    }
                }

                try (PreparedStatement ps =
                             con.prepareStatement(update)) {

                    ps.setBigDecimal(1, amount);
                    ps.setString(2, account);

                    if (withdrawal) {
                        ps.setBigDecimal(3, amount);
                    }

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Balance update failed.");
                    }
                }

                try (PreparedStatement ps =
                             con.prepareStatement(insert)) {

                    ps.setString(1, account);
                    ps.setBigDecimal(2, amount);
                    ps.executeUpdate();
                }

                con.commit();

            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<String[]> getTransactions(String account)
            throws SQLException {

        List<String[]> list = new ArrayList<>();

        String sql = "SELECT transaction_id, transaction_type, "
                   + "amount, transaction_date FROM transactions "
                   + "WHERE account_number = ? "
                   + "ORDER BY transaction_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, account);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[] {
                        String.valueOf(
                                rs.getInt("transaction_id")),
                        rs.getString("transaction_type"),
                        rs.getBigDecimal("amount").toPlainString(),
                        rs.getTimestamp(
                                "transaction_date").toString()
                    });
                }
            }
        }

        return list;
    }
}
