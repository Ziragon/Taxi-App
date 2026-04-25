package com.example.paymentservice.unit;


import com.example.paymentservice.entity.DriverPayoutAccount;
import com.example.paymentservice.exception.DriverPayoutAccountNotFoundException;
import com.example.paymentservice.exception.DuplicatePaymentMethodException;
import com.example.paymentservice.exception.PayoutAccountNotVerifiedException;
import com.example.paymentservice.repository.DriverPayoutAccountRepository;
import com.example.paymentservice.service.DriverPayoutAccountService;
import com.example.paymentservice.service.StripeService;
import com.stripe.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverPayoutAccountService Tests")
class DriverPayoutAccountServiceTest {

    @Mock
    private DriverPayoutAccountRepository repository;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private DriverPayoutAccountService service;

    private Account mockStripeAccount;
    private DriverPayoutAccount mockPayoutAccount;

    @BeforeEach
    void setUp() {
        mockStripeAccount = new Account();
        mockStripeAccount.setId("acct_test123");

        mockPayoutAccount = DriverPayoutAccount.builder()
                .id(1L)
                .driverId(100L)
                .stripeAccountId("acct_test123")
                .lastFour("4242")
                .verified(true)
                .defaultvalue(true)
                .build();
    }

    @Test
    @DisplayName("Should add first payout account successfully")
    void shouldAddFirstPayoutAccount() {

        Long driverId = 100L;
        String lastFour = "4242";

        when(stripeService.getOrCreateConnectAccount(driverId)).thenReturn(mockStripeAccount);
        when(repository.existsByDriverIdAndStripeAccountId(driverId, mockStripeAccount.getId()))
                .thenReturn(false);
        when(repository.findAllByDriverId(driverId)).thenReturn(Collections.emptyList());
        when(stripeService.isAccountVerified(mockStripeAccount.getId())).thenReturn(true);
        when(repository.save(any(DriverPayoutAccount.class))).thenReturn(mockPayoutAccount);

        DriverPayoutAccount result = service.addPayoutAccount(driverId, lastFour);

        assertThat(result).isNotNull();
        assertThat(result.getDriverId()).isEqualTo(driverId);
        assertThat(result.isDefaultvalue()).isTrue();
        assertThat(result.isVerified()).isTrue();

        verify(repository).save(argThat(account ->
                account.getDriverId().equals(driverId) &&
                        account.isDefaultvalue() &&
                        account.isVerified()
        ));
    }

    @Test
    @DisplayName("Should throw exception when duplicate payout account")
    void shouldThrowExceptionWhenDuplicate() {

        Long driverId = 100L;
        String lastFour = "4242";

        when(stripeService.getOrCreateConnectAccount(driverId)).thenReturn(mockStripeAccount);
        when(repository.existsByDriverIdAndStripeAccountId(driverId, mockStripeAccount.getId()))
                .thenReturn(true);


        assertThatThrownBy(() -> service.addPayoutAccount(driverId, lastFour))
                .isInstanceOf(DuplicatePaymentMethodException.class)
                .hasMessageContaining("already exists");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should get payout account by ID")
    void shouldGetById() {

        when(repository.findById(1L)).thenReturn(Optional.of(mockPayoutAccount));

        DriverPayoutAccount result = service.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw exception when payout account not found")
    void shouldThrowExceptionWhenNotFound() {

        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(DriverPayoutAccountNotFoundException.class);
    }

    @Test
    @DisplayName("Should get default payout account for driver")
    void shouldGetDefaultForDriver() {

        Long driverId = 100L;
        when(repository.findByDriverIdAndDefaultvalueTrue(driverId))
                .thenReturn(Optional.of(mockPayoutAccount));

        DriverPayoutAccount result = service.getDefaultForDriver(driverId);

        assertThat(result).isNotNull();
        assertThat(result.isDefaultvalue()).isTrue();
    }

    @Test
    @DisplayName("Should get verified default payout account")
    void shouldGetVerifiedDefaultForDriver() {

        Long driverId = 100L;
        when(repository.findByDriverIdAndDefaultvalueTrue(driverId))
                .thenReturn(Optional.of(mockPayoutAccount));

        DriverPayoutAccount result = service.getVerifiedDefaultForDriver(driverId);

        assertThat(result).isNotNull();
        assertThat(result.isVerified()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when default account is not verified")
    void shouldThrowExceptionWhenDefaultNotVerified() {

        Long driverId = 100L;
        mockPayoutAccount.setVerified(false);
        when(repository.findByDriverIdAndDefaultvalueTrue(driverId))
                .thenReturn(Optional.of(mockPayoutAccount));

        assertThatThrownBy(() -> service.getVerifiedDefaultForDriver(driverId))
                .isInstanceOf(PayoutAccountNotVerifiedException.class);
    }

    @Test
    @DisplayName("Should get all payout accounts for driver")
    void shouldGetAllByDriverId() {

        Long driverId = 100L;
        List<DriverPayoutAccount> accounts = List.of(mockPayoutAccount);
        when(repository.findAllByDriverId(driverId)).thenReturn(accounts);

        List<DriverPayoutAccount> result = service.getAllByDriverId(driverId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDriverId()).isEqualTo(driverId);
    }

    @Test
    @DisplayName("Should set default payout account")
    void shouldSetDefault() {

        Long driverId = 100L;
        Long accountId = 1L;
        when(repository.findById(accountId)).thenReturn(Optional.of(mockPayoutAccount));
        when(repository.save(any())).thenReturn(mockPayoutAccount);

        service.setDefault(driverId, accountId);

        verify(repository).clearDefaultForDriver(driverId);
        verify(repository).save(argThat(DriverPayoutAccount::isDefaultvalue));
    }

    @Test
    @DisplayName("Should throw exception when setting unverified account as default")
    void shouldThrowExceptionWhenSetUnverifiedAsDefault() {

        Long driverId = 100L;
        Long accountId = 1L;
        mockPayoutAccount.setVerified(false);
        when(repository.findById(accountId)).thenReturn(Optional.of(mockPayoutAccount));

        assertThatThrownBy(() -> service.setDefault(driverId, accountId))
                .isInstanceOf(PayoutAccountNotVerifiedException.class)
                .hasMessageContaining("Cannot set unverified");
    }

    @Test
    @DisplayName("Should sync verification status")
    void shouldSyncVerificationStatus() {

        Long accountId = 1L;
        when(repository.findById(accountId)).thenReturn(Optional.of(mockPayoutAccount));
        when(stripeService.isAccountVerified(mockPayoutAccount.getStripeAccountId()))
                .thenReturn(true);

        service.syncVerificationStatus(accountId);

        verify(repository).updateVerificationStatus(accountId, true);
    }

    @Test
    @DisplayName("Should verify payout account")
    void shouldVerify() {

        Long accountId = 1L;
        when(repository.findById(accountId)).thenReturn(Optional.of(mockPayoutAccount));
        when(stripeService.isAccountVerified(mockPayoutAccount.getStripeAccountId()))
                .thenReturn(true);

        service.verify(accountId);

        verify(repository).updateVerificationStatus(accountId, true);
    }

    @Test
    @DisplayName("Should throw exception when verifying unverified Stripe account")
    void shouldThrowExceptionWhenVerifyingUnverifiedStripeAccount() {

        Long accountId = 1L;
        when(repository.findById(accountId)).thenReturn(Optional.of(mockPayoutAccount));
        when(stripeService.isAccountVerified(mockPayoutAccount.getStripeAccountId()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.verify(accountId))
                .isInstanceOf(PayoutAccountNotVerifiedException.class)
                .hasMessageContaining("not verified in Stripe");
    }

    @Test
    @DisplayName("Should unverify payout account")
    void shouldUnverify() {

        Long accountId = 1L;

        service.unverify(accountId);

        verify(repository).updateVerificationStatus(accountId, false);
    }
}
