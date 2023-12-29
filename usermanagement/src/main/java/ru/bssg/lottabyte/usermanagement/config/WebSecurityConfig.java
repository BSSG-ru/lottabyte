package ru.bssg.lottabyte.usermanagement.config;

import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import ru.bssg.lottabyte.core.usermanagement.security.JwtAuthenticationEntryPoint;
import ru.bssg.lottabyte.usermanagement.service.UserService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import static org.springframework.security.config.Customizer.withDefaults;

import javax.naming.directory.Attribute;
import javax.servlet.http.HttpServletRequest;

import java.util.*;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true
)
@Component
@Lazy
@Data
@Slf4j
@EqualsAndHashCode(callSuper=false)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

        @Autowired
        private final UserService userService;

        @Autowired
        private JwtAuthenticationEntryPoint unauthorizedHandler;

        public static final String AUTHORITIES_CLAIM_NAME = "roles";

        @Override
        protected void configure(HttpSecurity http) throws Exception {
                http.requestMatcher(new OAuthRequestedMatcher())
                        .csrf()
                                .disable()
                        .cors()
                                .and()
                        .exceptionHandling()
                                .authenticationEntryPoint(unauthorizedHandler)
                        .and()
                                .sessionManagement()
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .and()
                        .authorizeRequests()
                        .antMatchers("/v1/preauth/**").permitAll()
                        .antMatchers("/v1/usermgmt/**").permitAll()
                        .antMatchers("/error").permitAll()
                        .anyRequest()
                        .fullyAuthenticated()
                        .and()
                        .httpBasic(withDefaults())
                        .formLogin();

                // JWT Validation Configuration
                http.oauth2ResourceServer()
                        .jwt()
                        .jwtAuthenticationConverter(authenticationConverter());
        }

        private static class OAuthRequestedMatcher implements RequestMatcher {
                @Override
                public boolean matches(HttpServletRequest request) {
                        String auth = request.getHeader("Authorization");
                        // Determine if the client request contained an OAuth Authorization
                        return (auth != null) && auth.startsWith("Bearer");
                }
        }

        protected JwtAuthenticationConverter authenticationConverter() {
                JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
                authoritiesConverter.setAuthorityPrefix("");
                authoritiesConverter.setAuthoritiesClaimName(AUTHORITIES_CLAIM_NAME);

                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
                return converter;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

}
