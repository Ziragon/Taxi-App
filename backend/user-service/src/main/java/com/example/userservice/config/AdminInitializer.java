package com.example.userservice.config;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.enums.AccountRole;
import com.example.userservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (accountRepository.existsByEmail(adminProperties.email())) {
            log.info("Admin account already exists, skipping initialization");
            return;
        }

        Account admin = Account.builder()
                .email(adminProperties.email())
                .phone(adminProperties.phone())
                .passwordHash(passwordEncoder.encode(adminProperties.password()))
                .role(AccountRole.ADMIN)
                .active(true)
                .build();

        accountRepository.save(admin);
        log.info("Admin account created: {}", adminProperties.email());
    }
}
