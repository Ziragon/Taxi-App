package com.example.paymentservice.integration;

import com.example.paymentservice.entity.DriverPayoutAccount;
import com.example.paymentservice.exception.DriverPayoutAccountNotFoundException;
import com.example.paymentservice.exception.DuplicatePaymentMethodException;
import com.example.paymentservice.exception.PayoutAccountNotVerifiedException;
import com.example.paymentservice.repository.DriverPayoutAccountRepository;
import com.example.paymentservice.service.DriverPayoutAccountService;
import com.example.paymentservice.service.StripeService;
import com.stripe.model.Account;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Driver Payout Account Flow Integration Tests")
class DriverPayoutAccountIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DriverPayoutAccountService payoutAccountService;

    @Autowired
    private DriverPayoutAccountRepository payoutAccountRepository;

    @MockitoBean
    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        payoutAccountRepository.deleteAll();

        Account mockStripeAccount = new Account();
        mockStripeAccount.setId("acct_test123");

        when(stripeService.getOrCreateConnectAccount(anyLong())).thenReturn(mockStripeAccount);
        when(stripeService.isAccountVerified(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("Полный flow: добавление аккаунта -> верификация -> смена default")
    void fullPayoutAccountFlow() {

        DriverPayoutAccount first = payoutAccountService.addPayoutAccount(200L, "4242");

        assertThat(first).satisfies(acc -> {
            assertThat(acc.getDriverId()).isEqualTo(200L);
            assertThat(acc.getStripeAccountId()).isEqualTo("acct_test123");
            assertThat(acc.isVerified()).isTrue();
            assertThat(acc.isDefaultvalue()).isTrue();
        });

        Account secondStripeAccount = new Account();
        secondStripeAccount.setId("acct_second456");

        when(stripeService.getOrCreateConnectAccount(201L)).thenReturn(secondStripeAccount);
        when(stripeService.isAccountVerified("acct_second456")).thenReturn(false);

        DriverPayoutAccount second = payoutAccountService.addPayoutAccount(201L, "4242");

        assertThat(second.isVerified()).isFalse();
        assertThat(second.isDefaultvalue()).isTrue();

        when(stripeService.isAccountVerified("acct_second456")).thenReturn(true);
        payoutAccountService.syncVerificationStatus(second.getId());

        entityManager.flush();
        entityManager.clear();

        DriverPayoutAccount synced = payoutAccountService.getById(second.getId());
        assertThat(synced.isVerified()).isTrue();

        DriverPayoutAccount defaultForFirst = payoutAccountService.getDefaultForDriver(200L);
        assertThat(defaultForFirst.getId()).isEqualTo(first.getId());
    }



    @Test
    @DisplayName("Первый аккаунт всегда становится default")
    void firstAccountAlwaysBecomesDefault() {
        DriverPayoutAccount account = payoutAccountService.addPayoutAccount(200L, "4242");
        assertThat(account.isDefaultvalue()).isTrue();
    }

    @Test
    @DisplayName("Дублирующий аккаунт выбрасывает DuplicatePaymentMethodException")
    void duplicateAccountThrowsException() {
        payoutAccountService.addPayoutAccount(200L, "4242");

        assertThatThrownBy(() ->
                payoutAccountService.addPayoutAccount(200L, "4242")
        ).isInstanceOf(DuplicatePaymentMethodException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("getVerifiedDefaultForDriver выбрасывает исключение если аккаунт не верифицирован")
    void getVerifiedDefaultThrowsWhenNotVerified() {
        when(stripeService.isAccountVerified(anyString())).thenReturn(false);

        payoutAccountService.addPayoutAccount(200L, "4242");

        assertThatThrownBy(() ->
                payoutAccountService.getVerifiedDefaultForDriver(200L)
        ).isInstanceOf(PayoutAccountNotVerifiedException.class);
    }

    @Test
    @DisplayName("setDefault выбрасывает исключение для неверифицированного аккаунта")
    void setDefaultThrowsWhenNotVerified() {
        when(stripeService.isAccountVerified(anyString())).thenReturn(false);

        DriverPayoutAccount account = payoutAccountService.addPayoutAccount(200L, "4242");

        assertThatThrownBy(() ->
                payoutAccountService.setDefault(200L, account.getId())
        ).isInstanceOf(PayoutAccountNotVerifiedException.class)
                .hasMessageContaining("Cannot set unverified");
    }

    @Test
    @DisplayName("verify выбрасывает исключение если Stripe аккаунт не верифицирован")
    void verifyThrowsWhenStripeNotVerified() {
        when(stripeService.isAccountVerified(anyString())).thenReturn(false);

        DriverPayoutAccount account = payoutAccountService.addPayoutAccount(200L, "4242");

        assertThatThrownBy(() ->
                payoutAccountService.verify(account.getId())
        ).isInstanceOf(PayoutAccountNotVerifiedException.class)
                .hasMessageContaining("not verified in Stripe");
    }

    @Test
    @DisplayName("unverify сбрасывает верификацию")
    void unverifyResetsVerification() {
        DriverPayoutAccount account = payoutAccountService.addPayoutAccount(200L, "4242");
        assertThat(account.isVerified()).isTrue();

        payoutAccountService.unverify(account.getId());

        entityManager.flush();
        entityManager.clear();

        DriverPayoutAccount unverified = payoutAccountRepository.findById(account.getId()).orElseThrow();
        assertThat(unverified.isVerified()).isFalse();
    }

    @Test
    @DisplayName("getAllByDriverId возвращает все аккаунты водителя")
    void getAllByDriverIdReturnsAll() {
        payoutAccountService.addPayoutAccount(200L, "4242");

        Account second = new Account();
        second.setId("acct_second");
        when(stripeService.getOrCreateConnectAccount(300L)).thenReturn(second);
        when(stripeService.isAccountVerified("acct_second")).thenReturn(true);

        payoutAccountService.addPayoutAccount(300L, "5555");

        List<DriverPayoutAccount> for200 = payoutAccountService.getAllByDriverId(200L);
        List<DriverPayoutAccount> for300 = payoutAccountService.getAllByDriverId(300L);

        assertThat(for200).hasSize(1);
        assertThat(for300).hasSize(1);
        assertThat(for200.getFirst().getDriverId()).isEqualTo(200L);
        assertThat(for300.getFirst().getDriverId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("getById выбрасывает исключение если не найден")
    void getByIdThrowsWhenNotFound() {
        assertThatThrownBy(() ->
                payoutAccountService.getById(999L)
        ).isInstanceOf(DriverPayoutAccountNotFoundException.class);
    }

    @Test
    @DisplayName("getDefaultForDriver выбрасывает исключение если default не найден")
    void getDefaultThrowsWhenNoDefault() {
        assertThatThrownBy(() ->
                payoutAccountService.getDefaultForDriver(999L)
        ).isInstanceOf(DriverPayoutAccountNotFoundException.class);
    }
}