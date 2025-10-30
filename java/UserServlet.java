import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());
    private static final Pattern USER_ID_PATTERN = Pattern.compile("\\d+");

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = request.getParameter("userId");  // User input from the URL

        response.setContentType("text/html");

        try (PrintWriter out = response.getWriter()) {
            // Server-side validation: ensure userId is numeric to avoid SQL injection attempts
            if (userId == null || !USER_ID_PATTERN.matcher(userId).matches()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Invalid userId parameter");
                return;
            }

            // Use try-with-resources to ensure DB resources are closed reliably
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydatabase", "user", "password");
                 PreparedStatement pstmt = conn.prepareStatement("SELECT id, name FROM users WHERE id = ?")) {

                // Bind the parameter safely as an integer
                pstmt.setInt(1, Integer.parseInt(userId));

                try (ResultSet rs = pstmt.executeQuery()) {
                    boolean found = false;
                    while (rs.next()) {
                        found = true;
                        out.println("User ID: " + rs.getInt("id"));
                        out.println("User Name: " + rs.getString("name"));
                    }

                    if (!found) {
                        out.println("No user found with the provided id.");
                    }
                }

            } catch (SQLException e) {
                // Log the detailed error on the server, but return a safe message to the client
                LOGGER.log(Level.SEVERE, "Database error while fetching user", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Internal server error");
            }
        }
    }
}
