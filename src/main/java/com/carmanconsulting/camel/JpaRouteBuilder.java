package com.carmanconsulting.camel;

import com.carmanconsulting.camel.entity.MyEntity;
import org.apache.camel.builder.RouteBuilder;

/**
 * @author James Carman
 */
public class JpaRouteBuilder extends RouteBuilder
{
    @Override
    public void configure() throws Exception
    {
        from("jms:queue:input")
                .transacted()
                .to("jpa:" + MyEntity.class.getName())
                .to("jms:queue:output");
    }
}
