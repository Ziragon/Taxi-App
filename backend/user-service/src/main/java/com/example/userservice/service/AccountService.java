package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.enums.AccountRole;
import com.example.userservice.exception.AccountAlreadyExistsException;
import com.example.userservice.exception.AccountNotFoundException;
import com.example.userservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Account createAccount(String email, String phone, String password) {
        if (accountRepository.existsByEmailOrPhone(email, phone)) {
            throw new AccountAlreadyExistsException("email or phone", email + " / " + phone);
        }

        String passwordHash = passwordEncoder.encode(password);

        Account account = Account.builder()
                .email(email)
                .phone(phone)
                .passwordHash(passwordHash)
                .role(AccountRole.USER)
                .isActive(true)
                .build();

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Account findByEmail(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("email", email));
    }

    @Transactional(readOnly = true)
    public Account findByPhone(String phone) {
        return accountRepository.findByPhone(phone)
                .orElseThrow(() -> new AccountNotFoundException("phone", phone));
    }

    @Transactional
    public void deactivateAccount(Long id) {
        Account account = findById(id);
        account.setIsActive(false);
        accountRepository.save(account);
    }

    @Transactional
    public void activateAccount(Long id) {
        Account account = findById(id);
        account.setIsActive(true);
        accountRepository.save(account);
    }
}
