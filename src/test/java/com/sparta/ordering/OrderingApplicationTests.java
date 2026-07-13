package com.sparta.ordering;

import com.sparta.ordering.global.config.ContainerInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test-security")
@ContextConfiguration(initializers = ContainerInitializer.class)
class OrderingApplicationTests {

    @Test
    void contextLoads() {
    }

}
