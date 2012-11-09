package com.carmanconsulting.camel;

import com.carmanconsulting.camel.entity.MyEntity;
import com.carmanconsulting.camel.jpa.MyEntityRepository;
import com.carmanconsulting.camel.jpa.MyEntityRepositoryPersistWithFlush;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.TimeUnit;

/**
 * @author James Carman
 */
public abstract class AbstractTestCaseTemplate extends AbstractRouteBuilderTest
{
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    public static final String INPUT_QUEUE_URI = "jms:queue:input";
    public static final String OUTPUT_QUEUE_URI = "jms:queue:output";
    public static final String DLC_QUEUE_URI = "jms:queue:dlc";

    @Produce(uri = INPUT_QUEUE_URI)
    private ProducerTemplate input;

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected void configurePersistenceUnit(PersistenceUnitBuilder builder)
    {
        builder.withClass(MyEntity.class);
    }

    protected ActiveMQConfiguration createActiveMQConfiguration()
    {
        final ActiveMQConfiguration activeMQConfig = new ActiveMQConfiguration();
        activeMQConfig.setTransacted(true);
        activeMQConfig.setTransactionManager(getTransactionManager());
        activeMQConfig.setBrokerURL("vm://" + getClass().getSimpleName() + "?broker.persistent=false&broker.useJmx=false");
        return activeMQConfig;
    }

    @Override
    protected JmsComponent createJmsComponent()
    {
        return new ActiveMQComponent(createActiveMQConfiguration());
    }

    @Override
    protected JpaComponent createJpaComponent()
    {
        JpaComponent jpa = new JpaComponent();
        jpa.setEntityManagerFactory(getEntityManagerFactory());
        jpa.setTransactionManager(getTransactionManager());
        return jpa;
    }

    protected MyEntityRepository createRepository()
    {
        return new MyEntityRepositoryPersistWithFlush(getEntityManagerFactory());
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure() throws Exception
            {
                interceptFrom().when(header("JMSRedelivered").isEqualTo("true")).to("jms:queue:dlc").stop();

                from(INPUT_QUEUE_URI).transacted().beanRef("repository", "saveEntity").to("jms:queue:output");
            }
        };
    }

    @Override
    protected PlatformTransactionManager createTransactionManager()
    {
        JpaTransactionManager transactionManager = new JpaTransactionManager(getEntityManagerFactory());
        transactionManager.setEntityManagerFactory(getEntityManagerFactory());
        transactionManager.setDataSource(getDataSource());
        return transactionManager;
    }

    @Override
    protected void doRegistryBindings(SimpleRegistry registry)
    {
        registry.put("repository", createRepository());
    }

    /*@Override
    public String isMockEndpoints()
    {
        return "jms:queue:*";
    }*/

    @Test
    public void testHappyPath() throws Exception
    {
        NotifyBuilder builder = createNotifyBuilder().from(INPUT_QUEUE_URI).whenCompleted(1).wereSentTo(OUTPUT_QUEUE_URI).create();

        final MyEntity test = new MyEntity();
        test.setName("test");
        input.sendBody(test);
        assertTrue(builderMatches(builder));
        assertEquals(1, messageCount(OUTPUT_QUEUE_URI));
        assertEquals(0, messageCount(DLC_QUEUE_URI));
        assertEquals(1, entityCount());
    }

    private boolean builderMatches(NotifyBuilder builder)
    {
        log.info("Awaiting notification {}...", builder);
        boolean matches = builder.matches(5, TimeUnit.SECONDS);
        log.info("Notification received? {}", matches);
        return matches;
    }

    private int messageCount(String uri) throws Exception
    {
        log.info("Obtaining message count for URI {}...", uri);
        PollingConsumer consumer = getContext().getEndpoint(uri).createPollingConsumer();
        int count = 0;
        while (consumer.receive(500) != null)
        {
            count++;
        }
        log.info("Received {} messages from URI {}.", count, uri);
        return count;
    }

    private int entityCount()
    {
        return createJdbcTemplate().queryForInt("select count(*) from test_entity");
    }

    @Test
    public void testWithLengthConstraintViolation() throws Exception
    {
        NotifyBuilder builder = createNotifyBuilder().from(INPUT_QUEUE_URI).whenFailed(1).and().whenCompleted(1).wereSentTo(DLC_QUEUE_URI).create();
        final MyEntity test = new MyEntity();
        test.setName(StringUtils.repeat("X", 256));
        input.sendBody(test);
        assertTrue(builderMatches(builder));
        assertEquals(0, messageCount(OUTPUT_QUEUE_URI));
        assertEquals(1, messageCount(DLC_QUEUE_URI));
        assertEquals(0, entityCount());
    }

    @Test
    public void testWithNullConstraintViolation() throws Exception
    {
        NotifyBuilder builder = createNotifyBuilder().from(INPUT_QUEUE_URI).whenFailed(1).and().whenCompleted(1).wereSentTo(DLC_QUEUE_URI).create();
        final MyEntity test = new MyEntity();
        test.setName(null);
        input.sendBody(test);
        assertTrue(builderMatches(builder));
        assertEquals(0, messageCount(OUTPUT_QUEUE_URI));
        assertEquals(1, messageCount(DLC_QUEUE_URI));
        assertEquals(0, entityCount());
    }
}
