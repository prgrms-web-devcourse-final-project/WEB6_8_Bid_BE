package com.backend.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스(/static, /public, /resources, /META-INF/resources)..
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        // 토스 리다이렉트용 정적 페이지..
                        .requestMatchers("/billing.html", "/payments/**", "/toss/**").permitAll()

                        // 공개 API - 루트, 파비콘, h2-console, actuator health
                        .requestMatchers("/", "/favicon.ico", "/h2-console/**", "/actuator/health").permitAll()
                        .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**",
                                "/swagger-ui.html", "/webjars/**", "/notifications/**", "/ws/**",
                                "/api/test/**", "/bid-test.html", "/websocket-test.html").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/*/products", "/api/*/products/{productId:\\d+}", "/api/*/products/es",
                                "/api/*/products/members/{memberId:\\d+}", "/api/*/products/es/members/{memberId:\\d+}",
                                "/api/v1/members/{memberId:\\d+}").permitAll()
                        .requestMatchers("/api/*/test-data/**").permitAll()

                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진 설정
        configuration.setAllowedOrigins(List.of(
                "https://cdpn.io",
                "http://localhost:3000",
                "https://bid-market.shop",
                "https://www.bid-market.shop",
                "https://api.bid-market.shop"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));

        // 자격 증명 허용 설정
        configuration.setAllowCredentials(true);

        // 허용할 헤더 설정
        configuration.setAllowedHeaders(List.of("*"));

        // CORS 설정을 소스에 등록
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
