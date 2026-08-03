package com.labortrack.labortrack_backend.security.config;

import com.labortrack.labortrack_backend.security.filter.PasswordChangeRequiredFilter;
import com.labortrack.labortrack_backend.security.handler.RestAccessDeniedHandler;
import com.labortrack.labortrack_backend.security.handler.RestAuthenticationEntryPoint;
import com.labortrack.labortrack_backend.security.jwt.JwtAuthenticationFilter;
import com.labortrack.labortrack_backend.security.jwt.JwtProperties;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetailsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * WHEN LOGIN - AUTHENTICATION MANAGER
 * This class creates the authentication object used by spring security.
 * It holds our custom userDetails service with the password encoder.
 * and provides authentication manager that can verify and authenticate
 * a user's login information.
 *
 * PER REQUEST - JWT FILTER AND HTTP SECURITY.
 * After login the user no longer sends password with every request.
 * Instead, to authenticate we use JWT token to allows accesses to
 * certain endpoints when authenticated so this class add setup http security.
 * securityFilterChain checks wheather the user has permission to certain
 * endpoints even after authenticated.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * This bean creates the object used to authenticate when user login.
     * This bean creates an object that holds both our custom UserDetails
     * service and password encoder used to verify and authenticate user
     * login information.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            LaborTrackUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * This bean creates the AuthenticationManager which uses
     * which receives the request from login's and uses the
     * authentication provider to verify those requests and
     * authenticate.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider authenticationProvider
    ) {
        return new ProviderManager(
                List.of(authenticationProvider)
        );
    }

    /**
     * This method simply set http security rules. The API will have
     * STATELESS session, which mean session must not remember user
     * information. Company registration and user login would be public
     * endpoints, which means they don't required authentication. But any
     * other requests would need authentication to access the other API's
     * endpoints.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            PasswordChangeRequiredFilter passwordChangeRequiredFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler)
            throws Exception{

        http.
                // JWT authentication is STATELESS. Http session must not remember
                // user information
                sessionManagement(
                        session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Disabling csrf, form login, http basic, and logout. LaborTrack does not need use them.
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                // Return custom dto JSON error response for security failures
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // Defining endpoint protections. For each request:
                .authorizeHttpRequests(
                        authorizeRequests -> authorizeRequests
                                // avoid need of authentication when registering a company (PUBLIC)
                                // a new company needs to be able to registers before jwt/authentication
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/companies/register"
                                ).permitAll()

                                // avoid need of authentication when login (PUBLIC)
                                // users must be able to submit credentials to received jwt/authentication
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/auth/login"
                                ).permitAll()

                                // only admin users can create employees
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/companies/*/employees"
                                ).hasRole("ADMIN")

                                // only employees can perform clock-in and clock-out
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/employees/*/clock-in"
                                ).hasRole("EMPLOYEE")
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/employees/*/clock-out"
                                ).hasRole("EMPLOYEE")

                                // both roles can request work-session history
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/employees/*/work-sessions"
                                ).hasAnyRole("ADMIN", "EMPLOYEE")

                                // any other request required authentication first before accessing (PROTECTED)
                                .anyRequest().authenticated()

                )

                // check jwt (jwt filter) before username and password filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // add passwordChange filter after JWT filter
                .addFilterAfter(
                        passwordChangeRequiredFilter,
                        JwtAuthenticationFilter.class
                );
        return http.build();
    }

}
