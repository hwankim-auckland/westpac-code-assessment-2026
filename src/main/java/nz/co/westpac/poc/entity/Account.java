package nz.co.westpac.poc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

import lombok.*;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_accounts_account_number",
                columnNames = "account_number"))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account {

    @Id
    @Setter(AccessLevel.NONE)
    @Column(name = "id", nullable = false, updatable = false, length = 64)
    private String id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID customerId;

    @Column(name = "account_number", nullable = false, updatable = false, length = 20)
    @Setter(AccessLevel.NONE)
    private String accountNumber;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "account_nickname", length = 50)
    private String accountNickname;
}
