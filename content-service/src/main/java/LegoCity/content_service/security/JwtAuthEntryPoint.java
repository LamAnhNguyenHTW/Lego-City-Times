package LegoCity.content_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        writeJson(response, 401, "Unauthorized",
                "Authentifizierung erforderlich — Bearer Token fehlt oder ist ungültig",
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
