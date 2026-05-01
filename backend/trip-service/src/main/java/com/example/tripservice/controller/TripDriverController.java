package com.example.tripservice.controller;

import com.example.tripservice.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripDriverController {

    private final TripService tripService;

    
}
