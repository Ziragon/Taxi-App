package com.example.tripservice.dto.data;

import com.example.tripservice.entity.enums.DriverReply;

public record DriverResponseDto (

        Long driverId, DriverReply type
) {
    public static DriverResponseDto accept(Long driverId) {
        return new DriverResponseDto(driverId, DriverReply.ACCEPT);
    }

    public static DriverResponseDto reject() {
        return new DriverResponseDto(null, DriverReply.REJECT);
    }

    public static DriverResponseDto timeout() {
        return new DriverResponseDto(null, DriverReply.TIMEOUT);
    }

    public static DriverResponseDto cancelled() {
        return new DriverResponseDto(null, DriverReply.CANCELLED);
    }
}
