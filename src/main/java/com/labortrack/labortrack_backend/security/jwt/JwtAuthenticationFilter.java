package com.labortrack.labortrack_backend.security.jwt;

import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates http requests that contains a bearer JWT

 * During login, the user submits email and password and
 * authentication manager verifies those credentials.

 * After login, requests do not send email and password
 * again to authenticationManager. Instead, each requests
 * carries client JWT in the authorization header. Then
 * This filter validates the token, load the current user,
 * and creates an authenticated Authentication object and
 * stores it in the SecurityContext.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final LaborTrackUserDetailsService userDetailsService;
    public JwtAuthenticationFilter(JwtService jwtService, LaborTrackUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * This method examines one http request for a JWT and authenticates the
     * requests when the token is valid.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // get authorization header from request
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        // When the bearer is not valid, simply leave the requests
        // unauthenticated and continue through the filterchain.
        if(authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // remove bearer and only keep JWT
        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        try {
            // Do not replace an Authentication object that was already
            // established earlier in the security filter chain.
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // extract the email from the token
                String email = jwtService.extractUsernameFromToken(token);

                // load the user from db using the subject
                LaborTrackUserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // confirm the token is valid and belong to this user
                if (jwtService.isTokenValid(token, userDetails)) {
                    //Create an authenticated Spring Security object.
                    // credentials stays null because not need for password
                    // after authentication has been already succeed.
                    UsernamePasswordAuthenticationToken
                            authentication = UsernamePasswordAuthenticationToken.authenticated(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    // attach request information such as the remote address.
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    // set the authenticated object onto security context
                    SecurityContext securityContext =
                            SecurityContextHolder.createEmptyContext();
                    securityContext.setAuthentication(authentication);
                    SecurityContextHolder.setContext(securityContext);
                }
            }
        }
        //expired, malformed, or belongs to a user that no longer exists.
        // if any exception gets raise the authentication gets invalid and user must stay unauthenticated and unauthorized to enter.
        catch (
                JwtException
                | UsernameNotFoundException
                | IllegalArgumentException exception
        ) {
            // requests stays unauthenticated.
            // A protected endpoint will later return 401 Unauthorized.
        }
        // continue to the next filter or controller
        filterChain.doFilter(request, response);
    }
}
