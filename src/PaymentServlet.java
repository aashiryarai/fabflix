package servlets;

import model.CartItem;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

@WebServlet("/api/payment")
public class PaymentServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/masterdb");
        } catch (NamingException e) {
            throw new ServletException("Unable to retrieve DataSource", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String cardNumber = request.getParameter("cardNumber");
        String expDate = request.getParameter("expDate");

        try (Connection conn = dataSource.getConnection()) {
            // Check if card exists
            String query = "SELECT id FROM creditcards WHERE id = ? AND firstName = ? AND lastName = ? AND expiration = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, cardNumber);
                ps.setString(2, firstName);
                ps.setString(3, lastName);
                ps.setDate(4, Date.valueOf(expDate));

                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }

            // Get customerId from session
            HttpSession session = request.getSession();
            Integer customerId = (Integer) session.getAttribute("userId");
            if (customerId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // Get cart from session
            Map<String, CartItem> cart = (Map<String, CartItem>) session.getAttribute("cart");
            if (cart == null || cart.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Insert sales for each item in cart
            String insertSale = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, CURDATE())";
            try (PreparedStatement saleStmt = conn.prepareStatement(insertSale)) {
                for (CartItem item : cart.values()) {
                    for (int i = 0; i < item.getQuantity(); i++) {
                        saleStmt.setInt(1, customerId);
                        saleStmt.setString(2, item.getMovieId());
                        saleStmt.executeUpdate();
                    }
                }
            }

            // Clear cart
            cart.clear();
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
