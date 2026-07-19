package com.example.tds.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.tds.dto.responses.GraphHopperRouteResponse;
import com.example.tds.dto.responses.GraphHopperApiResponse;
import com.example.tds.dto.responses.RouteInstructionsDto;
import com.example.tds.exception.BadRequestException;
import org.springframework.http.HttpStatusCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphHopperService {

    @Qualifier("graphHopperWebClient")
    private final WebClient webClient;

    public GraphHopperRouteResponse getRoute(double startLng, double startLat, double endLng, double endLat) {
        return getRoute(startLng, startLat, endLng, endLat, "motorcycle");
    }

    public GraphHopperRouteResponse getRoute(
            double startLng,
            double startLat,
            double endLng,
            double endLat,
            String profile
    ) {
        GraphHopperApiResponse apiResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route")
                        .queryParam("point", startLat + "," + startLng)
                        .queryParam("point", endLat + "," + endLng)
                        .queryParam("profile", profile)
                        .build()
                ).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    log.error("GraphHopper Bad Request: {}", response.statusCode());
                    return Mono.error(new BadRequestException("Could not calculate route for given coordinates"));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.error("GraphHopper Server Error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Routing service is temporarily unavailable"));
                })
                .bodyToMono(GraphHopperApiResponse.class)
                .block();

        if (apiResponse != null && apiResponse.getPaths() != null && !apiResponse.getPaths().isEmpty()) {
            GraphHopperApiResponse.GraphHopperPath path = apiResponse.getPaths().get(0);
            return new GraphHopperRouteResponse(
                    path.getDistance(),
                    path.getTime() / 1000,
                    path.getPoints(),
                    path.getInstructions() != null ? path.getInstructions().stream()
                            .map(inst -> RouteInstructionsDto.builder()
                                    .instruction(inst.getText())
                                    .distance(inst.getDistance())
                                    .timeInSeconds(inst.getTime() / 1000)
                                    .streetName(inst.getStreetName())
                                    .build())
                            .collect(Collectors.toList()) : new ArrayList<>()
            );
        }

        return new GraphHopperRouteResponse();
    }
}
