package pe.edu.upc.iam_service.iam.infrastructure.authorization.sfs.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pe.edu.upc.iam_service.iam.infrastructure.authorization.auth0.Auth0AuthorizationRequestFilter;
import pe.edu.upc.iam_service.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import pe.edu.upc.iam_service.iam.infrastructure.authorization.sfs.pipeline.InternalServiceAuthenticationFilter;
import pe.edu.upc.iam_service.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import pe.edu.upc.iam_service.iam.infrastructure.tokens.jwt.BearerTokenService;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {
    private final UserDetailsService userDetailsService;
    private final BearerTokenService tokenService;
    private final BCryptHashingService hashingService;
    private final AuthenticationEntryPoint unauthorizedRequestHandler;
    private final String internalServiceSecret;
    private final boolean legacyJwtEnabled;
    private final JwtDecoder auth0JwtDecoder;

    public WebSecurityConfiguration(
            @Qualifier("defaultUserDetailsService") UserDetailsService userDetailsService,
            BearerTokenService tokenService,
            BCryptHashingService hashingService,
            AuthenticationEntryPoint unauthorizedRequestHandler,
            @Qualifier("auth0JwtDecoder") JwtDecoder auth0JwtDecoder,
            @Value("${authorization.internal-service.secret:internal-service-secret-key}") String internalServiceSecret,
            @Value("${authorization.legacy-jwt.enabled:true}") boolean legacyJwtEnabled
    ) {
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.hashingService = hashingService;
        this.unauthorizedRequestHandler = unauthorizedRequestHandler;
        this.internalServiceSecret = internalServiceSecret;
        this.legacyJwtEnabled = legacyJwtEnabled;
        this.auth0JwtDecoder = auth0JwtDecoder;
    }

    @Bean
    public BearerAuthorizationRequestFilter authorizationRequestFilter() {
        return new BearerAuthorizationRequestFilter(tokenService, userDetailsService);
    }

    @Bean
    public InternalServiceAuthenticationFilter internalServiceAuthenticationFilter() {
        return new InternalServiceAuthenticationFilter(internalServiceSecret);
    }

    @Bean
    public Auth0AuthorizationRequestFilter auth0AuthorizationRequestFilter() {
        return new Auth0AuthorizationRequestFilter(auth0JwtDecoder);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(hashingService);
        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return hashingService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CORS default configuration
        http.cors(AbstractHttpConfigurer::disable);

        // CSRF disabled
        http.csrf(AbstractHttpConfigurer::disable);

        // Identity and Access Management Configuration
        http.exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(unauthorizedRequestHandler))
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(
                                "/api/v1/authentication/**",
                                "/api/v1/jwks/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/actuator/**"
                        ).permitAll()
                        .anyRequest().authenticated());
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(internalServiceAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(auth0AuthorizationRequestFilter(), UsernamePasswordAuthenticationFilter.class);
        if (legacyJwtEnabled) {
            http.addFilterBefore(authorizationRequestFilter(), UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }
}
