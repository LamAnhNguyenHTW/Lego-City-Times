package LegoCity.content_service.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 30, message = "Benutzername muss 3–30 Zeichen lang sein")
    private String username;

    @NotBlank
    @Email(message = "Keine gültige E-Mail-Adresse")
    private String email;

    @NotBlank
    @Size(min = 8, message = "Passwort muss mindestens 8 Zeichen lang sein")
    private String password;
}
