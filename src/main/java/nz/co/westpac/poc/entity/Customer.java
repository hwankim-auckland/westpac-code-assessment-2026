package nz.co.westpac.poc.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class Customer {
    private UUID customerId;
    private String customerName;
}
