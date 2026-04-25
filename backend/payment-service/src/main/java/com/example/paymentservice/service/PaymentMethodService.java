package com.example.paymentservice.service;

import com.example.paymentservice.entity.PaymentMethod;
import com.example.paymentservice.exception.DefaultPaymentMethodException;
import com.example.paymentservice.exception.DuplicatePaymentMethodException;
import com.example.paymentservice.exception.PaymentMethodNotActiveException;
import com.example.paymentservice.exception.PaymentMethodNotFoundException;
import com.example.paymentservice.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional
    public PaymentMethod addPaymentMethod(Long passengerId,
                                          String stripeCustomerId,
                                          String stripePaymentMethodId,
                                          String cardBrand,
                                          String lastFour) {
        if (paymentMethodRepository.existsByPassengerIdAndStripePaymentMethodId(passengerId, stripePaymentMethodId)) {
            throw new DuplicatePaymentMethodException(passengerId, lastFour);
        }

        boolean isFirst = paymentMethodRepository.findAllByPassengerId(passengerId).isEmpty();

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .passengerId(passengerId)
                .stripeCustomerId(stripeCustomerId)
                .stripePaymentMethodId(stripePaymentMethodId)
                .cardBrand(cardBrand)
                .lastFour(lastFour)
                .defaultvalue(isFirst)
                .active(true)
                .build();

        return paymentMethodRepository.save(paymentMethod);
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
    }

    @Transactional
    public void deactivate(Long passengerId, Long paymentMethodId) {
        PaymentMethod paymentMethod = getById(paymentMethodId);

        if (paymentMethod.isDefaultvalue()) {
            throw new DefaultPaymentMethodException(passengerId);
        }

        paymentMethodRepository.deactivateById(paymentMethodId);
    }
}
