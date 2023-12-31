package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.io.PrintWriter;


@EnableWebSecurity
@Configuration
public class SecurityConfig implements CommunityConstant {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AntPathRequestMatcher[] requestMatchers = new AntPathRequestMatcher[] {
                AntPathRequestMatcher.antMatcher("/user/setting"),
                AntPathRequestMatcher.antMatcher("/user/upload"),
                AntPathRequestMatcher.antMatcher("/user/updatePassword"),
                AntPathRequestMatcher.antMatcher("/discuss/add"),
                AntPathRequestMatcher.antMatcher("/comment/add/**"),
                AntPathRequestMatcher.antMatcher("/letter/**"),
                AntPathRequestMatcher.antMatcher("/notice/**"),
                AntPathRequestMatcher.antMatcher("/like"),
                AntPathRequestMatcher.antMatcher("/follow"),
                AntPathRequestMatcher.antMatcher("/unfollow")};
        AntPathRequestMatcher[] moderatorRequestMatchers = new AntPathRequestMatcher[] {
                AntPathRequestMatcher.antMatcher("/discuss/top"),
                AntPathRequestMatcher.antMatcher("/discuss/wonderful")};
        AntPathRequestMatcher[] adminRequestMatchers = new AntPathRequestMatcher[] {
                AntPathRequestMatcher.antMatcher("/discuss/delete"),
                AntPathRequestMatcher.antMatcher("/data/**"),
                AntPathRequestMatcher.antMatcher("/actuator/**")};
        http.authorizeHttpRequests((authorize) -> {
            try {
                authorize
                        .requestMatchers(requestMatchers).hasAnyAuthority(AUTHORITY_USER, AUTHORITY_ADMIN, AUTHORITY_MODERATOR)
                        .requestMatchers(moderatorRequestMatchers).hasAuthority(AUTHORITY_MODERATOR)
                        .requestMatchers(adminRequestMatchers).hasAuthority(AUTHORITY_ADMIN)
                        .anyRequest().permitAll();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        http.csrf(c -> c.disable());
        // 权限不够时的处理
        http.exceptionHandling((exceptions) -> exceptions
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦！"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    // 权限不足
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限！"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                }));

        // Security底层默认会拦截/logout请求,进行退出处理.
        // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
        http.logout(httpSecurityLogoutConfigurer -> httpSecurityLogoutConfigurer.logoutUrl("/securitylogout"));
        return http.build();
    }

    // 自定义彻底清楚授权信息
    @Bean
    public SecurityContextLogoutHandler securityContextLogoutHandler() {
        return new SecurityContextLogoutHandler();
    }



}
