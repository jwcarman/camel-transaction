package com.carmanconsulting.camel;

import org.apache.camel.builder.RouteBuilder;

public class JdbcRouteBuilder extends RouteBuilder
{
    public void configure()
    {
        from("jms:queue:input")
                .transacted()
                .to("sql:insert into test_table (test_value) values (#)?dataSourceRef=dataSource")
                .choice()
                .when(body().isEqualTo("Foo"))
                .to("jms:queue:output")
                .otherwise().throwException(new IllegalArgumentException("No way, Jose!"));


    }

}
