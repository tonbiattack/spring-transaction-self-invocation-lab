package com.example.transactionlab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransactionProxyBoundaryIntegrationTest {

    @Autowired
    private TransactionalBalanceWriter transactionalBalanceWriter;

    @Test
    void トランザクション境界は別BeanのSpringAopProxyとして公開される() {
        assertThat(AopUtils.isAopProxy(transactionalBalanceWriter))
                .as("@Transactional を持つ別BeanはSpring AOP Proxyで公開されること")
                .isTrue();

        assertThat(AopUtils.getTargetClass(transactionalBalanceWriter))
                .isEqualTo(TransactionalBalanceWriter.class);
    }
}
