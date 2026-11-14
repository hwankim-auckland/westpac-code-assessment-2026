package nz.co.westpac.poc.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import nz.co.westpac.poc.support.TestContainerBase;
import org.junit.jupiter.api.Test;

class AccountGetE2ETest extends TestContainerBase {

    @Test
    void getsAccountAndCachesItInRedis() throws Exception {
        UUID customerId = UUID.randomUUID();
        String accountId = (String) body(createAccount(customerId, "holiday")).get("accountId");

        HttpResponse<String> first = getAccount(customerId, accountId);

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(body(first)).containsEntry("accountId", accountId);
        assertThat(redisTemplate.hasKey("accounts:" + accountId)).isTrue();

        HttpResponse<String> second = getAccount(customerId, accountId);

        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.body()).isEqualTo(first.body());
    }

    @Test
    void listsOnlyTheTokenSubjectsAccounts() throws Exception {
        UUID customerId = UUID.randomUUID();
        String firstId = (String) body(createAccount(customerId, "holiday1")).get("accountId");
        String secondId = (String) body(createAccount(customerId, "holiday2")).get("accountId");
        createAccount(UUID.randomUUID(), "business");

        HttpResponse<String> response = getAccounts(customerId);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(accountIds(response.body())).containsExactlyInAnyOrder(firstId, secondId);
    }

    @Test
    void returnsNotFoundForUnknownAccount() throws Exception {
        HttpResponse<String> response = getAccount(UUID.randomUUID(), "0".repeat(64));

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat((String) body(response).get("title")).isEqualTo("Savings bank account not found");
    }

    @Test
    void rejectsRequestsWithoutAToken() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/v1/accounts/" + "0".repeat(64)))
                .GET()
                .build());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void rejectsRequestsWithAnInvalidToken() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/v1/accounts"))
                .header("Authorization", "Bearer not-a-jwt")
                .GET()
                .build());

        assertThat(response.statusCode()).isEqualTo(401);
    }
}
