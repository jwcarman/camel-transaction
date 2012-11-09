package com.carmanconsulting.camel;

import org.apache.activemq.camel.component.ActiveMQConfiguration;

/**
 * @author James Carman
 */
public class TestWithJpaFlushAndNonTransactedJmsComponent extends TestWithJpaFlush
{
    @Override
    protected ActiveMQConfiguration createActiveMQConfiguration()
    {
        ActiveMQConfiguration config = super.createActiveMQConfiguration();
        config.setTransactionManager(null);
        config.setTransacted(false);
        return config;
    }
}
