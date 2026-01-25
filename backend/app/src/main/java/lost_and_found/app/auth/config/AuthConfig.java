package lost_and_found.app.auth.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lost_and_found.app.auth.infra.OAuth2SuccessHandler;
import lost_and_found.app.shared.util.Csv;

@Configuration
@EnableWebSecurity
@Slf4j
public class AuthConfig {

    @Value("${hostname.prod}")
    private String prodHostname;

    @Value("${hostname.dev}")
    private String devHostname;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /** Origins of the native app's web view, e.g. capacitor://localhost. Comma-separated. */
    @Value("${app.native-origins:}")
    private String nativeOrigins;

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public AuthConfig(
        AuthenticationProvider authenticationProvider, 
        JwtAuthFilter jwtAuthFilter,
        OAuth2SuccessHandler oAuth2SuccessHandler
    ) {
        this.authenticationProvider = authenticationProvider;
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/auth/**",
                    "/items/public/**",
                    "/error",
                    "/oauth2/**",
                    "/login/oauth2/**").permitAll()
                .requestMatchers(
                    HttpMethod.GET, 
                    "/items/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
            )
            .sessionManagement(
                session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .oauth2Login(oAuth2 -> oAuth2
                .failureHandler(
                    (req, res, exception) -> {
                        // Fires when the user cancels the Google consent screen (error=access_denied)
                        // or Google/our token exchange fails. Send them back to the login page
                        // instead of leaving an empty response.
                        log.warn("Google sign-in did not complete: {}", exception.getMessage());
                        res.sendRedirect(frontendUrl.replaceAll("/$", "") + "/login?oauthError=google");
                    }
                )
                .successHandler(oAuth2SuccessHandler)
                .loginPage("/oauth2/authorization/google")
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        List<String> allowedOrigins = new ArrayList<>(List.of(prodHostname, devHostname));
        allowedOrigins.addAll(Csv.split(nativeOrigins));
        corsConfiguration.setAllowedOrigins(allowedOrigins);
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        corsConfiguration.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "Accept", "Origin",
            "X-Requested-With", "Access-Control-Request-Method", "Access-Control-Request-Headers"
        ));
        corsConfiguration.setExposedHeaders(List.of("Authorization"));
        corsConfiguration.setAllowCredentials(true);
        configurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return configurationSource;
    }
}
