package com.carmanconsulting.camel;

import com.carmanconsulting.camel.jpa.MyEntityRepository;
import com.carmanconsulting.camel.jpa.MyEntityRepositoryPersistNoFlush;

/**
 * @author James Carman
 */
public class TestWithNoJpaFlush extends AbstractTestCaseTemplate
{
    @Override
    protected MyEntityRepository createRepository()
    {
        return new MyEntityRepositoryPersistNoFlush(getEntityManagerFactory());
    }
}
