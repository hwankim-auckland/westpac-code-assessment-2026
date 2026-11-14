package nz.co.westpac.poc;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import nz.co.westpac.poc.support.TestContainerBase;
import org.junit.jupiter.api.Test;

class ApplicationTest extends TestContainerBase {

    @Test
    void healthEndpointIsUp() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/actuator/health"))
                .GET()
                .build());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }
}
