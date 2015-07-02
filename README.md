Hibernate Search, JDG and JMS: demonstrates JBoss EAP integrating Hibernate Search with an Infinispan directory and JMS backend
===============================================================================================
Author: Matt Robson

Technologies: JBoss EAP, JBoss Data Grid, Hibernate Search, HornetQ

Products: JBoss EAP 6.4 & JBoss Data Grid 6.5

Breakdown
---------
This example walks through all the requirements to get up and running with Hibernate Search using the Infinispan directory provider and a JMS backend on EAP 6.4 and JDG 6.5.  It provides a simple datamodel and the ability to read, write and reindex.  The JMS backend is setup as a symmetrical cluster with each live node having a dedicated backup in the opposing configuration. 

For more information see:                                                                                                     
                                                                                                                              
* <https://access.redhat.com/documentation/en-US/JBoss_Enterprise_Application_Platform> for more information about using JBoss EAP
* <https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Data_Grid> for more information about using JBoss Data Grid

System Requirements
-------------------
Before building and running this example you need:

* Maven 3.2 or higher
* Java 1.7
* JBoss EAP 6.4
* JBoss Data Grid 6.5 Library Module

Setting the Base
----------------
You can easily enough run this example off 1 server if you're looking to test the waters.  For simplicity, we're going to use 3 unique local installations, but you could use Docker, VMs or Vagrant for additional isolation if you like.

Locally, my 3 servers are:

	Master - /EAP-6.4.0-Final-1/
	Slave A - /EAP-6.4.0-Final-2/
	Slave B - /EAP-6.4.0-Final-3/

Once you have downloaded 'JBoss Data Grid 6.5 Library Module', you need to install the module in all 3 EAP instances.  Copy the 'javax' and 'org' directories to /modules/.

* <https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Data_Grid/6.5/html-single/Getting_Started_Guide/index.html#Deploy_JBoss_Data_Grid_in_JBoss_EAP_Library_Mode> for more information on install JBoss Data Grid Library Mode in EAP.

Next we want to clone the project so we can first get access to the necessary HornetQ configurations.

        git clone https://github.com/mrobson/hibernate-search-infinispan-jms.git

Once the project is cloned you will see:

	hibernate-search-infinispan-jms/hornetq-master-node/standalone-full-ha-search-master.xml
	hibernate-search-infinispan-jms/hornetq-symmetrical-nodea/standalone-full-ha-search-hq1.xml
	hibernate-search-infinispan-jms/hornetq-symmetrical-nodeb/standalone-full-ha-search-hq2.xml

These are pre-configured for the master node and the required JMS backends.  You can review the HornetQ subsystem to see how a symmetrical cluster is formed and how to link backup nodes across servers. The only thing you need to do is ensure the datasource configuration (url, username, password and driver module) will allow you to connect to your database.

Copy the provided XML files to their respective installations:

	cp hibernate-search-infinispan-jms/hornetq-master-node/standalone-full-ha-search-master.xml /EAP-6.4.0-Final-1/standalone/configuration/
	cp hibernate-search-infinispan-jms/hornetq-symmetrical-nodea/standalone-full-ha-search-hq1.xml /EAP-6.4.0-Final-2/standalone/configuration/
	cp hibernate-search-infinispan-jms/hornetq-symmetrical-nodeb/standalone-full-ha-search-hq2.xml /EAP-6.4.0-Final-3/standalone/configuration/

Once that's done you can start your 3 servers to ensure everything is running optimally before deploying any code.

From the bin/ directory:

Master:

	./standalone.sh --server-config=standalone-full-ha-search-master.xml -Djboss.node.name=nodeOne -b 127.0.0.1 -Djgroups.bind_addr=127.0.0.1 -Djava.net.preferIPv4Stack=true

Starting the master, you won't see too much.  Here is what you should look out for.

Number one is no errors.

Aside from that, your should see your datasource bound:

	13:54:26,508 INFO  [org.jboss.as.connector.subsystems.datasources] (MSC service thread 1-4) JBAS010400: Bound data source [java:/test/datasource]

You will see HornetQ start with the necessary bound objects:

	13:54:26,771 INFO  [org.hornetq.core.server] (ServerService Thread Pool -- 68) HQ221007: Server is now live
	13:54:26,772 INFO  [org.hornetq.core.server] (ServerService Thread Pool -- 68) HQ221001: HornetQ Server version 2.3.25.Final (2.3.x, 123) [651fd776-fb23-11e4-ad98-b99f19447bdf] 
	13:54:26,838 INFO  [org.hornetq.core.server] (ServerService Thread Pool -- 69) HQ221003: trying to deploy queue jms.queue.IndexingQueue
	13:54:26,869 INFO  [org.jboss.as.connector.deployment] (MSC service thread 1-3) JBAS010406: Registered connection factory java:/JmsXA
	13:54:26,885 INFO  [org.jboss.as.messaging] (ServerService Thread Pool -- 69) JBAS011601: Bound messaging object to jndi name java:/queue/indexupdates

Slave A:

Starting the slave nodes, there will be a lot of activity with HornetQ.  We're going to start the first slave node and let if fully boot for illustrative purposes only (you can start them simultaneously)

	./standalone.sh --server-config=standalone-full-ha-search-hq1.xml -Djboss.node.name=nodeTwo -b 127.0.0.1 -Djgroups.bind_addr=127.0.0.1 -Djava.net.preferIPv4Stack=true -Djboss.socket.binding.port-offset=10

You will see your live and backup server starting:

	14:13:15,996 INFO  [org.hornetq.core.server] (ServerService Thread Pool -- 69) HQ221000: live server is starting with configuration HornetQ Configuration (clustered=true,backup=false,sharedStore=false,journalDirectory=/EAP-6.4.0-Final-2/standalone/data/../data/messaging/journal,bindingsDirectory=/EAP-6.4.0-Final-2/standalone/data/../data/messaging/bindings,largeMessagesDirectory=/EAP-6.4.0-Final-2/standalone/data/../data/messaging/largemessages,pagingDirectory=/EAP-6.4.0-Final-2/standalone/data/../data/messaging/paging) 


	14:13:15,905 INFO  [org.hornetq.core.server] (ServerService Thread Pool -- 70) HQ221000: backup server is starting with configuration HornetQ Configuration (clustered=true,backup=true,sharedStore=false,journalDirectory=/EAP-6.4.0-Final-2/standalone/data/../data/messaging-bak/journal,bindingsDirectory=/EAP-6.4.0-Final-2/standalone/data/../data/messaging-bak/bindings,largeMessagesDirectory=/EAP-6.4.0-Final-2/standalone/data/../data/messaging-bak/largemessages,pagingDirectory=/EAP-6.4.0-Final-2/standalone/data/../data/messaging-bak/paging)                                                                                         

Again, you will see your datasource bound along with your connection factories and queue:

	14:13:16,356 INFO  [org.jboss.as.connector.subsystems.datasources] (MSC service thread 1-2) JBAS010400: Bound data source [java:/test/datasource]                                                                 
	14:13:28,193 INFO  [org.hornetq.core.server] (ServerService Thread Pool -- 69) HQ221007: Server is now live
	14:13:17,140 INFO  [org.jboss.as.messaging] (ServerService Thread Pool -- 68) JBAS011601: Bound messaging object to jndi name java:/RemoteConnectionFactory                                                       
	14:13:17,141 INFO  [org.jboss.as.messaging] (ServerService Thread Pool -- 68) JBAS011601: Bound messaging object to jndi name java:jboss/exported/jms/RemoteConnectionFactory
	14:13:28,199 INFO  [org.jboss.as.messaging] (ServerService Thread Pool -- 69) JBAS011601: Bound messaging object to jndi name java:/queue/indexupdates                                                            

Finally you will see the backup waiting on its live server which is on the node we have no started yet:

	14:13:28,925 INFO  [org.hornetq.core.server] (HQ119000: Activation for server HornetQServerImpl::serverUUID=null) HQ221109: HornetQ Backup Server version 2.3.25.Final (2.3.x, 123) [null] started, waiting live to fail before it gets active                                                                             

Once slave b is started, you will see some additional things happening in the logs.  First you see the live server replicating it journal files to the backup on slave b:

	14:19:19,300 INFO  [org.hornetq.core.server] (Thread-95) HQ221025: Replication: sending JournalFileImpl: (hornetq-data-28.hq id = 7, recordID = 7) (size=10,485,760) to backup. NIOSequentialFile /EAP-6.4.0-Final-2/standalone/data/../data/messaging/journal/hornetq-data-28.hq

Then you see the symmetrical cluster being formed:

	14:19:24,682 INFO  [org.hornetq.core.server] (Thread-11 (HornetQ-server-HornetQServerImpl::serverUUID=c2176f65-fb3d-11e4-aac5-351352cf256d-1862985516)) HQ221027: Bridge ClusterConnectionBridge@3ffd1845 [name=sf.search-cluster.5aa2cc4c-fb3e-11e4-8400-eb421b7af79e, queue=QueueImpl[name=sf.search-cluster.5aa2cc4c-fb3e-11e4-8400-eb421b7af79e, postOffice=PostOfficeImpl [server=HornetQServerImpl::serverUUID=c2176f65-fb3d-11e4-aac5-351352cf256d]]@1447ad5 targetConnector=ServerLocatorImpl (identity=(Cluster-connection-bridge::ClusterConnectionBridge@3ffd1845 [name=sf.search-cluster.5aa2cc4c-fb3e-11e4-8400-eb421b7af79e, queue=QueueImpl[name=sf.search-cluster.5aa2cc4c-fb3e-11e4-8400-eb421b7af79e, postOffice=PostOfficeImpl [server=HornetQServerImpl::serverUUID=c2176f65-fb3d-11e4-aac5-351352cf256d]]@1447ad5 targetConnector=ServerLocatorImpl [initialConnectors=[TransportConfiguration(name=netty, factory=org-hornetq-core-remoting-impl-netty-NettyConnectorFactory) ?port=5465&host=localhost-localdomain&use-nio=true], discoveryGroupConfiguration=null]]::ClusterConnectionImpl@2106855591[nodeUUID=c2176f65-fb3d-11e4-aac5-351352cf256d, connector=TransportConfiguration(name=netty, factory=org-hornetq-core-remoting-impl-netty-NettyConnectorFactory) ?port=5455&host=localhost-localdomain, address=jms, server=HornetQServerImpl::serverUUID=c2176f65-fb3d-11e4-aac5-351352cf256d])) [initialConnectors=[TransportConfiguration(name=netty, factory=org-hornetq-core-remoting-impl-netty-NettyConnectorFactory) ?port=5465&host=localhost-localdomain&use-nio=true], discoveryGroupConfiguration=null]] is connected

Finally, once the live server on slave b has synchronized with the backup server on slave a, you will see:

	14:19:27,815 INFO  [org.hornetq.core.server] (New I/O worker #35) HQ221024: Backup server HornetQServerImpl::serverUUID=5aa2cc4c-fb3e-11e4-8400-eb421b7af79e is synchronized with live-server.
	14:19:28,243 INFO  [org.hornetq.core.server] (Thread-1 (HornetQ-server-HornetQServerImpl::serverUUID=null-847143839)) HQ221031: backup announced


Slave B:

Once slave a is started, start slave b.

	./standalone.sh --server-config=standalone-full-ha-search-hq2.xml -Djboss.node.name=nodeThree -b 127.0.0.1 -Djgroups.bind_addr=127.0.0.1 -Djava.net.preferIPv4Stack=true -Djboss.socket.binding.port-offset=20

You will see all the same messages as on slave a, the live and backup server starting, along with the datasource, connection factories and queue being bound.

Once the initial configuration is done, you will see HornetQ communicating with the symmetrical partner along with the live servers synchronizing with their backup pair.

Because the live server on slave a is already running, you see the backup on slave b start synchronizing to the live server first:

	14:19:21,285 INFO  [org.hornetq.core.server] (Old I/O client worker ([id: 0xc9e70590, /127.0.0.1:38808 => localhost.localdomain/127.0.0.1:5455])) HQ221024: Backup server HornetQServerImpl::serverUUID=c2176f65-fb3d-11e4-aac5-351352cf256d is synchronized with live-server.
	14:19:23,240 INFO  [org.hornetq.core.server] (Thread-1 (HornetQ-server-HornetQServerImpl::serverUUID=null-1179819529)) HQ221031: backup announced

The live server on slave b becomes active and then forms a bridge between the live server on slave a, this create the symmetrical cluster:

	14:19:24,506 INFO  [org.hornetq.core.server] (ServerService Thread Pool -- 68) HQ221007: Server is now live
	14:19:24,697 INFO  [org.hornetq.core.server] (Thread-18 (HornetQ-server-HornetQServerImpl::serverUUID=5aa2cc4c-fb3e-11e4-8400-eb421b7af79e-1151709877)) HQ221027: Bridge ClusterConnectionBridge@5939b748 [name=sf.search-cluster.c2176f65-fb3d-11e4-aac5-351352cf256d, queue=QueueImpl[name=sf.search-cluster.c2176f65-fb3d-11e4-aac5-351352cf256d, postOffice=PostOfficeImpl [server=HornetQServerImpl::serverUUID=5aa2cc4c-fb3e-11e4-8400-eb421b7af79e]]@5c3005bb targetConnector=ServerLocatorImpl (identity=(Cluster-connection-bridge::ClusterConnectionBridge@5939b748 [name=sf.search-cluster.c2176f65-fb3d-11e4-aac5-351352cf256d, queue=QueueImpl[name=sf.search-cluster.c2176f65-fb3d-11e4-aac5-351352cf256d, postOffice=PostOfficeImpl [server=HornetQServerImpl::serverUUID=5aa2cc4c-fb3e-11e4-8400-eb421b7af79e]]@5c3005bb targetConnector=ServerLocatorImpl [initialConnectors=[TransportConfiguration(name=netty, factory=org-hornetq-core-remoting-impl-netty-NettyConnectorFactory) ?port=5455&host=localhost-localdomain], discoveryGroupConfiguration=null]]::ClusterConnectionImpl@323373156[nodeUUID=5aa2cc4c-fb3e-11e4-8400-eb421b7af79e, connector=TransportConfiguration(name=netty, factory=org-hornetq-core-remoting-impl-netty-NettyConnectorFactory) ?port=5465&host=localhost-localdomain&use-nio=true, address=jms, server=HornetQServerImpl::serverUUID=5aa2cc4c-fb3e-11e4-8400-eb421b7af79e])) [initialConnectors=[TransportConfiguration(name=netty, factory=org-hornetq-core-remoting-impl-netty-NettyConnectorFactory) ?port=5455&host=localhost-localdomain], discoveryGroupConfiguration=null]] is connected

You will then see the live server on slave b replicating any journal files to the backup server on slave a:

	14:19:25,804 INFO  [org.hornetq.core.server] (Thread-95) HQ221025: Replication: sending JournalFileImpl: (hornetq-data-12.hq id = 8, recordID = 8) (size=10,485,760) to backup. NIOSequentialFile /EAP-6.4.0-Final-3/standalone/data/../data/messaging/journal/hornetq-data-12.hq

At this point your 3 servers are up and running as expected and you can continue on to building and deploying the applications.

Build and Deploy
----------------

1) change to project directory

	cd hibernate-search-infinispan-jms

2) build

	mvn clean install

This will produce 2 deployable WARs and the datamodel gets added to each as a dependancy.

	.m2/repository/org/mrobson/example/search-slave/1.0-SNAPSHOT/search-slave-1.0-SNAPSHOT.war
	.m2/repository/org/mrobson/example/search-master/1.0-SNAPSHOT/search-master-1.0-SNAPSHOT.war

3) deploy

We're taking advantage of the jboss-as-maven-plugin here so deployment is very simple, just specify the host and port and it will deploy to the correct server.

	cd search-master/
	mvn jboss-as:deploy -Dhost=127.0.0.1 -Dport=9999

When we deploy the master node, we see it create the 'SearchMasterMDB' using the 'hornetq-ra' resource adapter, start a consumer thread for our entity and finally start a JGroups channel for our replicated cache.

	11:02:44,149 INFO  [org.jboss.as.repository] (management-handler-thread - 1) JBAS014900: Content added at location /EAP-6.4.0-Final-1/standalone/data/content/6c/f7c571a14832b29ea9765cb913866c97cfac3c/content
	11:02:44,156 INFO  [org.jboss.as.server.deployment] (MSC service thread 1-8) JBAS015876: Starting deployment of "search-master-1.0-SNAPSHOT.war" (runtime-name: "search-master-1.0-SNAPSHOT.war")
	11:02:44,174 INFO  [org.jboss.as.jpa] (MSC service thread 1-5) JBAS011401: Read persistence.xml for search-pu
	11:02:44,386 INFO  [org.jboss.as.jpa] (ServerService Thread Pool -- 72) JBAS011402: Starting Persistence Unit Service 'search-master-1.0-SNAPSHOT.war#search-pu'
	11:02:44,452 INFO  [org.jboss.as.ejb3] (MSC service thread 1-7) JBAS014142: Started message driven bean 'SearchMasterMDB' with 'hornetq-ra' resource adapter
	11:02:44,517 INFO  [org.hibernate.Version] (ServerService Thread Pool -- 72) HHH000412: Hibernate Core {4.2.18.Final-redhat-2}
	11:02:44,563 INFO  [org.hibernate.ejb.Ejb3Configuration] (ServerService Thread Pool -- 72) HHH000204: Processing PersistenceUnitInfo [
		name: search-pu ...]
	11:02:44,806 INFO  [org.hibernate.dialect.Dialect] (ServerService Thread Pool -- 72) HHH000400: Using dialect: org.hibernate.dialect.Oracle10gDialect
	11:02:45,201 INFO  [org.hibernate.engine.transaction.internal.TransactionFactoryInitiator] (ServerService Thread Pool -- 72) HHH000268: Transaction strategy: org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory
	11:02:45,341 INFO  [org.hibernate.search.Version] (ServerService Thread Pool -- 72) HSEARCH000034: Hibernate Search 4.6.0.Final-redhat-2
	11:02:45,664 INFO  [org.hibernate.search.backend.impl.lucene.SyncWorkProcessor] (ServerService Thread Pool -- 72) HSEARCH000230: Starting sync consumer thread for index 'com.mrobson.example.search.datamodel.Person'
	11:02:45,754 INFO  [org.infinispan.remoting.transport.jgroups.JGroupsTransport] (CacheStartThread,null,LuceneIndexesMetadata) ISPN000078: Starting JGroups Channel
	11:03:01,038 INFO  [org.infinispan.remoting.transport.jgroups.JGroupsTransport] (CacheStartThread,null,LuceneIndexesMetadata) ISPN000094: Received new cluster view: [mrobson-55465|0] (1) [mrobson-55465]
	11:03:01,047 INFO  [org.infinispan.remoting.transport.jgroups.JGroupsTransport] (CacheStartThread,null,LuceneIndexesMetadata) ISPN000079: Cache local address is mrobson-55465, physical addresses are [127.0.0.1:55288]
	11:03:01,055 INFO  [org.infinispan.factories.GlobalComponentRegistry] (CacheStartThread,null,LuceneIndexesMetadata) ISPN000128: Infinispan version: Infinispan 'Infinium' 6.3.0.Final-redhat-5
	11:03:01,336 INFO  [org.jboss.web] (ServerService Thread Pool -- 73) JBAS018210: Register web context: /masternode
	11:03:01,590 INFO  [org.jboss.as.server] (management-handler-thread - 1) JBAS015859: Deployed "search-master-1.0-SNAPSHOT.war" (runtime-name : "search-master-1.0-SNAPSHOT.war")

	cd ../search-slave
	mvn jboss-as:deploy -Dhost=127.0.0.1 -Dport=10009
	mvn jboss-as:deploy -Dhost=127.0.0.1 -Dport=10019

When we deploy the slave nodes, we see it deploy our SearchDAO, start our persistence unit, validate our database schema, start a JGroups channel and finally join the cluster with our master node.

	11:25:18,249 INFO  [org.jboss.as.repository] (management-handler-thread - 1) JBAS014900: Content added at location /EAP-6.4.0-Final-2/standalone/data/content/2c/51fc95bf57305889f1f80a8c2cb1fb1a5420ac/content
	11:25:18,267 INFO  [org.jboss.as.server.deployment] (MSC service thread 1-7) JBAS015876: Starting deployment of "search-slave-1.0-SNAPSHOT.war" (runtime-name: "search-slave-1.0-SNAPSHOT.war")
	11:25:18,523 INFO  [org.jboss.as.jpa] (MSC service thread 1-7) JBAS011401: Read persistence.xml for search-pu
	11:25:19,137 INFO  [org.jboss.as.ejb3.deployment.processors.EjbJndiBindingsDeploymentUnitProcessor] (MSC service thread 1-7) JNDI bindings for session bean named SearchDao in deployment unit deployment "search-slave-1.0-SNAPSHOT.war" are as follows:
		java:global/search-slave-1.0-SNAPSHOT/SearchDao!com.mrobson.example.search.slave.dao.SearchDao
		java:app/search-slave-1.0-SNAPSHOT/SearchDao!com.mrobson.example.search.slave.dao.SearchDao
		java:module/SearchDao!com.mrobson.example.search.slave.dao.SearchDao
		java:global/search-slave-1.0-SNAPSHOT/SearchDao
		java:app/search-slave-1.0-SNAPSHOT/SearchDao
		java:module/SearchDao
	11:25:19,426 INFO  [org.jboss.as.jpa] (ServerService Thread Pool -- 73) JBAS011402: Starting Persistence Unit Service 'search-slave-1.0-SNAPSHOT.war#search-pu'
	11:25:19,742 INFO  [org.hibernate.Version] (ServerService Thread Pool -- 73) HHH000412: Hibernate Core {4.2.18.Final-redhat-2}
	11:25:19,913 INFO  [org.hibernate.ejb.Ejb3Configuration] (ServerService Thread Pool -- 73) HHH000204: Processing PersistenceUnitInfo [name: search-pu ...]
	11:25:20,377 INFO  [org.hibernate.dialect.Dialect] (ServerService Thread Pool -- 73) HHH000400: Using dialect: org.hibernate.dialect.Oracle10gDialect
	11:25:20,490 INFO  [org.hibernate.engine.transaction.internal.TransactionFactoryInitiator] (ServerService Thread Pool -- 73) HHH000268: Transaction strategy: org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory
	11:25:20,838 INFO  [org.hibernate.search.Version] (ServerService Thread Pool -- 73) HSEARCH000034: Hibernate Search 4.6.0.Final-redhat-2
	11:25:20,984 INFO  [org.hibernate.tool.hbm2ddl.SchemaValidator] (ServerService Thread Pool -- 73) HHH000229: Running schema validator
	11:25:20,984 INFO  [org.hibernate.tool.hbm2ddl.SchemaValidator] (ServerService Thread Pool -- 73) HHH000102: Fetching database metadata
	11:25:21,581 INFO  [org.hibernate.tool.hbm2ddl.TableMetadata] (ServerService Thread Pool -- 73) HHH000261: Table found: PRODUCTCONFIG.SEARCH_PERSON
	11:25:21,582 INFO  [org.hibernate.tool.hbm2ddl.TableMetadata] (ServerService Thread Pool -- 73) HHH000037: Columns: [id, lastname, firstname]
	11:25:23,701 INFO  [org.infinispan.remoting.transport.jgroups.JGroupsTransport] (CacheStartThread,null,LuceneIndexesMetadata) ISPN000078: Starting JGroups Channel
	11:25:24,365 INFO  [org.infinispan.remoting.transport.jgroups.JGroupsTransport] (CacheStartThread,null,LuceneIndexesMetadata) ISPN000094: Received new cluster view: [mrobson-55465|1] (2) [mrobson-55465, mrobson-59810]
	11:25:24,379 INFO  [org.infinispan.remoting.transport.jgroups.JGroupsTransport] (CacheStartThread,null,LuceneIndexesMetadata) ISPN000079: Cache local address is mrobson-59810, physical addresses are [127.0.0.1:32839]
	11:25:24,461 INFO  [org.infinispan.factories.GlobalComponentRegistry] (CacheStartThread,null,LuceneIndexesMetadata) ISPN000128: Infinispan version: Infinispan 'Infinium' 6.3.0.Final-redhat-5
	11:25:25,019 INFO  [org.jboss.web] (ServerService Thread Pool -- 78) JBAS018210: Register web context: /searchnode
	11:25:25,401 INFO  [org.jboss.as.server] (management-handler-thread - 1) JBAS015859: Deployed "search-slave-1.0-SNAPSHOT.war" (runtime-name : "search-slave-1.0-SNAPSHOT.war")

Once all 3 servers have been deployed, you will see a 3 node cluster.

	11:27:43,073 INFO  [org.infinispan.remoting.transport.jgroups.JGroupsTransport] (CacheStartThread,null,LuceneIndexesMetadata) ISPN000094: Received new cluster view: [mrobson-55465|2] (3) [mrobson-55465, mrobson-59810, mrobson-8189]

When a new server joins the cluster, in the case of slave-2, you will see a cluster rebalance happen on the master node as follows.

	11:25:24,342 INFO  [org.infinispan.remoting.transport.jgroups.JGroupsTransport] (Incoming-2,mrobson-55465) ISPN000094: Received new cluster view: [mrobson-55465|1] (2) [mrobson-55465, mrobson-59810]
	11:25:24,665 INFO  [org.infinispan.CLUSTER] (remote-thread-2) ISPN000310: Starting cluster-wide rebalance for cache LuceneIndexesLocking, topology CacheTopology{id=1, rebalanceId=1, currentCH=ReplicatedConsistentHash{ns = 60, owners = (1)[mrobson-55465: 60]}, pendingCH=ReplicatedConsistentHash{ns = 60, owners = (2)[mrobson-55465: 30, mrobson-59810: 30]}, unionCH=null, actualMembers=[mrobson-55465, mrobson-59810]}
	11:25:24,665 INFO  [org.infinispan.CLUSTER] (remote-thread-0) ISPN000310: Starting cluster-wide rebalance for cache LuceneIndexesMetadata, topology CacheTopology{id=1, rebalanceId=1, currentCH=ReplicatedConsistentHash{ns = 60, owners = (1)[mrobson-55465: 60]}, pendingCH=ReplicatedConsistentHash{ns = 60, owners = (2)[mrobson-55465: 30, mrobson-59810: 30]}, unionCH=null, actualMembers=[mrobson-55465, mrobson-59810]}
	11:25:24,665 INFO  [org.infinispan.CLUSTER] (remote-thread-1) ISPN000310: Starting cluster-wide rebalance for cache LuceneIndexesData, topology CacheTopology{id=1, rebalanceId=1, currentCH=ReplicatedConsistentHash{ns = 60, owners = (1)[mrobson-55465: 60]}, pendingCH=ReplicatedConsistentHash{ns = 60, owners = (2)[mrobson-55465: 30, mrobson-59810: 30]}, unionCH=null, actualMembers=[mrobson-55465, mrobson-59810]}
	11:25:24,766 INFO  [org.infinispan.CLUSTER] (remote-thread-1) ISPN000336: Finished cluster-wide rebalance for cache LuceneIndexesLocking, topology id = 1
	11:25:24,780 INFO  [org.infinispan.CLUSTER] (remote-thread-1) ISPN000336: Finished cluster-wide rebalance for cache LuceneIndexesData, topology id = 1
	11:25:24,780 INFO  [org.infinispan.CLUSTER] (remote-thread-2) ISPN000336: Finished cluster-wide rebalance for cache LuceneIndexesMetadata, topology id = 1

4) test it out

There are 3 services available on the slave nodes, read, write and reindex.

	${HOST}:${PORT}/searchnode/read - preforms a read based on the criteria we outlined readPeople() and returns a count plus the time it took to preform the read

	${HOST}:${PORT}/searchnode/write - writes a new person to the database as outlined in savePeople() - writes are back-ended by JMS

	${HOST}:${PORT}/searchnode/reindex - preforms a reindex of all the database entries into JBoss Data Grid as outlined in reindexPeople() and returns a count plus the time it took to reindex - reindexing is back-ended by JMS

5) Done!  Let me know if you have any feedback, hopefully this serves as a good starting point to build out your architecture and use case.
