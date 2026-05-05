package com.example.tripservice.dto.data;

import com.example.shared.dto.data.DriverLocationDto;
import com.example.tripservice.entity.Tariff;

import java.util.List;

public record TripExternalDto(

        RouteDto route,

        WeatherDto weather,

        List<DriverLocationDto> drivers,

        List<Tariff> tariffs
) {}
