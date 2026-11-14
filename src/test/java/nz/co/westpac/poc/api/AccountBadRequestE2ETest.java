package nz.co.westpac.poc.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import nz.co.westpac.poc.support.TestContainerBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountBadRequestE2ETest extends TestContainerBase {

    @BeforeEach
    void seedBlocklist() {
        redisTemplate.opsForSet().add(BLOCKLIST_KEY, "admin", "idiot");
    }

    @Test
    void rejectsNicknameShorterThanFiveCharacters() throws Exception {
        HttpResponse<String> response = createAccount(UUID.randomUUID(), "abcd");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat((String) body(response).get("detail")).contains("between 5 and 30");
    }

    @Test
    void rejectsNicknameLongerThanThirtyCharacters() throws Exception {
        HttpResponse<String> response = createAccount(UUID.randomUUID(), "a".repeat(31));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat((String) body(response).get("detail")).contains("between 5 and 30");
    }

    @Test
    void rejectsOffensiveNickname() throws Exception {
        HttpResponse<String> response = createAccount(UUID.randomUUID(), "ADMIN");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat((String) body(response).get("detail")).contains("not allowed");
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/accounts"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token(UUID.randomUUID()))
                .POST(HttpRequest.BodyPublishers.ofString("{\"accountNickname\":"))
                .build();

        HttpResponse<String> response = send(request);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat((String) body(response).get("title")).isEqualTo("Invalid request");
    }

    @Test
    void rejectsTokenSubjectThatIsNotACustomerId() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/accounts"))
                .header("Authorization", "Bearer " + token("not-a-uuid"))
                .GET()
                .build();

        HttpResponse<String> response = send(request);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat((String) body(response).get("detail")).contains("customer UUID");
    }
}
