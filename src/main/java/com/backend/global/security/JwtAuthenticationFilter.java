package com.backend.global.security;

import com.backend.domain.member.entity.Member;
import com.backend.domain.member.repository.MemberRepository;
import com.backend.global.redis.RedisUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // JWT 검증을 건너뛸 경로들
        if (shouldSkipFilter(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (redisUtil.getData(token) == null && jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                Member member = memberRepository.findByEmail(email).orElseThrow(); // 토큰이 유효하면 유저는 반드시 존재

                User user = new User(member.getEmail(), member.getPassword(), List.of());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
    
    private boolean shouldSkipFilter(String path, String method) {
        // 정적 리소스
        if (path.startsWith("/static/") || path.startsWith("/public/") || 
            path.startsWith("/resources/") || path.startsWith("/META-INF/resources/")) {
            return true;
        }
        
        // 토스 페이먼트 관련
        if (path.equals("/billing.html") || path.startsWith("/payments/") || path.startsWith("/toss/")) {
            return true;
        }
        
        // 공개 API
        if (path.equals("/") || path.equals("/favicon.ico") || 
            path.startsWith("/h2-console/") || path.equals("/actuator/health")) {
            return true;
        }
        
        // 인증 API
        if (path.startsWith("/api/v1/auth/")) {
            return true;
        }
        
        // Swagger 및 API 문서
        if (path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs/") || 
            path.equals("/swagger-ui.html") || path.startsWith("/webjars/")) {
            return true;
        }
        
        // WebSocket 및 알림
        if (path.startsWith("/notifications/") || path.startsWith("/ws/")) {
            return true;
        }
        
        // 테스트 API
        if (path.startsWith("/api/test/") || path.equals("/bid-test.html") || 
            path.equals("/websocket-test.html")) {
            return true;
        }
        
        // GET 요청 중 공개 API
        if ("GET".equals(method)) {
            // 상품 조회 API
            if (path.matches("/api/[^/]+/products") || 
                path.matches("/api/[^/]+/products/\\d+") ||
                path.matches("/api/[^/]+/products/es") ||
                path.matches("/api/[^/]+/products/members/\\d+") ||
                path.matches("/api/[^/]+/products/es/members/\\d+")) {
                return true;
            }
            
            // 회원 조회 API
            if (path.matches("/api/v1/members/\\d+")) {
                return true;
            }
        }
        
        // 업로드 파일
        if (path.startsWith("/uploads/")) {
            return true;
        }
        
        // 테스트 데이터 API
        if (path.matches("/api/[^/]+/test-data/.*")) {
            return true;
        }
        
        // 입찰 API (기존 로직 유지)
        if (path.startsWith("/api/v1/bids/")) {
            return true;
        }
        
        return false;
    }
}
