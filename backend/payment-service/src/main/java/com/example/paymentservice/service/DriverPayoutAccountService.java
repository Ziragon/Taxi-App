package com.example.paymentservice.service;

import com.example.paymentservice.entity.DriverPayoutAccount;
import com.example.paymentservice.exception.DriverPayoutAccountNotFoundException;
import com.example.paymentservice.exception.DuplicatePaymentMethodException;
import com.example.paymentservice.exception.InvalidPaymentOperationException;
import com.example.paymentservice.exception.PayoutAccountNotVerifiedException;
import com.example.paymentservice.repository.DriverPayoutAccountRepository;
import com.example.shared.exception.common.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverPayoutAccountService {

    private final DriverPayoutAccountRepository driverPayoutAccountRepository;
    private final StripeService stripeService;

    @Transactional
    public DriverPayoutAccount addPayoutAccount(Long driverId, String lastFour) {

        StripeService.FakeAccount stripeAccount = stripeService.getOrCreateConnectAccount(driverId);

        if (driverPayoutAccountRepository.existsByDriverIdAndStripeAccountId(driverId, stripeAccount.id())) {
            throw new DuplicatePaymentMethodException(
                    "Payout account already exists for driver: %s".formatted(driverId)
            );
        }

        boolean isFirst = driverPayoutAccountRepository.findAllByDriverId(driverId).isEmpty();
        boolean isVerified = stripeService.isAccountVerified(stripeAccount.id());

        DriverPayoutAccount payoutAccount = DriverPayoutAccount.builder()
                .driverId(driverId)
                .stripeAccountId(stripeAccount.id())
                .lastFour(lastFour)
                .verified(isVerified)
                .defaultvalue(isFirst)
                .build();

        DriverPayoutAccount saved = driverPayoutAccountRepository.save(payoutAccount);

        log.info("Payout account added for driver {} verified={}", driverId, isVerified);

        return saved;
    }

    @Transactional(readOnly = true)
    public DriverPayoutAccount getById(Long id) {
        return driverPayoutAccountRepository.findById(id)
                .orElseThrow(() -> new DriverPayoutAccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public DriverPayoutAccount getDefaultForDriver(Long driverId) {
        return driverPayoutAccountRepository.findByDriverIdAndDefaultvalueTrue(driverId)
                .orElseThrow(() -> new DriverPayoutAccountNotFoundException(
                        "driverId", driverId
                ));
    }

    @Transactional(readOnly = true)
    public DriverPayoutAccount getVerifiedDefaultForDriver(Long driverId) {
        DriverPayoutAccount account = getDefaultForDriver(driverId);

        if (!account.isVerified()) {
            throw new PayoutAccountNotVerifiedException(driverId);
        }

        return account;
    }

    @Transactional(readOnly = true)
    public List<DriverPayoutAccount> getAllByDriverId(Long driverId) {
        return driverPayoutAccountRepository.findAllByDriverId(driverId);
    }

    @Transactional
    public void setDefault(Long driverId, Long payoutAccountId) {
        DriverPayoutAccount payoutAccount = getById(payoutAccountId);

        if (!payoutAccount.getDriverId().equals(driverId)) {
            throw new AccessDeniedException();
        }

        if (!payoutAccount.isVerified()) {
            throw new PayoutAccountNotVerifiedException(
                    "Cannot set unverified payout account as default: %s".formatted(payoutAccountId)
            );
        }

        if (payoutAccount.isDefaultvalue()) {
            return;
        }

        driverPayoutAccountRepository.clearDefaultForDriver(driverId);

        payoutAccount.setDefaultvalue(true);
        driverPayoutAccountRepository.save(payoutAccount);

        log.info("Payout account {} set as default for driver {}", payoutAccountId, driverId);
    }


    @Transactional
    public void delete(Long driverId, Long payoutAccountId) {
        DriverPayoutAccount payoutAccount = getById(payoutAccountId);

        if (!payoutAccount.getDriverId().equals(driverId)) {
            throw new AccessDeniedException();
        }

        if (payoutAccount.isDefaultvalue()) {
            List<DriverPayoutAccount> others = driverPayoutAccountRepository
                    .findAllByDriverId(driverId)
                    .stream()
                    .filter(a -> !a.getId().equals(payoutAccountId))
                    .toList();

            if (!others.isEmpty()) {
                throw new InvalidPaymentOperationException(
                        "Payout account",
                        "cannot delete default account while other accounts exist"
                );
            }
        }

        driverPayoutAccountRepository.deleteById(payoutAccountId);

        log.info("Payout account {} deleted for driver {}", payoutAccountId, driverId);
    }

    @Transactional
    public void syncVerificationStatus(Long payoutAccountId) {
        DriverPayoutAccount account = getById(payoutAccountId);

        boolean isVerified = stripeService.isAccountVerified(account.getStripeAccountId());

        driverPayoutAccountRepository.updateVerificationStatus(payoutAccountId, isVerified);

        log.info("Synced verification status for payout account {} verified={}", payoutAccountId, isVerified);
    }

    @Transactional
    public void verify(Long payoutAccountId) {
        DriverPayoutAccount account = getById(payoutAccountId);

        boolean isVerified = stripeService.isAccountVerified(account.getStripeAccountId());

        if (!isVerified) {
            throw new PayoutAccountNotVerifiedException(
                    "Account is not verified in Stripe: %s".formatted(payoutAccountId)
            );
        }

        driverPayoutAccountRepository.updateVerificationStatus(payoutAccountId, true);

        log.info("Payout account {} verified manually", payoutAccountId);
    }

    @Transactional
    public void unverify(Long payoutAccountId) {
        driverPayoutAccountRepository.updateVerificationStatus(payoutAccountId, false);
        log.info("Payout account {} unverified", payoutAccountId);
    }
}