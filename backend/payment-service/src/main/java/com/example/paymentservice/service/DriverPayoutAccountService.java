package com.example.paymentservice.service;

import com.example.paymentservice.entity.DriverPayoutAccount;
import com.example.paymentservice.exception.DriverPayoutAccountNotFoundException;
import com.example.paymentservice.exception.DuplicatePaymentMethodException;
import com.example.paymentservice.exception.PayoutAccountNotVerifiedException;
import com.example.paymentservice.repository.DriverPayoutAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverPayoutAccountService {

    private final DriverPayoutAccountRepository driverPayoutAccountRepository;

    @Transactional
    public DriverPayoutAccount addPayoutAccount(Long driverId,
                                                String stripeAccountId,
                                                String lastFour) {
        if (driverPayoutAccountRepository.existsByDriverIdAndStripeAccountId(driverId, stripeAccountId)) {
            throw new DuplicatePaymentMethodException(
                    "Payout account already exists for driver %s with account: %s"
                            .formatted(driverId, stripeAccountId)
            );
        }

        boolean isFirst = driverPayoutAccountRepository.findAllByDriverId(driverId).isEmpty();

        DriverPayoutAccount payoutAccount = DriverPayoutAccount.builder()
                .driverId(driverId)
                .stripeAccountId(stripeAccountId)
                .lastFour(lastFour)
                .verified(false)
                .defaultvalue(isFirst)
                .build();

        return driverPayoutAccountRepository.save(payoutAccount);
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

        if (!payoutAccount.isVerified()) {
            throw new PayoutAccountNotVerifiedException(
                    "Cannot set unverified payout account as default: %s".formatted(payoutAccountId)
            );
        }

        driverPayoutAccountRepository.clearDefaultForDriver(driverId);

        payoutAccount.setDefaultvalue(true);
        driverPayoutAccountRepository.save(payoutAccount);
    }

    @Transactional
    public void verify(Long payoutAccountId) {
        driverPayoutAccountRepository.updateVerificationStatus(payoutAccountId, true);
    }

    @Transactional
    public void unverify(Long payoutAccountId) {
        driverPayoutAccountRepository.updateVerificationStatus(payoutAccountId, false);
    }
}
