package lost_and_found.app.auth.infra;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lost_and_found.app.auth.dto.LoginResponseDTO;
import lost_and_found.app.auth.service.OAuth2AuthenticationService;

 /**
  * The handler of a successful oauth2 authentication. 
  * This being called means that all necessary user info has been delivered to our application.
  */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthenticationService authenticationService;
    private final String frontendUrl;

    public OAuth2SuccessHandler(
        OAuth2AuthenticationService authenticationService,
        @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.authenticationService = authenticationService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User user = (OAuth2User)authentication.getPrincipal();

        String registrationId = token.getAuthorizedClientRegistrationId();

        ResponseEntity<LoginResponseDTO> loginResponse = authenticationService.handleOAuth2Login(user, registrationId);
        
        List<String> setCookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies != null) {
            for (String c : setCookies) {
                response.addHeader(HttpHeaders.SET_COOKIE, c);
            }
        }

        response.sendRedirect(frontendUrl.replaceAll("/$", "") + "/");
    }

}
