package nz.co.westpac.poc.api.dto;

import java.util.UUID;

public record AccountResponse(
        String accountId,
        String accountNumber,
        String customerName,
        String accountNickname) {
}
