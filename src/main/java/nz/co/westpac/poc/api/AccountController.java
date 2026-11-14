package nz.co.westpac.poc.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import nz.co.westpac.poc.api.dto.AccountResponse;
import nz.co.westpac.poc.api.dto.CreateAccountRequest;
import nz.co.westpac.poc.entity.validator.InvalidAccountException;
import nz.co.westpac.poc.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accountService.create(customerId(jwt), request);
        return ResponseEntity
                .created(URI.create("/api/v1/accounts/" + account.accountId()))
                .body(account);
    }

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getByCustomer(customerId(jwt));
    }

    @GetMapping("/{accountId}")
    public AccountResponse get(@PathVariable String accountId) {
        return accountService.get(accountId);
    }

    private static UUID customerId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException ex) {
            throw new InvalidAccountException("Token subject must be a customer UUID");
        }
    }
}
