package nz.co.westpac.poc.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import nz.co.westpac.poc.support.TestContainerBase;
import org.junit.jupiter.api.Test;

class RequestIdE2ETest extends TestContainerBase {

    @Test
    void echoesProvidedRequestIdInTheResponseHeader() throws Exception {
        UUID customerId = UUID.randomUUID();
        String requestId = "test-" + UUID.randomUUID();

        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/v1/accounts"))
                .header("X-Request-Id", requestId)
                .header("Authorization", "Bearer " + token(customerId))
                .GET()
                .build());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Request-Id")).hasValue(requestId);
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissing() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/v1/accounts/" + "0".repeat(64)))
                .GET()
                .build());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("X-Request-Id"))
                .hasValueSatisfying(value -> assertThat(value).matches("[0-9a-f-]{36}"));
    }
}
