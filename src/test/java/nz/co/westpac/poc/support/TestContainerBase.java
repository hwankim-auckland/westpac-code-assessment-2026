package nz.co.westpac.poc.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class TestContainerBase {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.7")
            .withDatabaseName("accounts_db")
            .withUsername("local_admin")
            .withPassword("some_strong_password");

    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.10.3")
            .withExposedPorts(6379);

    protected static final String BLOCKLIST_KEY = "not_allowed_nick_name_lists";

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    protected final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    protected int port;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtEncoder jwtEncoder;

    protected URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    protected String token(UUID customerId) {
        return token(customerId.toString());
    }

    protected String token(String subject) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("test")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    protected HttpResponse<String> createAccount(UUID customerId, String nickname) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (nickname != null) {
            payload.put("accountNickname", nickname);
        }

        return send(HttpRequest.newBuilder(uri("/api/v1/accounts"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token(customerId))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build());
    }

    protected HttpResponse<String> getAccounts(UUID customerId) throws Exception {
        return send(HttpRequest.newBuilder(uri("/api/v1/accounts"))
                .header("Authorization", "Bearer " + token(customerId))
                .GET()
                .build());
    }

    protected HttpResponse<String> getAccount(UUID customerId, String accountId) throws Exception {
        return send(HttpRequest.newBuilder(uri("/api/v1/accounts/" + accountId))
                .header("Authorization", "Bearer " + token(customerId))
                .GET()
                .build());
    }

    protected HttpResponse<String> send(HttpRequest request) throws Exception {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> body(HttpResponse<String> response) throws Exception {
        return objectMapper.readValue(response.body(), Map.class);
    }

    @SuppressWarnings("unchecked")
    protected List<String> accountIds(String json) throws Exception {
        List<Map<String, Object>> accounts = objectMapper.readValue(json, List.class);
        return accounts.stream().map(account -> (String) account.get("accountId")).toList();
    }

}
