package com.carmanconsulting.camel;

import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.springframework.jms.connection.JmsTransactionManager;

/**
 * @author James Carman
 */
public class TestWithNoJpaFlushAndJmsTransactions extends TestWithNoJpaFlush
{
    @Override
    protected ActiveMQConfiguration createActiveMQConfiguration()
    {
        ActiveMQConfiguration config = super.createActiveMQConfiguration();
        config.setTransactionManager(new JmsTransactionManager(config.getConnectionFactory()));
        return config;
    }
}
