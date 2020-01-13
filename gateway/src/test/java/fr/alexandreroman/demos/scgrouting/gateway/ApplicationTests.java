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

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("tests")
class ApplicationTests {
    @Autowired
    private WebTestClient client;
    private static WireMockServer serverA;
    private static WireMockServer serverB;

    @BeforeAll
    static void setup() {
        serverA = new WireMockServer(9000);
        serverA.start();
        serverB = new WireMockServer(9001);
        serverB.start();
    }

    @BeforeEach
    void init() {
        serverA.resetAll();
        serverB.resetAll();
    }

    @AfterAll
    static void dispose() {
        if (serverA != null) {
            serverA.shutdown();
            serverA = null;
        }
        if (serverB != null) {
            serverB.shutdown();
            serverB = null;
        }
    }

    @Test
    void contextLoads() {
    }

    @Test
    void testRouting() {
        serverA.stubFor(get(urlEqualTo("/hello")).willReturn(aResponse().withBody("A")));
        serverA.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withBody("Root A")));
        serverB.stubFor(get(urlEqualTo("/hello")).willReturn(aResponse().withBody("B")));
        serverB.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withBody("Root B")));

        client.get().uri("/service/hello").exchange().expectBody(String.class).isEqualTo("A");
        client.get().uri("/service").exchange().expectBody(String.class).isEqualTo("Root A");
        client.get().uri("/flip").exchange().expectBody(String.class).isEqualTo("Service set to B\n");
        client.get().uri("/service/hello").exchange().expectBody(String.class).isEqualTo("B");
        client.get().uri("/service").exchange().expectBody(String.class).isEqualTo("Root B");
    }
}
