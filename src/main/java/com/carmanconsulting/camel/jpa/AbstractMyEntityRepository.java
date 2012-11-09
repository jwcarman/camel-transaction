package com.carmanconsulting.camel.jpa;

import org.springframework.orm.jpa.JpaTemplate;

import javax.persistence.EntityManagerFactory;

/**
 * @author James Carman
 */
public abstract class AbstractMyEntityRepository implements MyEntityRepository
{
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private final EntityManagerFactory emf;

//----------------------------------------------------------------------------------------------------------------------
// Constructors
//----------------------------------------------------------------------------------------------------------------------

    public AbstractMyEntityRepository(EntityManagerFactory emf)
    {
        this.emf = emf;
    }

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    protected final JpaTemplate createJpaTemplate()
    {
        return new JpaTemplate(emf);
    }
}
