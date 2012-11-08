package com.carmanconsulting.camel;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.junit.After;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author James Carman
 */
public abstract class AbstractRouteBuilderTest extends CamelTestSupport
{
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private SimpleRegistry registry;
    protected PlatformTransactionManager transactionManager;
    private BasicDataSource dataSource;

//----------------------------------------------------------------------------------------------------------------------
// Getter/Setter Methods
//----------------------------------------------------------------------------------------------------------------------

    protected synchronized DataSource getDataSource()
    {
        if (dataSource == null)
        {
            dataSource = new BasicDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            dataSource.setUrl("jdbc:h2:mem:" + getClass().getSimpleName() + ";TRACE_LEVEL_FILE=4");
            dataSource.setMinIdle(1);
            dataSource.setInitialSize(1);
            dataSource.setMaxActive(1);
            dataSource.setTestOnBorrow(true);
            dataSource.setTestOnReturn(true);
            dataSource.setValidationQuery("select 1");
        }
        return dataSource;
    }

    protected PlatformTransactionManager getTransactionManager()
    {
        if(transactionManager == null)
        {
            transactionManager = new DataSourceTransactionManager(getDataSource());
        }
        return transactionManager;
    }

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------



    @After
    public void checkMockEndpointAssertions() throws InterruptedException
    {
        assertMockEndpointsSatisfied(getTimeoutValue(), getTimeoutUnit());
    }



    @After
    public void closeDataSource() throws SQLException
    {
        log.info("Closing DataSource...");
        dataSource.close();
    }

    protected CamelContext createCamelContext() throws Exception
    {
        final CamelContext context = new DefaultCamelContext(registry);

        context.addComponent("jms", createJmsComponent());


        return context;
    }

    protected ActiveMQComponent createJmsComponent()
    {
        final ActiveMQConfiguration activeMQConfig = new ActiveMQConfiguration();
        activeMQConfig.setTransacted(true);
        activeMQConfig.setBrokerURL("vm://" + getClass().getSimpleName() + "?broker.persistent=false&broker.useJmx=false");

        final ActiveMQComponent jms = new ActiveMQComponent(activeMQConfig);
        jms.setTransacted(true);
        jms.setTransactionManager(getTransactionManager());
        return jms;
    }


    protected JdbcTemplate createJdbcTemplate()
    {
        return new JdbcTemplate(getDataSource());
    }

    protected void doBindings(SimpleRegistry registry)
    {
        // Do nothing
    }

    @Override
    protected final void doPreSetup() throws Exception
    {
        super.doPreSetup();
        registry = new SimpleRegistry();
        registry.put("dataSource", getDataSource());
        registry.put("transactionManager", getTransactionManager());
        doBindings(registry);
    }

    protected Exchange receiveFromDeadLetterQueue()
    {
        return consumer.receive("jms:queue:ActiveMQ.DLQ", 2000);
    }

//----------------------------------------------------------------------------------------------------------------------
// Inner Classes
//----------------------------------------------------------------------------------------------------------------------


    public long getTimeoutValue()
    {
        return 3;
    }

    public TimeUnit getTimeoutUnit()
    {
        return TimeUnit.SECONDS;
    }
}
