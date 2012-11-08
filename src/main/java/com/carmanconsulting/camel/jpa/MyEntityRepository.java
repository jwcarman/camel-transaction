package com.carmanconsulting.camel.jpa;

import com.carmanconsulting.camel.entity.MyEntity;
import org.springframework.orm.jpa.JpaTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * @author James Carman
 */
public class MyEntityRepository
{
    private final EntityManagerFactory emf;

    public MyEntityRepository(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    public void saveEntity(MyEntity entity)
    {
        JpaTemplate template = new JpaTemplate(emf);
        template.persist(entity);
        //template.flush();
    }
}
