package atm;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class DBConnection {
private static final String URL =
&quot;jdbc:mysql://localhost:3306/atm_db&quot;;
private static final String USER = &quot;root&quot;;
// Replace with your own MySQL password
private static final String PASSWORD = &quot;your_password&quot;;
public static Connection getConnection()
throws SQLException {
return DriverManager.getConnection(
URL, USER, PASSWORD);
}
}
