/*
 * Copyright (c) 2020 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.alexandreroman.demos.scgrouting.gateway;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableConfigurationProperties
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Configuration
class GatewayConfig {
    @Bean
    RouteLocator routes(RouteLocatorBuilder builder, CustomRoutingFilter filter) {
        return builder.routes()
                .route(r -> r.path("/service/**", "/service")
                        .filters(f -> f
                                // Rewrite the URL part: remove "/service" prefix.
                                .rewritePath("/service/(?<segment>.*)", "/${segment}")
                                .rewritePath("/service", "/")
                                .filter(filter))
                        // "uri" parameter is required, but we cannot use a value here
                        // since we dynamically set the route in the filter.
                        .uri("no://go")
                        .id("service"))
                .build();
    }
}

enum ServiceTarget {
    A, B
}

@Component
@RequiredArgsConstructor
class CustomRoutingFilter implements GatewayFilter, Ordered {
    private final AppConfig config;
    private final ServiceTargetRepository repo;
    private final Map<ServiceTarget, Route> routes = new HashMap<>(2);

    @PostConstruct
    private void init() {
        // Initialize service routes.
        routes.put(ServiceTarget.A,
                Route.async().id("service-a").uri(config.getServiceA()).predicate(p -> true).build());
        routes.put(ServiceTarget.B,
                Route.async().id("service-b").uri(config.getServiceB()).predicate(p -> true).build());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final Route route = routes.get(repo.target);
        // Route input request to the target service using attribute GATEWAY_ROUTE_ATTR,
        // which is read by RouteToRequestUrlFilter to route the request.
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Filter order is required since we set an attribute which is read by an other filter.
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER - 1;
    }
}

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
class AppConfig {
    private String serviceA;
    private String serviceB;
}

@Component
class ServiceTargetRepository {
    ServiceTarget target = ServiceTarget.A;
}

@RestController
@RequiredArgsConstructor
class FlipController {
    private final ServiceTargetRepository repo;

    @GetMapping(value = "/flip", produces = MediaType.TEXT_PLAIN_VALUE)
    String flip() {
        repo.target = repo.target.equals(ServiceTarget.A) ? ServiceTarget.B : ServiceTarget.A;
        return "Service set to " + repo.target + "\n";
    }
}

@RestController
class IndexController {
    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    Mono<String> index(UriComponentsBuilder uriBuilder) {
        final var flipUrl = uriBuilder.cloneBuilder().path("/flip").toUriString();
        final var serviceUrl = uriBuilder.cloneBuilder().path("/service").toUriString();
        final var helloUrl = uriBuilder.cloneBuilder().path("/service/hello").toUriString();
        return Mono.just(
                "Use these endpoints:\n"
                        + " - " + serviceUrl + ": show current service\n"
                        + " - " + flipUrl + ": flip service (from service A to service B and vice-versa)\n"
                        + " - " + helloUrl + ": call an API from current service\n"
        );
    }
}
