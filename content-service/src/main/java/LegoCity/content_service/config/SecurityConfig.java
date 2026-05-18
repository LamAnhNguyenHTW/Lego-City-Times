package LegoCity.content_service.config;

import LegoCity.content_service.security.JwtAccessDeniedHandler;
import LegoCity.content_service.security.JwtAuthEntryPoint;
import LegoCity.content_service.security.JwtAuthFilter;
import LegoCity.content_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthEntryPoint authEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // Auth-Endpunkte: immer öffentlich
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Öffentliche Lesezugriffe
                .requestMatchers(HttpMethod.GET, "/api/v1/articles/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tags/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/images/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                // API-Docs & Monitoring
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()

                // ── ADMIN-only Schreibzugriffe ──────────────────────────────
                .requestMatchers(HttpMethod.POST,   "/api/v1/articles").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/articles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/articles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/articles/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST,   "/api/v1/categories").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST,   "/api/v1/tags").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/tags/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/tags/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST,   "/api/v1/images/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/images/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/images/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(authService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
