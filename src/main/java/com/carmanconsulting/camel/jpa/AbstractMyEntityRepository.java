package com.carmanconsulting.camel.jpa;

import org.springframework.orm.jpa.JpaTemplate;

import javax.persistence.EntityManagerFactory;

/**
 * @author James Carman
 */
public abstract class AbstractMyEntityRepository implements MyEntityRepository
{
    private final EntityManagerFactory emf;

    public AbstractMyEntityRepository(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

    protected final JpaTemplate createJpaTemplate()
    {
        return new JpaTemplate(emf);
    }
}
