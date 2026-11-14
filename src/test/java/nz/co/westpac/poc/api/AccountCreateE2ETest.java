package nz.co.westpac.poc.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import java.net.http.HttpResponse;
import nz.co.westpac.poc.support.TestContainerBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountCreateE2ETest extends TestContainerBase {

    @BeforeEach
    void seedBlocklist() {
        redisTemplate.opsForSet().add(BLOCKLIST_KEY, "admin", "idiot");
    }

    @Test
    void createsSavingsAccountForTheTokenSubject() throws Exception {
        UUID customerId = UUID.randomUUID();

        HttpResponse<String> created = createAccount(customerId, "holiday");
        Map<String, Object> body = body(created);

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.headers().firstValue("Location")).isPresent();
        assertThat(body)
                .containsEntry("customerName", "Hwan Kim")
                .containsEntry("accountNickname", "holiday")
                .doesNotContainKey("customerId");
        assertThat((String) body.get("accountId")).matches("[0-9a-f]{64}");
        assertThat((String) body.get("accountNumber")).matches("03 0187 [0-9]{7} [0-9]{3}");
    }

    @Test
    void acceptsAccountWithoutNickname() throws Exception {
        HttpResponse<String> created = createAccount(UUID.randomUUID(), null);

        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(body(created)).containsEntry("accountNickname", null);
    }

    @Test
    void rejectsSixthAccountForTheSameCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();

        for (int i = 0; i < 5; i++) {
            assertThat(createAccount(customerId, "holiday" + i).statusCode()).isEqualTo(201);
        }

        HttpResponse<String> sixth = createAccount(customerId, "holiday6");

        assertThat(sixth.statusCode()).isEqualTo(409);
        assertThat((String) body(sixth).get("detail")).contains("more than 5 savings bank accounts");
    }
}
