package com.example.tds.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.tds.dto.responses.GoogleMapsRouteResponse;
import com.example.tds.dto.responses.GoogleMapsApiResponse;
import com.example.tds.dto.responses.RouteInstructionsDto;
import com.example.tds.exception.BadRequestException;
import org.springframework.http.HttpStatusCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleMapsRoutingService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern DURATION_SECONDS_PATTERN = Pattern.compile("(\\d+)s");

    @Qualifier("googleMapsWebClient")
    private final WebClient webClient;

    public GoogleMapsRouteResponse getRoute(double startLng, double startLat, double endLng, double endLat) {
        Map<String, Object> requestBody = buildRequestBody(startLng, startLat, endLng, endLat);

        GoogleMapsApiResponse apiResponse = webClient.post()
                .uri("/directions/v2:computeRoutes")
                .header("X-Goog-FieldMask",
                        "routes.duration,routes.distanceMeters,routes.polyline," +
                        "routes.legs.steps.distanceMeters," +
                        "routes.legs.steps.staticDuration,routes.legs.steps.polyline," +
                        "routes.legs.steps.navigationInstruction")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                    response.bodyToMono(String.class)
                    .defaultIfEmpty("No response body")
                    .flatMap(body -> {
                        log.error("Google Maps Routes API Bad Request: {} | Body: {}", response.statusCode(), body);
                        return Mono.error(new BadRequestException("Could not calculate route for given coordinates"));
                    })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                    response.bodyToMono(String.class)
                        .defaultIfEmpty("No response body")
                        .flatMap(body -> {
                            log.error("Google Maps Routes API Server Error: {} | Body: {}", response.statusCode(), body);
                            return Mono.error(new RuntimeException("Routing service is temporarily unavailable"));
                        })
                )
                .bodyToMono(GoogleMapsApiResponse.class)
                .block();

        return mapToRouteResponse(apiResponse);
    }

    private Map<String, Object> buildRequestBody(double startLng, double startLat, double endLng, double endLat) {
        return Map.of(
                "origin", Map.of(
                        "location", Map.of(
                                "latLng", Map.of(
                                        "latitude", startLat,
                                        "longitude", startLng
                                )
                        )
                ),
                "destination", Map.of(
                        "location", Map.of(
                                "latLng", Map.of(
                                        "latitude", endLat,
                                        "longitude", endLng
                                )
                        )
                ),
                "travelMode", "TWO_WHEELER",
                "routingPreference", "TRAFFIC_AWARE",
                "departureTime", java.time.Instant.now().plusSeconds(60).toString(),
                "computeAlternativeRoutes", false,
                "languageCode", "en-US"
        );
    }

    private GoogleMapsRouteResponse mapToRouteResponse(GoogleMapsApiResponse apiResponse) {
        if (apiResponse == null || apiResponse.getRoutes() == null || apiResponse.getRoutes().isEmpty()) {
            return new GoogleMapsRouteResponse();
        }

        GoogleMapsApiResponse.Route route = apiResponse.getRoutes().get(0);
        GoogleMapsApiResponse.Leg leg = (route.getLegs() != null && !route.getLegs().isEmpty())
                ? route.getLegs().get(0) : null;

        long durationInSeconds = parseDurationSeconds(route.getDuration());

        String encodedPolyline = (route.getPolyline() != null)
                ? route.getPolyline().getEncodedPolyline() : "";

        List<RouteInstructionsDto> instructions = new ArrayList<>();
        if (leg != null && leg.getSteps() != null) {
            instructions = leg.getSteps().stream()
                    .map(step -> {
                        String instruction = "";
                        if (step.getNavigationInstruction() != null) {
                            instruction = stripHtml(step.getNavigationInstruction().getInstructions());
                        }
                        String streetName = "";
                        if (step.getNavigationInstruction() != null
                                && step.getNavigationInstruction().getManeuver() != null) {
                            streetName = step.getNavigationInstruction().getManeuver();
                        }
                        return RouteInstructionsDto.builder()
                                .instruction(instruction)
                                .distance(step.getDistanceMeters())
                                .timeInSeconds(parseDurationSeconds(step.getStaticDuration()))
                                .streetName(streetName)
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        GoogleMapsRouteResponse response = new GoogleMapsRouteResponse();
        response.setDistanceInMeters(route.getDistanceMeters());
        response.setDurationInSeconds(durationInSeconds);
        response.setEncodedPolyline(encodedPolyline);
        response.setInstructions(instructions);
        return response;
    }

    private long parseDurationSeconds(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 0;
        }
        Matcher matcher = DURATION_SECONDS_PATTERN.matcher(duration);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        try {
            return Long.parseLong(duration);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        Matcher matcher = HTML_TAG_PATTERN.matcher(html);
        return matcher.replaceAll("").trim();
    }
}
