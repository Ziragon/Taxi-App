package com.example.tripservice.dto.response;

import com.example.tripservice.dto.data.RouteDto;

import java.util.List;

public record OsrmResponse(

        String code,

        List<RouteDto> routes
) {}
