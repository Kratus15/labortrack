package com.labortrack.labortrack_backend.security.jwt;

import com.labortrack.labortrack_backend.security.user.LaborTrackUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * This class is responsible for generating JWT token when the user
 * sign-in, validating a toke verifying it is/still valid to use,
 * extract the username from the token, and extracting expiration
 * time.
 */
@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // not injected internally created by JwtService.class
        this.signingKey =  Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtProperties.secret())
        );
    }

    /**
     * This method build the JWT token object with claims of userId, companyId,
     * role, and employeeId if user is one. This is when a user login, you generate
     * a valid JWT token, create authentication and save authentication on security context.
     * Have in mind the subject/username is the email because we are treating
     * email as username.
     */
    public String generateToken(LaborTrackUserDetails userDetails) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.expiration());

        JwtBuilder tokenBuilder = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getUserId())
                .claim("companyId", userDetails.getCompanyId())
                .claim("role",  userDetails.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt));

        if (userDetails.getEmployeeId() != null) {
            tokenBuilder.claim("employeeId", userDetails.getEmployeeId());
        }

        return tokenBuilder
                .signWith(signingKey)
                .compact();
    }

    /**
     * This method extract the username from the JWT token object. Username
     * is a claim on the object and this method simply extract that claim
     * and get its value. The subject of the object represents the username
     * which in this case we assign the email as the username.
     */
    public String extractUsernameFromToken(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * This method verifies if the token is/still valid. It extracts the username
     * from the token, then check userDetails. Username if matched, then return true,
     * otherwise false. An expired token object causes JJWT parsing fail, and catch
     * block return false.
     */
    public boolean isTokenValid(
            String token,
            LaborTrackUserDetails userDetails) {
        // extract username from toke and compare it with userDetails.username. return true if matched, otherwise false
        try {
            String username = extractUsernameFromToken(token);
            return username.equals(userDetails.getUsername())
                    && userDetails.isEnabled();
        }
        catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * This method return the expiration time in seconds of the JWT token
     * objects. Got it from JwtProperties, which gave the reference values
     * for JWT properties.
     */
    public long getExpirationSeconds() {
        return jwtProperties.expiration().toSeconds();
    }

    // HELPER METHODS

    private Claims extractClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
