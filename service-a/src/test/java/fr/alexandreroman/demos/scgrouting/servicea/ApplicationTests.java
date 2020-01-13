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

package fr.alexandreroman.demos.scgrouting.servicea;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@Import(SecurityConfig.class)
@WebFluxTest
class ApplicationTests {
    @Autowired
    private WebTestClient client;

    @Test
    void contextLoads() {
    }

    @Test
    void testIndex() {
        client.get().uri("/").exchange().expectBody(String.class).isEqualTo("Welcome to Service A\n");
    }

    @Test
    void testHello() {
        client.get().uri("/hello").exchange().expectBody(String.class).isEqualTo("Hello world\n");
    }
}
