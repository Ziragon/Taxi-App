package com.example.tripservice.messaging;

import com.example.shared.dto.event.TripOfferEvent;
import com.example.tripservice.dto.event.OfferExpiredEvent;
import com.example.tripservice.entity.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.tripservice.config.RabbitMQConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripOfferPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOffer(Trip trip, Long driverId) {
        TripOfferEvent event = new TripOfferEvent(
                trip.getId(),
                driverId,
                trip.getPassengerId(),
                trip.getOriginAddress(),
                trip.getOriginLat(),
                trip.getOriginLng(),
                trip.getDestinationAddress(),
                trip.getDestinationLat(),
                trip.getDestinationLng(),
                trip.getPrice(),
                trip.getDistanceKm(),
                trip.getDurationMin(),
                trip.getTripClass()
        );

        log.info("Publishing TripOfferEvent: tripId={}, driverId={}", trip.getId(), driverId);

        rabbitTemplate.convertAndSend(
                NOTIFICATION_EXCHANGE,
                TRIP_OFFER_ROUTING_KEY,
                event
        );
    }

    public void publishOfferExpired(Long tripId, Long driverId) {
        // Можно расширить если нужно больше данных
        log.info("Publishing OfferExpired: tripId={}, driverId={}", tripId, driverId);

        OfferExpiredEvent event = new OfferExpiredEvent(tripId, driverId);

        rabbitTemplate.convertAndSend(
                NOTIFICATION_EXCHANGE,
                TRIP_OFFER_EXPIRED_ROUTING_KEY,
                event
        );
    }
}