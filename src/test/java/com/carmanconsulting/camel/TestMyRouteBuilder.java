package com.carmanconsulting.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * @author James Carman
 */
public class TestMyRouteBuilder extends AbstractRouteBuilderTest
{
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    public static final String OUTPUT_QUEUE = "jms:queue:output";
    public static final String INPUT_QUEUE = "jms:queue:input";

    @Produce(uri = INPUT_QUEUE)
    private ProducerTemplate input;

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new MyRouteBuilder();
    }

    @Override
    public String isMockEndpoints()
    {
        return OUTPUT_QUEUE;
    }

    @Test
    public void testHappyPath() throws Exception
    {
        MockEndpoint mock = getMockEndpoint("mock:" + OUTPUT_QUEUE);
        mock.expectedBodiesReceived("Foo");
        input.sendBody("Foo");
    }

    @Test
    public void testWithFailedSqlInsert() throws Exception
    {
        final String invalidValue = StringUtils.repeat("X", 256);
        input.sendBody(invalidValue);

        final Exchange exchange = receiveFromDeadLetterQueue();
        assertNotNull(exchange);
        assertEquals(invalidValue, exchange.getIn().getBody(String.class));

        assertTestTableEmpty();
    }

    @Test
    public void testFailedOutputQueueWrite() throws Exception
    {
        input.sendBody("Bar");

        final Exchange exchange = receiveFromDeadLetterQueue();
        assertNotNull(exchange);
        assertEquals("Bar", exchange.getIn().getBody(String.class));

        assertTestTableEmpty();
    }

}
