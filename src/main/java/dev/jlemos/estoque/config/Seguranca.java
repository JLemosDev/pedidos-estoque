package dev.jlemos.estoque.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class Seguranca {

  @Bean
  PasswordEncoder senhaEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService usuarios(
    PasswordEncoder encoder,
    @Value("${app.usuario}") String usuario,
    @Value("${app.senha}") String senha
  ) {
    if (senha.length() < 8) throw new IllegalArgumentException(
      "APP_PASSWORD deve ter ao menos 8 caracteres."
    );
    return new InMemoryUserDetailsManager(
      User.withUsername(usuario)
        .password(encoder.encode(senha))
        .roles("ADMIN")
        .build()
    );
  }

  @Bean
  SecurityFilterChain acesso(HttpSecurity http) throws Exception {
    return http
      .authorizeHttpRequests(a ->
        a
          .requestMatchers(
            "/",
            "/index.html",
            "/app.js",
            "/app.css",
            "/favicon.svg",
            "/error"
          )
          .permitAll()
          .anyRequest()
          .authenticated()
      )
      .sessionManagement(s ->
        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .csrf(c -> c.disable())
      .httpBasic(b ->
        b.authenticationEntryPoint((request, response, e) -> {
          response.setStatus(401);
          response.setContentType("application/json;charset=UTF-8");
          response
            .getWriter()
            .write("{\"error\":\"Usuário ou senha inválidos.\"}");
        })
      )
      .build();
  }
}
