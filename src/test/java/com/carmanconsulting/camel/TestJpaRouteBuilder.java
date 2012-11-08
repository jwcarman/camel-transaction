package com.carmanconsulting.camel;

import com.carmanconsulting.camel.entity.MyEntity;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

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
        return new JpaRouteBuilder();
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
        endpoint.await(2, TimeUnit.SECONDS);
        assertEntityTableCount(1);
    }

    @Override
    public long getTimeoutValue()
    {
        return 2;
    }
}
