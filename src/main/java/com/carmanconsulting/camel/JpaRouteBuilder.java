package com.carmanconsulting.camel;

import com.carmanconsulting.camel.entity.MyEntity;
import com.carmanconsulting.camel.jpa.MyEntityRepository;
import org.apache.camel.builder.RouteBuilder;

/**
 * @author James Carman
 */
public class JpaRouteBuilder extends RouteBuilder
{
    private MyEntityRepository entityRepository;

    public JpaRouteBuilder(MyEntityRepository entityRepository)
    {
        this.entityRepository = entityRepository;
    }

    @Override
    public void configure() throws Exception
    {
        onException().to("log:onException()?level=INFO&multiline=true&showAll=true");

        from("jms:queue:input")
                .transacted()
                .bean(entityRepository, "saveEntity")
                .to("jms:queue:output");

    }
}
