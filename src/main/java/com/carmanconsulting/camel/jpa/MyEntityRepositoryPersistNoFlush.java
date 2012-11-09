package com.carmanconsulting.camel.jpa;

import com.carmanconsulting.camel.entity.MyEntity;

import javax.persistence.EntityManagerFactory;

/**
 * @author James Carman
 */
public class MyEntityRepositoryPersistNoFlush extends AbstractMyEntityRepository
{
    public MyEntityRepositoryPersistNoFlush(EntityManagerFactory emf)
    {
        super(emf);
    }

    @Override
    public void saveEntity(MyEntity entity)
    {
        createJpaTemplate().persist(entity);
    }
}
