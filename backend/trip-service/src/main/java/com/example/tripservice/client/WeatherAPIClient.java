package com.example.tripservice.client;

import com.example.tripservice.dto.response.WeatherResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "weather", url = "https://api.weatherapi.com/v1")
public interface WeatherAPIClient {

    @GetMapping("/current.json")
    WeatherResponse getWeather(@RequestParam("key") String key,
                               @RequestParam("q") String query,
                               @RequestParam("lang") String lang);
}
