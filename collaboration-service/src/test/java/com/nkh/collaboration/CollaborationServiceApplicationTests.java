package com.nkh.collaboration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "app.redis.listener-enabled=false")
class CollaborationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
