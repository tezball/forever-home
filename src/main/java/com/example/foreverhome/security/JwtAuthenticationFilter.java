package com.example.foreverhome.security;

import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.service.CookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String IMPERSONATION_HEADER = "X-Impersonation-Token";

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieService cookieService;
    private final JdbcClient jdbcClient;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CookieService cookieService, JdbcClient jdbcClient) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieService = cookieService;
        this.jdbcClient = jdbcClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // First, check for impersonation token (admin viewing as another user)
            String impersonationToken = request.getHeader(IMPERSONATION_HEADER);
            if (StringUtils.hasText(impersonationToken)) {
                Optional<ImpersonationSession> session = validateImpersonationToken(impersonationToken);
                if (session.isPresent()) {
                    ImpersonationSession imp = session.get();
                    logger.info("Impersonation active: admin {} viewing as user {} ({})",
                            imp.adminUserId, imp.targetUserId, imp.targetEmail);

                    UserRole role = UserRole.valueOf(imp.targetRole);
                    UserPrincipal principal = new UserPrincipal(imp.targetUserId, role, true);
                    principal.setImpersonated(true);
                    principal.setImpersonatingAdminId(imp.adminUserId);

                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role.name())
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // Standard JWT authentication
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);
                UserRole role = jwtTokenProvider.getRoleFromToken(jwt);
                boolean verified = jwtTokenProvider.isVerifiedFromToken(jwt);

                UserPrincipal principal = new UserPrincipal(userId, role, verified);
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role.name())
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Validate an impersonation token against the database.
     */
    private Optional<ImpersonationSession> validateImpersonationToken(String token) {
        try {
            return jdbcClient.sql("""
                SELECT is.id, is.admin_user_id, is.target_user_id, u.email as target_email, u.role as target_role
                FROM impersonation_sessions is
                JOIN app_users u ON is.target_user_id = u.id
                WHERE is.token = :token
                AND is.ended_at IS NULL
                AND is.expires_at > NOW()
                """)
                    .param("token", token)
                    .query((rs, rowNum) -> new ImpersonationSession(
                            UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("admin_user_id")),
                            UUID.fromString(rs.getString("target_user_id")),
                            rs.getString("target_email"),
                            rs.getString("target_role")
                    ))
                    .optional();
        } catch (Exception e) {
            logger.warn("Failed to validate impersonation token", e);
            return Optional.empty();
        }
    }

    private record ImpersonationSession(
            UUID sessionId,
            UUID adminUserId,
            UUID targetUserId,
            String targetEmail,
            String targetRole
    ) {}

    private String extractJwtFromRequest(HttpServletRequest request) {
        // First, try to get token from Authorization header (for backward compatibility)
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        // Fall back to httpOnly cookie (preferred secure method)
        return cookieService.getAccessToken(request).orElse(null);
    }
}
