package nz.co.westpac.poc.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class RequestLogFilterTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("6d29ceee-e865-49b7-b34f-96f801460f2e");

    private final RequestLogFilter filter = new RequestLogFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void putsTheCustomerIdInTheLoggingContextWhileTheRequestRuns() throws Exception {
        authenticateAs(CUSTOMER_ID.toString());
        AtomicReference<String> customerIdDuringRequest = new AtomicReference<>();

        filter.doFilter(request("request-1"), new MockHttpServletResponse(),
                (request, response) -> customerIdDuringRequest.set(MDC.get("customerId")));

        assertThat(customerIdDuringRequest.get()).isEqualTo(CUSTOMER_ID.toString());
    }

    @Test
    void leavesTheCustomerIdEmptyForAnUnauthenticatedRequest() throws Exception {
        AtomicReference<String> customerIdDuringRequest = new AtomicReference<>("not-set");

        filter.doFilter(request("request-2"), new MockHttpServletResponse(),
                (request, response) -> customerIdDuringRequest.set(MDC.get("customerId")));

        assertThat(customerIdDuringRequest.get()).isNull();
    }

    @Test
    void leavesTheCustomerIdEmptyWhenTheTokenSubjectIsNotACustomerId() throws Exception {
        authenticateAs("not-a-uuid");
        AtomicReference<String> customerIdDuringRequest = new AtomicReference<>("not-set");

        filter.doFilter(request("request-3"), new MockHttpServletResponse(),
                (request, response) -> customerIdDuringRequest.set(MDC.get("customerId")));

        assertThat(customerIdDuringRequest.get()).isNull();
    }

    @Test
    void doesNotFailTheRequestWhenTheLoggingContextCannotBeResolved() {
        authenticateAs(CUSTOMER_ID.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatCode(() -> filter.doFilter(request("request-4"), response,
                (request, res) -> res.setContentType("application/json")))
                .doesNotThrowAnyException();
    }

    private static MockHttpServletRequest request(String requestId) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/accounts");
        request.setAttribute(RequestIdFilter.ATTRIBUTE, requestId);
        return request;
    }

    private static void authenticateAs(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
