package com.carmanconsulting.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.SimpleLoadTimeWeaver;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author James Carman
 */
public abstract class AbstractJpaRouteBuilderTest extends AbstractRouteBuilderTest implements PersistenceUnitPostProcessor
{
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private EntityManagerFactory entityManagerFactory;

//----------------------------------------------------------------------------------------------------------------------
// Abstract Methods
//----------------------------------------------------------------------------------------------------------------------

    protected abstract void buildPersistenceUnit(PersistenceUnitBuilder builder);

//----------------------------------------------------------------------------------------------------------------------
// PersistenceUnitPostProcessor Implementation
//----------------------------------------------------------------------------------------------------------------------

    public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui)
    {
        pui.setExcludeUnlistedClasses(true);
        pui.setPersistenceUnitName(getPersistenceUnitName());
        buildPersistenceUnit(new PersistenceUnitBuilder(pui));
    }

//----------------------------------------------------------------------------------------------------------------------
// Getter/Setter Methods
//----------------------------------------------------------------------------------------------------------------------

    protected EntityManagerFactory getEntityManagerFactory()
    {
        if (entityManagerFactory == null)
        {
            LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
            bean.setPersistenceUnitName(getPersistenceUnitName());
            bean.setPersistenceUnitPostProcessors(new PersistenceUnitPostProcessor[]{this});
            bean.setPersistenceProviderClass(PersistenceProviderImpl.class);
            bean.setDataSource(getDataSource());
            bean.getJpaPropertyMap().put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
            bean.getJpaPropertyMap().put("openjpa.RuntimeUnenhancedClasses", "supported");
            bean.getJpaPropertyMap().put("openjpa.Log", "log4j");
            bean.afterPropertiesSet();
            entityManagerFactory = bean.getObject();
        }
        return entityManagerFactory;
    }

    protected PlatformTransactionManager getTransactionManager()
    {
        if(transactionManager == null)
        {
            transactionManager = new JpaTransactionManager(getEntityManagerFactory());
        }
        return transactionManager;
    }

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected CamelContext createCamelContext() throws Exception
    {
        CamelContext context = super.createCamelContext();
        context.addComponent("jpa", createJpaComponent());
        return context;
    }

    private JpaComponent createJpaComponent()
    {
        JpaComponent jpa = new JpaComponent();
        jpa.setEntityManagerFactory(getEntityManagerFactory());
        jpa.setTransactionManager(getTransactionManager());
        return jpa;
    }

    protected String getPersistenceUnitName()
    {
        return getClass().getSimpleName();
    }

    protected class PersistenceUnitBuilder
    {
        private final MutablePersistenceUnitInfo pui;

        private List<String> defaultClasses;

        public PersistenceUnitBuilder(MutablePersistenceUnitInfo pui)
        {
            this.pui = pui;
            pui.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
            pui.setPersistenceProviderClassName(PersistenceProviderImpl.class.getName());
            pui.setJtaDataSource(null);

            this.defaultClasses = new ArrayList<String>(pui.getManagedClassNames());
            pui.getManagedClassNames().clear();
        }

        public PersistenceUnitBuilder addDefaultClasses()
        {
            for (String defaultClass : defaultClasses)
            {
                pui.addManagedClassName(defaultClass);
            }

            return this;
        }

        public PersistenceUnitBuilder addClass(Class<?> entityClass)
        {
            pui.addManagedClassName(entityClass.getName());

            return this;
        }

        public PersistenceUnitBuilder showSql()
        {
            pui.getProperties().setProperty("hibernate.show_sql", "true");

            return this;
        }
    }
}
