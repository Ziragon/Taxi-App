package com.example.paymentservice.service;

import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.exception.DefaultPaymentMethodException;
import com.example.paymentservice.exception.DuplicatePaymentMethodException;
import com.example.paymentservice.exception.PaymentMethodNotActiveException;
import com.example.paymentservice.exception.PaymentMethodNotFoundException;
import com.example.paymentservice.repository.PaymentMethodRepository;
import com.stripe.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final StripeService stripeService;

    @Transactional
    public PaymentMethod addPaymentMethod(Long passengerId,
                                          String stripePaymentMethodId,
                                          boolean setAsDefault) {
        if (paymentMethodRepository.existsByPassengerIdAndStripePaymentMethodId(passengerId, stripePaymentMethodId)) {
            throw new DuplicatePaymentMethodException(
                    "Payment method already exists for passenger: %s".formatted(passengerId)
            );
        }

        StripeService.FakeCustomer customer = stripeService.getOrCreateCustomer(passengerId);

        StripeService.FakePaymentMethod stripePaymentMethod =
                stripeService.attachPaymentMethodToCustomer(stripePaymentMethodId, customer.id());

        boolean isFirst = paymentMethodRepository.findAllByPassengerId(passengerId).isEmpty();
        boolean shouldBeDefault = isFirst || setAsDefault;

        if (shouldBeDefault) {
            paymentMethodRepository.clearDefaultForPassenger(passengerId);
        }

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .passengerId(passengerId)
                .stripeCustomerId(customer.id())
                .stripePaymentMethodId(stripePaymentMethodId)
                .cardBrand(stripePaymentMethod.brand())
                .lastFour(stripePaymentMethod.last4())
                .defaultvalue(shouldBeDefault)
                .active(true)
                .build();

        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

        if (shouldBeDefault) {
            stripeService.setDefaultPaymentMethod(customer.id(), stripePaymentMethodId);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public PaymentMethod getById(Long id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new PaymentMethodNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public PaymentMethod getDefaultForPassenger(Long passengerId) {
        return paymentMethodRepository.findByPassengerIdAndDefaultvalueTrue(passengerId)
                .orElseThrow(() -> new PaymentMethodNotFoundException(
                        "No default payment method found for passenger: %s".formatted(passengerId)
                ));
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> getActiveByPassengerId(Long passengerId) {
        return paymentMethodRepository.findAllByPassengerIdAndActiveTrue(passengerId);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> getAllByPassengerId(Long passengerId) {
        return paymentMethodRepository.findAllByPassengerId(passengerId);
    }

    @Transactional
    public void setDefault(Long passengerId, Long paymentMethodId) {
        PaymentMethod paymentMethod = getById(paymentMethodId);

        if (!paymentMethod.isActive()) {
            throw new PaymentMethodNotActiveException(paymentMethodId);
        }

        paymentMethodRepository.clearDefaultForPassenger(passengerId);

        paymentMethod.setDefaultvalue(true);
        paymentMethodRepository.save(paymentMethod);

        stripeService.setDefaultPaymentMethod(
                paymentMethod.getStripeCustomerId(),
                paymentMethod.getStripePaymentMethodId()
        );
    }

    @Transactional
    public void deactivate(Long passengerId, Long paymentMethodId) {
        PaymentMethod paymentMethod = getById(paymentMethodId);

        if (paymentMethod.isDefaultvalue()) {
            throw new DefaultPaymentMethodException(passengerId);
        }

        stripeService.detachPaymentMethod(paymentMethod.getStripePaymentMethodId());


        paymentMethodRepository.deactivateById(paymentMethodId);
        paymentMethodRepository.flush();

        log.info("Карта деактивирована локально (Stripe пропущен)", paymentMethodId);
    }
}