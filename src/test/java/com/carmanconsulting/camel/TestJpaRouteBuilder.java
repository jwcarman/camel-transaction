package com.carmanconsulting.camel;

import com.carmanconsulting.camel.entity.MyEntity;
import com.carmanconsulting.camel.jpa.MyEntityRepository;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.List;

/**
 * @author James Carman
 */
public class TestJpaRouteBuilder extends AbstractJpaRouteBuilderTest
{
    @Produce(uri = "jms:queue:input")
    private ProducerTemplate input;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new JpaRouteBuilder(new MyEntityRepository(getEntityManagerFactory()));
    }

    @Override
    public String isMockEndpoints()
    {
        return "jms:queue:output";
    }

    @Override
    protected void buildPersistenceUnit(PersistenceUnitBuilder builder)
    {
        builder.addClass(MyEntity.class);
    }

    protected void assertEntityTableCount(int count)
    {
        assertEquals(count, createJdbcTemplate().queryForInt("select count(*) from test_entity"));
    }

    @Test
    public void testHappyPath() throws Exception
    {
        final MyEntity test = new MyEntity();
        test.setName("test");
        MockEndpoint endpoint = getMockEndpoint("mock:jms:queue:output");
        endpoint.expectedMessageCount(1);
        input.sendBody(test);
        checkMockEndpointAssertions();
        assertEntityTableCount(1);
    }

    @Test
    public void testWithNullConstraintViolation() throws Exception
    {
        getMockEndpoint("mock:jms:queue:output").expectedMessageCount(0);

        final MyEntity test = new MyEntity();
        input.sendBody(test);
        checkMockEndpointAssertions();


        final Exchange exchange = receiveFromDeadLetterQueue();
        assertNotNull(exchange);
        assertEntityTableCount(0);
    }

    @Test
    public void testWithLengthConstraintViolation() throws Exception
    {
        final MyEntity test = new MyEntity();
        test.setName(StringUtils.repeat("X", 256));
        input.sendBody(test);
        final Exchange exchange = receiveFromDeadLetterQueue();
        assertNotNull(exchange);
        assertEntityTableCount(0);
    }
    @Override
    public long getTimeoutValue()
    {
        return 2;
    }
}
