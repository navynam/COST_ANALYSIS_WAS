package com.costanalysis.global.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Mock 프로파일 전용 빈 설정.
 * 원래 WebClientConfig에서 제공하던 ObjectMapper,
 * JPA 제거로 인해 필요한 no-op TransactionManager 등을 제공한다.
 */
@Configuration
@Profile("mock")
public class MockBeanConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * JPA 자동 구성을 제외했으므로 @Transactional 프록시가 동작하도록
     * no-op TransactionManager를 등록한다.
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {
                // no-op
            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {
                // no-op
            }
        };
    }
}
