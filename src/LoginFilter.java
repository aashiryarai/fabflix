import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;

@WebFilter(filterName = "LoginFilter", urlPatterns = {"/api/*", "/logout", "/dashboard.html"})
public class LoginFilter implements Filter {
    private final ArrayList<String> allowedURIs = new ArrayList<>();

    public void init(FilterConfig fConfig) {
        allowedURIs.add("login.html");
        allowedURIs.add("dashboard-login.html");
        allowedURIs.add("api/login");
        allowedURIs.add("api/_dashboard");
        allowedURIs.add("index.html");
        allowedURIs.add("index.js");
        allowedURIs.add("styles.css");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        HttpSession session = httpRequest.getSession(false);
        boolean isCustomer = session != null && session.getAttribute("user") != null;
        boolean isEmployee = session != null && session.getAttribute("employee") != null;

        System.out.println("LoginFilter: " + requestURI);

        if (isUrlAllowedWithoutLogin(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // Require employee login for dashboard/admin APIs
        if (requestURI.contains("dashboard.html")
                || requestURI.contains("api/add-star")
                || requestURI.contains("api/add-movie")
                || requestURI.contains("api/metadata")
                || requestURI.contains("_dashboard")) {
            if (!isEmployee) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/dashboard-login.html");
                return;
            }
        }

        // Require at least some login for other protected routes
        if (!isCustomer && !isEmployee) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.html");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isUrlAllowedWithoutLogin(String requestURI) {
        return allowedURIs.stream().anyMatch(requestURI.toLowerCase()::endsWith);
    }

    public void destroy() {
        // No cleanup needed
    }
}
