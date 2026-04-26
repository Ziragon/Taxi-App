package com.example.tripservice.client;

import com.example.tripservice.dto.response.OsrmResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "osrm", url = "${osrm.base-url}")
public interface OsrmClient {

    @GetMapping("/route/v1/driving/{coords}")
    OsrmResponse getRoute(@PathVariable String coords,
                          @RequestParam String overview);
}
