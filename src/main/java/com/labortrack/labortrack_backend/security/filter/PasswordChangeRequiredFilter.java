package com.labortrack.labortrack_backend.security.filter;

import com.labortrack.labortrack_backend.security.handler.RestAccessDeniedHandler;
import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This filter class simply prevents authenticated users with a temporary
 * password from accessing normal application endpoints until they change
 * their password and mustChangePassword set to false.
 */
@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private final RestAccessDeniedHandler accessDeniedHandler;
    public PasswordChangeRequiredFilter(RestAccessDeniedHandler accessDeniedHandler) {
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * This method allows requests to BYPASS this filter when
     * the request method is POST to /api/auth/change-password
     * or CORS preflight other than that filter the request.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean isOptionsRequest = HttpMethod.OPTIONS.matches(request.getMethod());

        boolean isPasswordChangeRequest =
                HttpMethod.POST.matches(request.getMethod())
                && request.getRequestURI()
                        .equals(request.getContextPath()
                                + "/api/auth/change-password");

        return isOptionsRequest || isPasswordChangeRequest;
    }

    /**
     * This filter simply check the incoming request that could BYPASS,
     * and check weather the authenticated user needs to change password
     * before accessing any other endpoint.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // if the request was made from an authenticated User that
        // has mustChangePassword parameter equals to true then
        // throw exception
        if (authentication != null
        &&  authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof
        LaborTrackUserDetails userDetails
        && userDetails.isMustChangePassword()) {

            // user is authenticated but mustChangePassword field still set to true THROW
            accessDeniedHandler.handleWithMessage(
                    request,
                    response,
                    "Password change required."
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}
