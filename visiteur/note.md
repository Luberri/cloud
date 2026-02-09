Je veux utiliser docker pour tout le projet web (le dossier visiteur) : java react etc (a part les images postgis et tileserver deja la). Et en fait je vais deployer ce projet dans un autre ordinateur, comment faire pour que ca marche direct dans d'autres ordinateurs en faisant compose up -d ?

En fait je veux que assembler manager et visiteur dans visiteur. Mais en ce moment manager utilise une base docker et visiteur une base locale et seulement docker pour l'affichage de la carte. Je veux que les deux utilisent la meme base docker (la base de visiteur avec la carte). Pour jdk-17, nginx, etc je veux encore utiliser localement mais pour la base postgres je veux utiliser docker (pour tout : le script database.sql, les test_data et data).

Images :
postgis, jdk-17, nginx, tileserver

Longitude Ouest : 47.48
Latitude Sud    : -18.98
Longitude Est   : 47.60
Latitude Nord   : -18.82

Etapes pour afficher une carte OSM d’Antananarivo avec PostGIS :
1️⃣ Création du container PostGIS avec Docker
docker run -d \
  --name postgis \
  -p 5432:5432 \
  -e POSTGRES_DB=gisdb \
  -e POSTGRES_USER=gisuser \
  -e POSTGRES_PASSWORD=gispass \
  postgis/postgis:15-3.4
Crée un container Docker avec Postgres + PostGIS
Base par défaut : gisdb
Utilisateur : gisuser, mot de passe : gispass

Tables créées :
planet_osm_point
planet_osm_line
planet_osm_polygon
planet_osm_roads
Index géométriques et osm_id ajoutés automatiquement.

Je dois maintenant afficher la carte (si possible sans python). 

Fais en sorte que le header et le footer a une hauteur fixe assez petite. Je ne veux pas qu'ils prennent beaucoup de place quand on zoom sur la carte.
Et fais un css plus clair et professionnel pour l'ensemble des pages (la carte et le tableau de bord).

----------------

Mettre les compagnies et signalements/road_issues dans la meme base postgres docker que Ny Ando.

Utiliser la meme image jdk que Ny Ando

-----
J'essaie de lancer manager et visiteur en meme temps sans docker, en fait j'ai localement la base cloud_db et jdk-17. J'ai d'abord lance visiteur, qui fonctionne sans probleme, mais quand je lance manager ca fait :
 ERROR 14320 --- [cloud] [  restartedMain] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Exception during pool initialization.

org.postgresql.util.PSQLException: La tentative de connexion a ÚchouÚ.
        at org.postgresql.core.v3.ConnectionFactoryImpl.openConnectionImpl(ConnectionFactoryImpl.java:354) ~[postgresql-42.6.0.jar:42.6.0]
        at org.postgresql.core.ConnectionFactory.openConnection(ConnectionFactory.java:54) ~[postgresql-42.6.0.jar:42.6.0]
        at org.postgresql.jdbc.PgConnection.<init>(PgConnection.java:263) ~[postgresql-42.6.0.jar:42.6.0]
        at org.postgresql.Driver.makeConnection(Driver.java:443) ~[postgresql-42.6.0.jar:42.6.0]
        at org.postgresql.Driver.connect(Driver.java:297) ~[postgresql-42.6.0.jar:42.6.0]
        at com.zaxxer.hikari.util.DriverDataSource.getConnection(DriverDataSource.java:138) ~[HikariCP-5.0.1.jar:na]        at com.zaxxer.hikari.pool.PoolBase.newConnection(PoolBase.java:359) ~[HikariCP-5.0.1.jar:na]
        at com.zaxxer.hikari.pool.PoolBase.newPoolEntry(PoolBase.java:201) ~[HikariCP-5.0.1.jar:na]
        at com.zaxxer.hikari.pool.HikariPool.createPoolEntry(HikariPool.java:470) ~[HikariCP-5.0.1.jar:na]
        at com.zaxxer.hikari.pool.HikariPool.checkFailFast(HikariPool.java:561) ~[HikariCP-5.0.1.jar:na]
        at com.zaxxer.hikari.pool.HikariPool.<init>(HikariPool.java:100) ~[HikariCP-5.0.1.jar:na]
        at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:112) ~[HikariCP-5.0.1.jar:na]     
        at org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl.getConnection(DatasourceConnectionProviderImpl.java:122) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator$ConnectionProviderJdbcConnectionAccess.obtainConnection(JdbcEnvironmentInitiator.java:428) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcIsolationDelegate.delegateWork(JdbcIsolationDelegate.java:61) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.getJdbcEnvironmentUsingJdbcMetadata(JdbcEnvironmentInitiator.java:276) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.initiateService(JdbcEnvironmentInitiator.java:107) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.initiateService(JdbcEnvironmentInitiator.java:68) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.registry.internal.StandardServiceRegistryImpl.initiateService(StandardServiceRegistryImpl.java:129) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.service.internal.AbstractServiceRegistryImpl.createService(AbstractServiceRegistryImpl.java:263) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.service.internal.AbstractServiceRegistryImpl.initializeService(AbstractServiceRegistryImpl.java:238) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.service.internal.AbstractServiceRegistryImpl.getService(AbstractServiceRegistryImpl.java:215) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.model.relational.Database.<init>(Database.java:45) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.internal.InFlightMetadataCollectorImpl.getDatabase(InFlightMetadataCollectorImpl.java:223) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.internal.InFlightMetadataCollectorImpl.<init>(InFlightMetadataCollectorImpl.java:191) 
~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.model.process.spi.MetadataBuildingProcess.complete(MetadataBuildingProcess.java:170) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.metadata(EntityManagerFactoryBuilderImpl.java:1432) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.build(EntityManagerFactoryBuilderImpl.java:1503) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:75) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.createNativeEntityManagerFactory(LocalContainerEntityManagerFactoryBean.java:376) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:409) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:396) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:352) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1820) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1769) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:599) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:521) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:325) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:323) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:199) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.context.support.AbstractApplicationContext.getBean(AbstractApplicationContext.java:1232) ~[spring-context-6.1.2.jar:6.1.2]
        at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:950) ~[spring-context-6.1.2.jar:6.1.2]
        at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:625) ~[spring-context-6.1.2.jar:6.1.2]
        at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:762) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:464) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.1.jar:3.2.1]        at org.springframework.boot.SpringApplication.run(SpringApplication.java:1358) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:1347) ~[spring-boot-3.2.1.jar:3.2.1]
        at com.demo.cloud.CloudApplication.main(CloudApplication.java:10) ~[classes/:na]
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]        at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) 
~[na:na]
        at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
        at org.springframework.boot.devtools.restart.RestartLauncher.run(RestartLauncher.java:50) ~[spring-boot-devtools-3.2.1.jar:3.2.1]
Caused by: java.net.UnknownHostException: postgres
        at java.base/sun.nio.ch.NioSocketImpl.connect(NioSocketImpl.java:572) ~[na:na]
        at java.base/java.net.SocksSocketImpl.connect(SocksSocketImpl.java:327) ~[na:na]
        at java.base/java.net.Socket.connect(Socket.java:633) ~[na:na]
        at org.postgresql.core.PGStream.createSocket(PGStream.java:243) ~[postgresql-42.6.0.jar:42.6.0]
        at org.postgresql.core.PGStream.<init>(PGStream.java:98) ~[postgresql-42.6.0.jar:42.6.0]
        at org.postgresql.core.v3.ConnectionFactoryImpl.tryConnect(ConnectionFactoryImpl.java:132) ~[postgresql-42.6.0.jar:42.6.0]
        at org.postgresql.core.v3.ConnectionFactoryImpl.openConnectionImpl(ConnectionFactoryImpl.java:258) ~[postgresql-42.6.0.jar:42.6.0]
        ... 55 common frames omitted

2026-01-27T06:24:57.740+03:00  WARN 14320 --- [cloud] [  restartedMain] o.h.e.j.e.i.JdbcEnvironmentInitiator     : HHH000342: Could not obtain connection to query metadata

java.lang.NullPointerException: Cannot invoke "org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(java.sql.SQLException, String)" because the return value of "org.hibernate.resource.transaction.backend.jdbc.internal.JdbcIsolationDelegate.sqlExceptionHelper()" is null
        at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcIsolationDelegate.delegateWork(JdbcIsolationDelegate.java:116) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.getJdbcEnvironmentUsingJdbcMetadata(JdbcEnvironmentInitiator.java:276) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.initiateService(JdbcEnvironmentInitiator.java:107) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.initiateService(JdbcEnvironmentInitiator.java:68) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.registry.internal.StandardServiceRegistryImpl.initiateService(StandardServiceRegistryImpl.java:129) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.service.internal.AbstractServiceRegistryImpl.createService(AbstractServiceRegistryImpl.java:263) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.service.internal.AbstractServiceRegistryImpl.initializeService(AbstractServiceRegistryImpl.java:238) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.service.internal.AbstractServiceRegistryImpl.getService(AbstractServiceRegistryImpl.java:215) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.model.relational.Database.<init>(Database.java:45) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.internal.InFlightMetadataCollectorImpl.getDatabase(InFlightMetadataCollectorImpl.java:223) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.internal.InFlightMetadataCollectorImpl.<init>(InFlightMetadataCollectorImpl.java:191) 
~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.model.process.spi.MetadataBuildingProcess.complete(MetadataBuildingProcess.java:170) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.metadata(EntityManagerFactoryBuilderImpl.java:1432) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.build(EntityManagerFactoryBuilderImpl.java:1503) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:75) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.createNativeEntityManagerFactory(LocalContainerEntityManagerFactoryBean.java:376) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:409) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:396) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:352) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1820) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1769) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:599) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:521) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:325) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:323) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:199) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.context.support.AbstractApplicationContext.getBean(AbstractApplicationContext.java:1232) ~[spring-context-6.1.2.jar:6.1.2]
        at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:950) ~[spring-context-6.1.2.jar:6.1.2]
        at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:625) ~[spring-context-6.1.2.jar:6.1.2]
        at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:762) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:464) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.1.jar:3.2.1]        at org.springframework.boot.SpringApplication.run(SpringApplication.java:1358) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:1347) ~[spring-boot-3.2.1.jar:3.2.1]
        at com.demo.cloud.CloudApplication.main(CloudApplication.java:10) ~[classes/:na]
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]        at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) 
~[na:na]
        at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
        at org.springframework.boot.devtools.restart.RestartLauncher.run(RestartLauncher.java:50) ~[spring-boot-devtools-3.2.1.jar:3.2.1]

2026-01-27T06:24:57.809+03:00 ERROR 14320 --- [cloud] [  restartedMain] j.LocalContainerEntityManagerFactoryBean : Failed to initialize JPA EntityManagerFactory: Unable to create requested service [org.hibernate.engine.jdbc.env.spi.JdbcEnvironment] due to: Unable to determine Dialect without JDBC metadata (please set 'jakarta.persistence.jdbc.url' for common cases or 'hibernate.dialect' when a custom Dialect implementation must be provided)
2026-01-27T06:24:57.809+03:00  WARN 14320 --- [cloud] [  restartedMain] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: Unable to create requested service [org.hibernate.engine.jdbc.env.spi.JdbcEnvironment] due to: Unable to determine Dialect without JDBC metadata (please set 'jakarta.persistence.jdbc.url' for common cases or 'hibernate.dialect' when a custom Dialect implementation must be provided)
2026-01-27T06:24:57.809+03:00  INFO 14320 --- [cloud] [  restartedMain] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
2026-01-27T06:24:57.934+03:00  INFO 14320 --- [cloud] [  restartedMain] .s.b.a.l.ConditionEvaluationReportLogger : 

Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2026-01-27T06:24:57.992+03:00 ERROR 14320 --- [cloud] [  restartedMain] o.s.boot.SpringApplication               : Application run failed

org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: Unable to create requested service [org.hibernate.engine.jdbc.env.spi.JdbcEnvironment] due to: Unable to determine Dialect without JDBC metadata (please set 'jakarta.persistence.jdbc.url' for common cases or 'hibernate.dialect' when a custom Dialect implementation must be provided)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1773) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:599) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:521) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:325) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:323) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:199) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.context.support.AbstractApplicationContext.getBean(AbstractApplicationContext.java:1232) ~[spring-context-6.1.2.jar:6.1.2]
        at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:950) ~[spring-context-6.1.2.jar:6.1.2]
        at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:625) ~[spring-context-6.1.2.jar:6.1.2]
        at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:762) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:464) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:334) ~[spring-boot-3.2.1.jar:3.2.1]        at org.springframework.boot.SpringApplication.run(SpringApplication.java:1358) ~[spring-boot-3.2.1.jar:3.2.1]
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:1347) ~[spring-boot-3.2.1.jar:3.2.1]
        at com.demo.cloud.CloudApplication.main(CloudApplication.java:10) ~[classes/:na]
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]        at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) 
~[na:na]
        at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
        at org.springframework.boot.devtools.restart.RestartLauncher.run(RestartLauncher.java:50) ~[spring-boot-devtools-3.2.1.jar:3.2.1]
Caused by: org.hibernate.service.spi.ServiceException: Unable to create requested service [org.hibernate.engine.jdbc.env.spi.JdbcEnvironment] due to: Unable to determine Dialect without JDBC metadata (please set 'jakarta.persistence.jdbc.url' for common cases or 'hibernate.dialect' when a custom Dialect implementation must be provided)
        at org.hibernate.service.internal.AbstractServiceRegistryImpl.createService(AbstractServiceRegistryImpl.java:276) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.service.internal.AbstractServiceRegistryImpl.initializeService(AbstractServiceRegistryImpl.java:238) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.service.internal.AbstractServiceRegistryImpl.getService(AbstractServiceRegistryImpl.java:215) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.model.relational.Database.<init>(Database.java:45) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.internal.InFlightMetadataCollectorImpl.getDatabase(InFlightMetadataCollectorImpl.java:223) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.internal.InFlightMetadataCollectorImpl.<init>(InFlightMetadataCollectorImpl.java:191) 
~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.model.process.spi.MetadataBuildingProcess.complete(MetadataBuildingProcess.java:170) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.metadata(EntityManagerFactoryBuilderImpl.java:1432) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl.build(EntityManagerFactoryBuilderImpl.java:1503) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.springframework.orm.jpa.vendor.SpringHibernateJpaPersistenceProvider.createContainerEntityManagerFactory(SpringHibernateJpaPersistenceProvider.java:75) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.createNativeEntityManagerFactory(LocalContainerEntityManagerFactoryBean.java:376) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.buildNativeEntityManagerFactory(AbstractEntityManagerFactoryBean.java:409) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.AbstractEntityManagerFactoryBean.afterPropertiesSet(AbstractEntityManagerFactoryBean.java:396) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.afterPropertiesSet(LocalContainerEntityManagerFactoryBean.java:352) ~[spring-orm-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1820) ~[spring-beans-6.1.2.jar:6.1.2]
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1769) ~[spring-beans-6.1.2.jar:6.1.2]
        ... 21 common frames omitted
Caused by: org.hibernate.HibernateException: Unable to determine Dialect without JDBC metadata (please set 'jakarta.persistence.jdbc.url' for common cases or 'hibernate.dialect' when a custom Dialect implementation must be provided)        at org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl.determineDialect(DialectFactoryImpl.java:191) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl.buildDialect(DialectFactoryImpl.java:87) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.getJdbcEnvironmentWithDefaults(JdbcEnvironmentInitiator.java:143) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.getJdbcEnvironmentUsingJdbcMetadata(JdbcEnvironmentInitiator.java:348) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.initiateService(JdbcEnvironmentInitiator.java:107) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.initiateService(JdbcEnvironmentInitiator.java:68) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.boot.registry.internal.StandardServiceRegistryImpl.initiateService(StandardServiceRegistryImpl.java:129) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        at org.hibernate.service.internal.AbstractServiceRegistryImpl.createService(AbstractServiceRegistryImpl.java:263) ~[hibernate-core-6.4.1.Final.jar:6.4.1.Final]
        ... 36 common frames omitted