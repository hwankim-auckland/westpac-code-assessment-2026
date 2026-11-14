package nz.co.westpac.poc.service;

import nz.co.westpac.poc.entity.Customer;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * This is currently Mock, as out of scope on customer management.
 * this could come from other service etc.
 */
@Service
public class CustomerService {
    /**
     * This is currenlty mock due to the scope of this practice.
     * @param customerId
     * @return optional customer.
     */
    public Optional<Customer> getCustomer(UUID customerId){
        return Optional.of(
                Customer.builder()
                        .customerId(customerId)
                        .customerName("Hwan Kim") // :-) every customer is me for now.
                        .build()
        );
    }
}
