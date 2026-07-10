package pe.edu.upc.iam_service.iam.infrastructure.authorization.auth0;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class Auth0AuthorizationRequestFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Auth0AuthorizationRequestFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/authentication/**",
            "/api/v1/jwks/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtDecoder jwtDecoder;

    public Auth0AuthorizationRequestFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return true;
        }
        var requestPath = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        var token = getBearerToken(request);
        if (token != null) {
            try {
                var jwt = jwtDecoder.decode(token);
                SecurityContextHolder.getContext()
                        .setAuthentication(new JwtAuthenticationToken(jwt, authoritiesFrom(jwt), jwt.getSubject()));
                LOGGER.debug("Auth0 JWT authenticated for subject {}", jwt.getSubject());
            } catch (Exception exception) {
                LOGGER.debug("Auth0 JWT authentication did not match this token: {}", exception.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getBearerToken(HttpServletRequest request) {
        var authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    private Collection<SimpleGrantedAuthority> authoritiesFrom(Jwt jwt) {
        var permissions = jwt.getClaimAsStringList("permissions");
        var scopes = jwt.getClaimAsString("scope");

        var permissionAuthorities = permissions == null
                ? Stream.<String>empty()
                : permissions.stream();

        var scopeAuthorities = !StringUtils.hasText(scopes)
                ? Stream.<String>empty()
                : Stream.of(scopes.split(" "))
                .filter(StringUtils::hasText)
                .map(scope -> "SCOPE_" + scope);

        return Stream.concat(permissionAuthorities, scopeAuthorities)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
