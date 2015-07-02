HornetQ Configuration
=====================

Configuration explanation
-------------------------

All of the HornetQ configuration falls under the messaging subsystem in the standalone-*.xml.

	'<subsystem xmlns="urn:jboss:domain:messaging:1.4">'

For the master node, we have configured 1 HornetQ server.

	<hornetq-server name="hornetq-search-master">

Because we're running all 3 instances on 1 server, we have configured a unique socket-binding for netty.

	<connectors>
	    <netty-connector name="netty" socket-binding="messaging-mdb"/>
	</connectors>
	<acceptors>
	    <netty-acceptor name="netty" socket-binding="messaging-mdb"/>
	</acceptors>

	<socket-binding name="messaging-mdb" port="5447"/>

We configure a discovery group which acts as a listener to receive broadcasts from the cluster about updates to the available connectors.  This is how we dynamically learn about the available live HornetQ servers and how to connect to them.

	<discovery-groups>
	    <discovery-group name="dg-group1">
		<socket-binding>messaging-group</socket-binding>
		<refresh-timeout>10000</refresh-timeout>
	    </discovery-group>
	</discovery-groups>

Finally we configure a pooled-connection-factory which we can use from our MDB.  The pooled-connection-factory is designed to be used by local clients only and creates a special inbound connection factory for MDBs.  We specify the discovery group on the pooled-connection-factory so that it can dynamically learn about and connect to the available live HornetQ servers in the cluster.

	<pooled-connection-factory name="hornetq-ra">
		<transaction mode="none"/>
		<discovery-group-ref discovery-group-name="dg-group1"/>
		<entries>
			<entry name="java:/JmsXA"/>
		</entries>
	</pooled-connection-factory>

