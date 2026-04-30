package com.example.tripservice.dto.data;

import java.math.BigDecimal;

public record AddressDto(

        String address,

        BigDecimal latitude,

        BigDecimal longitude
) {}
