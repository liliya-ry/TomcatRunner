package utility;

import com.google.gson.Gson;
import org.apache.logging.log4j.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ResponseHandler {
    private static final String CONTENT_TYPE = "application/json";

    private final Gson gson;
    private final Logger logger;

    public ResponseHandler(Gson gson) {
        this.gson = gson;
        this.logger = LogManager.getLogger("BlogApiLogger");
    }

    public void sendAsJson(HttpServletResponse response, Object obj) throws IOException {
        response.setContentType(CONTENT_TYPE);
        String json = gson.toJson(obj);
        try (PrintWriter out = response.getWriter()) {
            out.print(json);
        }
    }

    public void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        ErrorResponse error = new ErrorResponse(status, message);
        sendAsJson(response, error);
        logger.error("{} : {}", error.code, error.message);
    }
}
