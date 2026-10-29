package com.mctait;

import com.mctait.cli.JobSearchRunner;
import com.mctait.service.JobAggregatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class MainTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context.getBean(JobAggregatorService.class));
    }

    @Test
    void cliRunnerIsNotActiveInTestProfile() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(JobSearchRunner.class));
    }
}
