package com.carmanconsulting.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.junit.After;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author James Carman
 */
public abstract class AbstractRouteBuilderTest extends CamelTestSupport implements PersistenceUnitPostProcessor
{
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private PlatformTransactionManager transactionManager;
    private EntityManagerFactory entityManagerFactory;
    private BasicDataSource dataSource;
    private CamelContext context;

//----------------------------------------------------------------------------------------------------------------------
// Abstract Methods
//----------------------------------------------------------------------------------------------------------------------

    protected abstract void configurePersistenceUnit(PersistenceUnitBuilder builder);

    protected abstract JmsComponent createJmsComponent();

    protected abstract JpaComponent createJpaComponent();

    protected abstract PlatformTransactionManager createTransactionManager();

//----------------------------------------------------------------------------------------------------------------------
// PersistenceUnitPostProcessor Implementation
//----------------------------------------------------------------------------------------------------------------------

    public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui)
    {
        pui.setExcludeUnlistedClasses(true);
        pui.setPersistenceUnitName(getPersistenceUnitName());
        configurePersistenceUnit(new PersistenceUnitBuilder(pui));
    }

//----------------------------------------------------------------------------------------------------------------------
// Getter/Setter Methods
//----------------------------------------------------------------------------------------------------------------------

    public CamelContext getContext()
    {
        return context;
    }

    protected synchronized DataSource getDataSource()
    {
        if (dataSource == null)
        {
            dataSource = new BasicDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            dataSource.setUrl("jdbc:h2:mem:" + getClass().getSimpleName() + ";TRACE_LEVEL_FILE=4");
            dataSource.setMinIdle(1);
            dataSource.setInitialSize(1);
            dataSource.setMaxActive(1);
        }
        return dataSource;
    }

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

    protected final PlatformTransactionManager getTransactionManager()
    {
        if (transactionManager == null)
        {
            transactionManager = createTransactionManager();
        }
        return transactionManager;
    }

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @After
    public void checkMockEndpointAssertions() throws InterruptedException
    {
        assertMockEndpointsSatisfied(getTimeoutValue(), getTimeoutUnit());
    }

    @After
    public void closeDataSource() throws SQLException
    {
        log.info("Closing DataSource...");
        dataSource.close();
    }

    protected CamelContext createCamelContext() throws Exception
    {
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("dataSource", getDataSource());
        registry.put("transactionManager", getTransactionManager());
        doRegistryBindings(registry);

        context = new DefaultCamelContext(registry);
        context.addComponent("jms", createJmsComponent());
        context.addComponent("jpa", createJpaComponent());
        return context;
    }

    protected JdbcTemplate createJdbcTemplate()
    {
        return new JdbcTemplate(getDataSource());
    }

    protected NotifyBuilder createNotifyBuilder()
    {
        return new NotifyBuilder(context);
    }

    protected void doRegistryBindings(SimpleRegistry registry)
    {
        // Do nothing
    }

    protected String getPersistenceUnitName()
    {
        return getClass().getSimpleName();
    }

    public TimeUnit getTimeoutUnit()
    {
        return TimeUnit.SECONDS;
    }

    public long getTimeoutValue()
    {
        return 3;
    }

    protected Exchange pollDeadLetterQueue()
    {
        return consumer.receive("jms:queue:ActiveMQ.DLQ", 2000);
    }

//----------------------------------------------------------------------------------------------------------------------
// Inner Classes
//----------------------------------------------------------------------------------------------------------------------

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

        public PersistenceUnitBuilder withProperty(String key, String value)
        {
            pui.addProperty(key, value);
            return this;
        }

        public PersistenceUnitBuilder withDefaultClasses()
        {
            for (String defaultClass : defaultClasses)
            {
                pui.addManagedClassName(defaultClass);
            }

            return this;
        }

        public PersistenceUnitBuilder withClass(Class<?> entityClass)
        {
            pui.addManagedClassName(entityClass.getName());
            return this;
        }
    }
}
