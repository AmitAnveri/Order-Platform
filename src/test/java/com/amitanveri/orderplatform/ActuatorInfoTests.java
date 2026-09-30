package com.amitanveri.orderplatform;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.endpoints.web.exposure.include=info")
class ActuatorInfoTests {

    @LocalServerPort
    private int port;

    @Test
    void infoIncludesGeneratedBuildMetadata() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/actuator/info"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            JsonNode build = JsonMapper.builder().build().readTree(response.body()).path("build");
            assertThat(build.path("name").stringValue()).isEqualTo("order-platform");
            assertThat(build.path("artifact").stringValue()).isEqualTo("order-platform");
            assertThat(build.path("group").stringValue()).isEqualTo("com.amitanveri");
            assertThat(build.path("version").stringValue()).isNotBlank();
            assertThat(build.path("time").stringValue()).isNotBlank();
        }
    }
}
