package com.carmanconsulting.camel.jpa;

import com.carmanconsulting.camel.entity.MyEntity;

/**
 * @author James Carman
 */
public interface MyEntityRepository
{
    void saveEntity(MyEntity entity);
}
