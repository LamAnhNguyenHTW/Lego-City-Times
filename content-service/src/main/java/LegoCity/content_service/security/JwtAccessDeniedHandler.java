package LegoCity.content_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        writeJson(response, 403, "Forbidden",
                "Keine Berechtigung — ADMIN-Rolle erforderlich",
                request.getRequestURI());
    }

    private void writeJson(HttpServletResponse response, int status, String error,
                            String message, String path) throws IOException {
        response.getWriter().write(
                "{\"status\":" + status +
                ",\"error\":\"" + error + "\"" +
                ",\"message\":\"" + message + "\"" +
                ",\"timestamp\":\"" + LocalDateTime.now() + "\"" +
                ",\"path\":\"" + path + "\"}"
        );
    }
}
