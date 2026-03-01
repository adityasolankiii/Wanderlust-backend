package com.wanderlust.wanderlust.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MapboxService {

    @Value("${mapbox.access-token}")
    private String accessToken;

    private static final String GEOCODING_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/";

    /**
     * Geocode a location string to coordinates.
     * Returns [longitude, latitude] or null if not found.
     */
    @SuppressWarnings("unchecked")
    public double[] geocode(String location) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(GEOCODING_URL + "{query}.json")
                    .queryParam("access_token", accessToken)
                    .queryParam("limit", 1)
                    .buildAndExpand(location)
                    .toUriString();

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                List<Map<String, Object>> features = (List<Map<String, Object>>) response.get("features");
                if (features != null && !features.isEmpty()) {
                    List<Double> center = (List<Double>) features.get(0).get("center");
                    if (center != null && center.size() >= 2) {
                        return new double[]{center.get(0), center.get(1)}; // [lng, lat]
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Geocoding failed for location: {}. Error: {}", location, e.getMessage());
        }
        return null;
    }
}
