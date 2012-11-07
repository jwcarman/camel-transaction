package com.carmanconsulting.camel;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.SQLException;

/**
 * @author James Carman
 */
public abstract class AbstractRouteBuilderTest extends CamelTestSupport
{
    private SimpleRegistry registry;
    private PlatformTransactionManager transactionManager;
    private BasicDataSource dataSource;

    protected CamelContext createCamelContext() throws Exception
    {
        final CamelContext context = new DefaultCamelContext(registry);

        final ActiveMQConfiguration activeMQConfig = new ActiveMQConfiguration();
        activeMQConfig.setTransacted(true);
        activeMQConfig.setBrokerURL("vm://" + getClass().getSimpleName() + "?broker.persistent=false&broker.useJmx=false");

        final ActiveMQComponent jms = new ActiveMQComponent(activeMQConfig);
        jms.setTransacted(true);
        jms.setTransactionManager(transactionManager);
        context.addComponent("jms", jms);
        return context;
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
        final BasicDataSource dataSource = createDataSource();
        registry.put("dataSource", dataSource);
        registry.put("transactionManager", transactionManager = new DataSourceTransactionManager(dataSource));
        doBindings(registry);
    }

    private BasicDataSource createDataSource()
    {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:h2:mem:" + getClass().getSimpleName());
        dataSource.setMinIdle(1);
        dataSource.setInitialSize(1);

        log.info("Creating test table...");
        new JdbcTemplate(dataSource).execute("create table test_table (id integer not null identity, test_value varchar(255), primary key(id))");
        return dataSource;
    }

    @After
    public void closeDataSource() throws SQLException
    {
        dataSource.close();
    }

    @After
    public void checkMockEndpointAssertions() throws InterruptedException
    {
        assertMockEndpointsSatisfied();
    }

    protected JdbcTemplate createJdbcTemplate()
    {
        return new JdbcTemplate(dataSource);
    }

    protected void assertTestTableCount(int count)
    {
        assertEquals(count, createJdbcTemplate().queryForInt("select count(*) from test_table"));
    }

    protected void assertTestTableEmpty()
    {
        assertTestTableCount(0);
    }

    protected Exchange receiveFromDeadLetterQueue()
    {
        return consumer.receive("jms:queue:ActiveMQ.DLQ", 2000);
    }
}
