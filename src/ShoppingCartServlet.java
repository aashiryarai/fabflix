import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import model.CartItem;
import com.google.gson.Gson;
import java.io.PrintWriter;

@WebServlet("/api/cart")
public class ShoppingCartServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        Map<String, CartItem> cart = (Map<String, CartItem>) session.getAttribute("cart");

        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        String movieId = request.getParameter("movieId");
        String title = request.getParameter("title"); // may be null
        String action = request.getParameter("action");

        if (movieId == null || action == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters.");
            return;
        }

        switch (action) {
            case "add":
                if (title == null) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing title for add action.");
                    return;
                }
                CartItem addItem = cart.get(movieId);
                if (addItem == null) {
                    double price = 5.0 + new Random().nextInt(11); // Random price 5–15
                    cart.put(movieId, new CartItem(movieId, title, price, 1));
                } else {
                    addItem.setQuantity(addItem.getQuantity() + 1);
                }
                break;
            case "increase":
                if (cart.containsKey(movieId)) {
                    cart.get(movieId).incrementQuantity();
                }
                break;
            case "decrease":
                if (cart.containsKey(movieId)) {
                    CartItem item = cart.get(movieId);
                    item.decrementQuantity();
                    if (item.getQuantity() <= 0) {
                        cart.remove(movieId);
                    }
                }
                break;
            case "delete":
                cart.remove(movieId);
                break;
            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action.");
                return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        Map<String, CartItem> cart = (Map<String, CartItem>) session.getAttribute("cart");

        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.write(new Gson().toJson(cart.values()));
        out.close();
    }
}
