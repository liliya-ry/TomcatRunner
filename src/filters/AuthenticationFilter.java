package filters;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.*;
import utility.ResponseHandler;

import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(servletNames = {"PostServlet", "CommentsServlet"})
public class AuthenticationFilter implements Filter {
    private ResponseHandler responseHandler;

    @Override
    public void init(FilterConfig fConfig) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        responseHandler = new ResponseHandler(gson);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        if (session == null) {
            responseHandler.sendError(resp, SC_UNAUTHORIZED, "Bad request: unauthorised user");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
