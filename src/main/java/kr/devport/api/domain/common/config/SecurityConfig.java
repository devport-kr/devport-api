package kr.devport.api.domain.common.config;

import kr.devport.api.domain.common.security.JwtAuthenticationFilter;
import kr.devport.api.domain.auth.oauth2.CustomOAuth2AuthorizationRequestResolver;
import kr.devport.api.domain.auth.oauth2.CustomOAuth2UserService;
import kr.devport.api.domain.auth.oauth2.OAuth2AuthenticationFailureHandler;
import kr.devport.api.domain.auth.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/h2-console", "/h2-console/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/webhooks/crawler/**").permitAll()
                .requestMatchers(
                    "/",
                    "/error",
                    "/favicon.ico",
                    "/api/git-repos/**",
                    "/api/llm/**",
                    "/api/llm-rankings",
                    "/api/benchmarks",
                    "/api/auth/refresh",
                    "/api/auth/signup",
                    "/api/auth/login",
                    "/api/auth/verify-email",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/auth/check-username",
                    "/api/auth/check-email"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/articles/*/view").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/articles/*/comments").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/articles/*/comments/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/articles/*/comments/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/ports/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/projects/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/projects/*/comments").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/projects/*/comments/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/projects/*/comments/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/projects/*/comments/*/vote").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/wiki/projects/*/chat").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/wiki/projects/*/chat/sessions/*").authenticated()
                .requestMatchers(HttpMethod.GET,
                    "/api/wiki/admin/projects/*/drafts",
                    "/api/wiki/admin/projects/*/drafts/*"
                ).hasAnyRole("ADMIN", "EDITOR")
                .requestMatchers(HttpMethod.POST,
                    "/api/wiki/admin/projects/*/drafts",
                    "/api/wiki/admin/projects/*/drafts/*/regenerate",
                    "/api/wiki/admin/projects/*/publish",
                    "/api/wiki/admin/projects/*/rollback"
                ).hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,
                    "/api/wiki/admin/projects/*/drafts/*"
                ).hasRole("ADMIN")
                .requestMatchers("/api/wiki/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/wiki/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                .requestMatchers("/api/me/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        String message = request.getRequestURI().contains("/api/wiki/projects/")
                                && request.getRequestURI().contains("/chat")
                                ? "챗봇과 대화할려면 로그인하세요"
                                : "로그인이 필요합니다";
                        response.getWriter().write("{\"message\":\"" + message + "\"}");
                        return;
                    }
                    response.sendRedirect("/login");
                })
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(
                        new CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository)
                    )
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())
            );

        return http.build();
    }
}
