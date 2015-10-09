package io.subutai.plugin.cassandra.impl.handler;


import com.google.common.base.Preconditions;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.plugin.cassandra.api.CassandraClusterConfig;
import io.subutai.plugin.cassandra.impl.CassandraImpl;
import io.subutai.plugin.cassandra.impl.ClusterConfiguration;
import io.subutai.plugin.cassandra.impl.Commands;
import io.subutai.plugin.common.api.AbstractOperationHandler;
import io.subutai.plugin.common.api.ClusterConfigurationException;
import io.subutai.plugin.common.api.ClusterException;
import io.subutai.plugin.common.api.NodeOperationType;


/**
 * This class handles operations that are related to just one node.
 */
public class NodeOperationHandler extends AbstractOperationHandler<CassandraImpl, CassandraClusterConfig>
{

    private String clusterName;
    private String hostname;
    private NodeOperationType operationType;


    public NodeOperationHandler( final CassandraImpl manager, final String clusterName, final String hostname,
                                 NodeOperationType operationType )
    {
        super( manager, clusterName );
        this.hostname = hostname;
        this.clusterName = clusterName;
        this.operationType = operationType;
        this.trackerOperation = manager.getTracker().createTrackerOperation( CassandraClusterConfig.PRODUCT_KEY,
                String.format( "Creating %s tracker object...", clusterName ) );
    }


    @Override
    public void run()
    {
        CassandraClusterConfig config = manager.getCluster( clusterName );
        if ( config == null )
        {
            trackerOperation.addLogFailed( String.format( "Cluster with name %s does not exist", clusterName ) );
            return;
        }

        Environment environment;
        try
        {
            environment = manager.getEnvironmentManager().loadEnvironment( config.getEnvironmentId() );
        }
        catch ( EnvironmentNotFoundException e )
        {
            trackerOperation.addLogFailed( "Cluster environment not found" );
            return;
        }

        EnvironmentContainerHost host = null;
        try
        {
            host = environment.getContainerHostByHostname( hostname );
        }
        catch ( ContainerHostNotFoundException e )
        {
            e.printStackTrace();
        }

        if ( host == null )
        {
            trackerOperation.addLogFailed( String.format( "No Container with ID %s", hostname ) );
            return;
        }

        if ( !config.getAllNodes().contains( host.getId() ) )
        {
            trackerOperation
                    .addLogFailed( String.format( "Node %s does not belong to %s cluster.", hostname, clusterName ) );
            return;
        }

        try
        {
            CommandResult result;
            switch ( operationType )
            {
                case START:
                    result = host.execute( new RequestBuilder( Commands.startCommand ) );
                    logResults( trackerOperation, result );
                    break;
                case STOP:
                    result = host.execute( new RequestBuilder( Commands.stopCommand ) );
                    logResults( trackerOperation, result );
                    break;
                case STATUS:
                    result = host.execute( new RequestBuilder( Commands.statusCommand ) );
                    logResults( trackerOperation, result );
                    break;
                case DESTROY:
                    destroyNode( host );
                    break;
            }
        }
        catch ( CommandException e )
        {
            trackerOperation.addLogFailed( String.format( "Command failed, %s", e.getMessage() ) );
        }
    }


    public void destroyNode( EnvironmentContainerHost host )
    {

        EnvironmentManager environmentManager = manager.getEnvironmentManager();
        try
        {
            CassandraClusterConfig config = manager.getCluster( clusterName );
            config.getNodes().remove( host.getId() );
            manager.saveConfig( config );
            // configure cluster again
            ClusterConfiguration configurator = new ClusterConfiguration( trackerOperation, manager );
            try
            {
                configurator
                        .configureCluster( config, environmentManager.loadEnvironment( config.getEnvironmentId() ) );
            }
            catch ( EnvironmentNotFoundException | ClusterConfigurationException e )
            {
                e.printStackTrace();
            }
            trackerOperation.addLog( String.format( "Cluster information is updated" ) );
            trackerOperation.addLogDone( String.format( "Container %s is removed from cluster", host.getHostname() ) );
        }
        catch ( ClusterException e )
        {
            e.printStackTrace();
        }
    }


    public static void logResults( TrackerOperation po, CommandResult result )
    {
        Preconditions.checkNotNull( result );
        String message = "UNKNOWN";
        if ( result.getExitCode() == 0 )
        {
            message = result.getStdOut();
        }

        else if ( result.getExitCode() == 768 )
        {
            message = "cassandra is not running";
        }
        po.addLogDone( message );
    }
}
