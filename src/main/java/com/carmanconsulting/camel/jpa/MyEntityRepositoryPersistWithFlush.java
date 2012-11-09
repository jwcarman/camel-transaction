package com.carmanconsulting.camel.jpa;

import com.carmanconsulting.camel.entity.MyEntity;
import org.springframework.orm.jpa.JpaTemplate;

import javax.persistence.EntityManagerFactory;

/**
 * @author James Carman
 */
public class MyEntityRepositoryPersistWithFlush extends AbstractMyEntityRepository
{
//----------------------------------------------------------------------------------------------------------------------
// Constructors
//----------------------------------------------------------------------------------------------------------------------

    public MyEntityRepositoryPersistWithFlush(EntityManagerFactory emf)
    {
        super(emf);
    }

//----------------------------------------------------------------------------------------------------------------------
// MyEntityRepository Implementation
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public void saveEntity(MyEntity entity)
    {
        JpaTemplate template = createJpaTemplate();
        template.persist(entity);
        template.flush();
    }
}
