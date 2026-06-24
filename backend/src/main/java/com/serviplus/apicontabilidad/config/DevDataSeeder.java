package com.serviplus.apicontabilidad.config;

import com.serviplus.apicontabilidad.data.UsuarioRepository;
import com.serviplus.apicontabilidad.domain.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DevDataSeeder {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultAdmin() {
        if (usuarioRepository.count() > 0) return;

        usuarioRepository.save(Usuario.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin1234!"))
                .role("ROLE_ADMIN")
                .build());

        log.info("Default admin user created — username: admin / password: Admin1234!");
    }
}
