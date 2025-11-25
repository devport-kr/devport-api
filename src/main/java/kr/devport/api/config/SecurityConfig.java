package kr.devport.api.config;

import kr.devport.api.security.JwtAuthenticationFilter;
import kr.devport.api.security.oauth2.CustomOAuth2UserService;
import kr.devport.api.security.oauth2.OAuth2AuthenticationFailureHandler;
import kr.devport.api.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(authorize -> authorize
                // H2 Console (development only)
                .requestMatchers("/h2-console", "/h2-console/**").permitAll()
                // Public endpoints
                .requestMatchers(
                    "/",
                    "/error",
                    "/favicon.ico",
                    "/api/articles/**",
                    "/api/llm-rankings",
                    "/api/benchmarks"
                ).permitAll()
                // Swagger/OpenAPI endpoints
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // OAuth2 endpoints
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            )
            // Add JWT authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // H2 console (development only)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())
            );

        return http.build();
    }
}
