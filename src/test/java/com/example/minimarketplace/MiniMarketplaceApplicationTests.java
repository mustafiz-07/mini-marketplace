package com.example.minimarketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "DB_URL=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "DB_USERNAME=sa",
    "DB_PASSWORD=",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
class MiniMarketplaceApplicationTests {

    @Test
    void contextLoads() {
    }

}
