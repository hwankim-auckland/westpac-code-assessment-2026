package nz.co.westpac.poc.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findAllByCustomerId(UUID customerId);

    int countByCustomerId(UUID customerId);
}
