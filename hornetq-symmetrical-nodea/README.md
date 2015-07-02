HornetQ Configuration
=====================

Configuration explanation
-------------------------

All of the HornetQ configuration falls under the messaging subsystem in the standalone-*.xml.

	'<subsystem xmlns="urn:jboss:domain:messaging:1.4">'

On the first JMS node, we configure 3 unique HornetQ servers within the same subsystem, one live node, one backup node and one for the slave services.

	<hornetq-server name="search-live-one">
	<hornetq-server name="search-backup-two">
	<hornetq-server name="search-client">

The first, 'search-live-one', is a designated live server in group 'one' and forms a pair with 'search-backup-one' which runs on a different server. It is configured to do in-memory data replication which means you do not require shared storage between your live and backup server.

Indicates we want to use the HornetQ journal to persist messages.

	<persistence-enabled>true</persistence-enabled>

Indicated this server is not a backup and will start live.

	<backup>false</backup>

Indicates that if the live server fails, the backup server takes over, but if the live server comes back, we want it to take control from the backup immediately.

	<allow-failback>true</allow-failback>

Indicates that if the live server is gracefully shutdown, we want to backup server to take over.

	<failover-on-shutdown>true</failover-on-shutdown>

Indicates we do not want to use a shared file store and instead use in-memory replication for our live / backup pairs.

	<shared-store>false</shared-store>

Indicates we want to check the cluster for other live server with the same server ID during startup.

	<check-for-live-server>true</check-for-live-server>

Indicates that when using remote replication, this live server will only pair with a backup server from the group 'search-one'.

	<backup-group-name>search-one</backup-group-name>

Indicates the name of the cluster configuration to use for replication.

	<replication-clustername>search-cluster</replication-clustername>

We configure a broadcast group which acts as the mechanism by which server can broadcast their list of available connectors to all other listening servers.

	<broadcast-groups>
	    <broadcast-group name="bg-group1">
		<socket-binding>messaging-group</socket-binding>
		<broadcast-period>5000</broadcast-period>
		<connector-ref>
		    netty
		</connector-ref>
	    </broadcast-group>
	</broadcast-groups>

We configure a discovery group which acts as a listener to receive broadcasts from the cluster about updates to the available connectors.  This is how we dynamically learn about the available live HornetQ servers and how to connect to them.

	<discovery-groups>
	    <discovery-group name="dg-group1">
		<socket-binding>messaging-group</socket-binding>
		<refresh-timeout>10000</refresh-timeout>
	    </discovery-group>
	</discovery-groups>

We configure a cluster connection so that we can group our servers into an explicit symmetric cluster to load balance traffic between them.  It also serves to form a cluster between the live and backup server for in-memory replication.  Server that are discovered as part of the same cluster connection form a core bridge under the covers for this purpose.  Again, we use the discovery group to find and maintain the list of available connectors. 

	<cluster-connections>
	    <cluster-connection name="search-cluster">
		<address>jms</address>
		<connector-ref>netty</connector-ref>
		<check-period>1000</check-period>
		<connection-ttl>20000</connection-ttl>
		<use-duplicate-detection>true</use-duplicate-detection>
		<forward-when-no-consumers>false</forward-when-no-consumers>
		<max-hops>1</max-hops>
		<discovery-group-ref discovery-group-name="dg-group1"/>
	    </cluster-connection>
	</cluster-connections>

Finally we configure the JMS queue for index updates.

	<jms-destinations>
	    <jms-queue name="IndexingQueue">
		<entry name="/queue/indexupdates"/>
		<durable>true</durable>
	    </jms-queue>
	</jms-destinations>

The second, 'search-backup-two', is a designated backup server in group 'two' and forms a pair with 'search-live-two' which runs on a different server. It is configured to do in-memory data replication which means you do not require shared storage between your live and backup server.  

It's configuration is largely the same as the live server, so we will only focus on the main differences.

Indicated this server is a backup and will always start as a backup.

	<backup>true</backup>

Indicates that when using remote replication, this backup server will only pair with a live server from the group 'search-two'.

	<backup-group-name>search-two</backup-group-name>

The third, 'search-client', is a basic server which provides a discovery group and a connection factory that local and remote clients can use to connect to HornetQ.

We configure a discovery group which acts as a listener to receive broadcasts from the cluster about updates to the available connectors.  This is how we dynamically learn about the available live HornetQ servers and how to connect to them.

	<discovery-groups>
	    <discovery-group name="dg-group1">
		<socket-binding>messaging-group</socket-binding>
		<refresh-timeout>10000</refresh-timeout>
	    </discovery-group>
	</discovery-groups>

Finally we configure a standard connection factory which clients can lookup in JNDI to connect to the current live HornetQ server.  Again, it uses the discovery group so it can dynamically learn about and connect to the available live HornetQ servers in the cluster.

	<connection-factory name="RemoteConnectionFactory">
		<discovery-group-ref discovery-group-name="dg-group1"/>
		<entries>
			<entry name="java:/RemoteConnectionFactory"/>
			<entry name="java:jboss/exported/jms/RemoteConnectionFactory"/>
		</entries>
		<ha>true</ha>
		<client-failure-check-period>5000</client-failure-check-period>
		<connection-ttl>20000</connection-ttl>
		<block-on-acknowledge>false</block-on-acknowledge>
		<retry-interval>1000</retry-interval>
		<retry-interval-multiplier>1.0</retry-interval-multiplier>
		<reconnect-attempts>-1</reconnect-attempts>
		<failover-on-initial-connection>true</failover-on-initial-connection>
	</connection-factory>
