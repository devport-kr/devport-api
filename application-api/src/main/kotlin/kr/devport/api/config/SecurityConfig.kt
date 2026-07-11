package kr.devport.api.config

import kr.devport.api.domain.auth.oauth2.CustomOAuth2AuthorizationRequestResolver
import kr.devport.api.domain.auth.oauth2.CustomOAuth2UserService
import kr.devport.api.domain.auth.oauth2.OAuth2AuthenticationFailureHandler
import kr.devport.api.domain.auth.oauth2.OAuth2AuthenticationSuccessHandler
import kr.devport.api.domain.auth.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val clientRegistrationRepository: ClientRegistrationRepository,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(10)

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager = authConfig.authenticationManager

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors(Customizer.withDefaults())
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests { authorize ->
                authorize
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
                        "/api/auth/oauth2/exchange",
                        "/api/auth/verify-email",
                        "/api/auth/resend-verification",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/articles/*/view").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/articles/*/comments").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/articles/*/comments/*").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/articles/*/comments/*").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/projects/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/projects/*/comments").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/projects/*/comments/*").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/projects/*/comments/*").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/projects/*/comments/*/vote").authenticated()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/wiki/projects/*/chat",
                        "/api/wiki/projects/*/chat/stream",
                        "/api/wiki/projects/chat",
                        "/api/wiki/projects/chat/stream",
                        "/api/wiki/chat",
                        "/api/wiki/chat/stream",
                    ).permitAll()
                    .requestMatchers("/api/wiki/sessions/**").authenticated()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/wiki/admin/projects/*/drafts",
                        "/api/wiki/admin/projects/*/drafts/*",
                    ).hasAnyRole("ADMIN", "EDITOR")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/wiki/admin/projects/*/drafts",
                        "/api/wiki/admin/projects/*/drafts/*/regenerate",
                        "/api/wiki/admin/projects/*/publish",
                        "/api/wiki/admin/projects/*/rollback",
                    ).hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/wiki/admin/projects/*/drafts/*").hasRole("ADMIN")
                    .requestMatchers("/api/wiki/admin/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/wiki/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/oauth2/**", "/login/**").permitAll()
                    .requestMatchers("/api/me/**").authenticated()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }.exceptionHandling { exception ->
                exception.authenticationEntryPoint { request, response, _ ->
                    if (request.requestURI.startsWith("/api/")) {
                        response.status = 401
                        response.contentType = "application/json;charset=UTF-8"
                        response.writer.write("{\"message\":\"로그인이 필요합니다\"}")
                    } else {
                        response.sendRedirect("/login")
                    }
                }
            }.oauth2Login { oauth2 ->
                oauth2
                    .authorizationEndpoint { authorization ->
                        authorization.authorizationRequestResolver(
                            CustomOAuth2AuthorizationRequestResolver(clientRegistrationRepository),
                        )
                    }.userInfoEndpoint { userInfo -> userInfo.userService(customOAuth2UserService) }
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler)
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .headers { headers -> headers.frameOptions { it.disable() } }

        return http.build()
    }
}
