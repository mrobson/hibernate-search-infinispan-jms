HornetQ Configuration
=====================

Configuration explanation
-------------------------

All of the HornetQ configuration falls under the messaging subsystem in the standalone-*.xml.

	'<subsystem xmlns="urn:jboss:domain:messaging:1.4">'

On the second JMS node, we configure 3 unique HornetQ servers within the same subsystem, one live node, one backup node and one for the slave services.

	<hornetq-server name="search-live-two">
	<hornetq-server name="search-backup-one">
	<hornetq-server name="search-client">

The first, 'search-live-two', is a designated live server in group 'two' and forms a pair with 'search-backup-two' which runs on a different server. It is configured to do in-memory data replication which means you do not require shared storage between your live and backup server.

It's configuration is identical to 'service-live-one' on 'hornetq-symmetrical-nodea' aside for 1 item.

Indicates that when using remote replication, this live server will only pair with a backup server from the group 'search-two'.

	<backup-group-name>search-two</backup-group-name>

The second, 'search-backup-one', is a designated backup server in group 'one' and forms a pair with 'search-live-one' which runs on a different server. It is configured to do in-memory data replication which means you do not require shared storage between your live and backup server.  

It's configuration is identical to 'service-backup-two' on 'hornetq-symmetrical-nodea' aside for 1 item.

Indicates that when using remote replication, this backup server will only pair with a live server from the group 'search-one'.

	<backup-group-name>search-one</backup-group-name>

The third, 'search-client', is a basic server which provides a discovery group and a connection factory that local and remote clients can use to connect to HornetQ.

It's configuration is identical to 'search-client' on 'hornetq-symmetrical-nodea'.
