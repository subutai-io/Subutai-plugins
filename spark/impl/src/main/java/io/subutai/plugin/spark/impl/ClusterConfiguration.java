package io.subutai.plugin.spark.impl;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.peer.PeerException;
import io.subutai.common.protocol.CustomProxyConfig;
import io.subutai.common.settings.Common;
import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.plugincommon.api.ClusterConfigurationException;
import io.subutai.core.plugincommon.api.ClusterConfigurationInterface;
import io.subutai.core.plugincommon.api.ClusterException;
import io.subutai.plugin.spark.api.SparkClusterConfig;


/**
 * Configures Spark cluster
 */
public class ClusterConfiguration implements ClusterConfigurationInterface<SparkClusterConfig>
{
    private static final Logger LOG = LoggerFactory.getLogger( ClusterConfiguration.class );

    private final SparkImpl manager;
    private final TrackerOperation po;


    public ClusterConfiguration( SparkImpl manager, TrackerOperation po )
    {
        this.manager = manager;
        this.po = po;
    }


    @Override
    public void configureCluster( final SparkClusterConfig config, final Environment environment )
            throws ClusterConfigurationException
    {
        final EnvironmentContainerHost master;
        final Set<EnvironmentContainerHost> slaves;
        final Set<EnvironmentContainerHost> allnodes = new HashSet<>();
        String vlanString = UUID.randomUUID().toString();
        try
        {
            master = environment.getContainerHostById( config.getMasterNodeId() );
            slaves = environment.getContainerHostsByIds( config.getSlaveIds() );

            allnodes.addAll( slaves );
            allnodes.add( master );

            for ( final EnvironmentContainerHost node : allnodes )
            {
                configureNode( node );
            }

            String slaveIPs = collectSlavesIp( slaves );

            // set slaves
            executeCommand( master, Commands.getSetSlavesCommand( slaveIPs ) );

            configureReverseProxy( master, master.getHostname().toLowerCase() + ".spark", config, vlanString );

            startCluster( master, environment, config );

            po.addLog( "Slave(s) successfully registered" );

            po.addLog( "Saving cluster info..." );

            config.setEnvironmentId( environment.getId() );
            config.setVlan( vlanString );
            config.setPeerId( master.getPeerId() );
            manager.saveConfig( config );
        }
        catch ( ContainerHostNotFoundException e )
        {
            throw new ClusterConfigurationException( e );
        }
        catch ( ClusterException e )
        {
            e.printStackTrace();
        }
    }


    private void configureNode( final EnvironmentContainerHost node ) throws ClusterConfigurationException
    {
        // set env variables
        executeCommand( node, Commands.getSetEnvVariablesCommand() );

        // set ip
        executeCommand( node,
                Commands.getSetIpCommand( node.getInterfaceByName( Common.DEFAULT_CONTAINER_INTERFACE ).getIp() ) );

        // set worker core
        executeCommand( node, Commands.getSetWorkerCoreCommand( "2" ) );
    }


    public CommandResult executeCommand( EnvironmentContainerHost host, RequestBuilder command )
            throws ClusterConfigurationException
    {

        CommandResult result;
        try
        {
            result = host.execute( command );
        }
        catch ( CommandException e )
        {
            throw new ClusterConfigurationException( e );
        }
        if ( !result.hasSucceeded() )
        {
            throw new ClusterConfigurationException( String.format( "Error on container %s: %s", host.getHostname(),
                    result.hasCompleted() ? result.getStdErr() : "Command timed out" ) );
        }
        return result;
    }


    private String collectSlavesIp( final Set<EnvironmentContainerHost> slaves )
    {
        StringBuilder sb = new StringBuilder();

        for ( final EnvironmentContainerHost slave : slaves )
        {
            sb.append( slave.getInterfaceByName( Common.DEFAULT_CONTAINER_INTERFACE ).getIp() ).append( "\n" );
        }

        if ( !sb.toString().isEmpty() )
        {
            sb.replace( sb.toString().length() - 1, sb.toString().length(), "" );
        }

        return sb.toString();
    }


    private void configureReverseProxy( final EnvironmentContainerHost master, final String domainName,
                                        final SparkClusterConfig config, final String vlanString )
    {
        try
        {
            CustomProxyConfig proxyConfig =
                    new CustomProxyConfig( config.getEnvironmentId(), vlanString, domainName, master.getId() );
            proxyConfig.setPort( 8080 );
            master.getPeer().addCustomProxy( proxyConfig );
        }
        catch ( PeerException e )
        {
            LOG.error( "Error to set proxy settings: ", e );
            po.addLogFailed( "Error to set proxy settings." );
            e.printStackTrace();
        }
    }


    public void removeNode( final EnvironmentContainerHost master, final EnvironmentContainerHost node,
                            final SparkClusterConfig config, final Environment environment )
            throws ClusterConfigurationException, ContainerHostNotFoundException
    {
        // Stopping cluster
        stopCluster( master, environment, config );

        po.addLog( "Unregistering slave from master..." );

        String slaveIp = node.getInterfaceByName( Common.DEFAULT_CONTAINER_INTERFACE ).getIp();
        RequestBuilder clearSlavesCommand = manager.getCommands().getClearSlaveCommand( slaveIp );
        executeCommand( master, clearSlavesCommand );

        po.addLog( "Successfully unregistered slave from master..." );

        config.getSlaveIds().remove( node.getId() );

        boolean uninstall = !node.getId().equals( config.getMasterNodeId() );
        if ( uninstall )
        {
            po.addLog( "Uninstalling Spark..." );
            RequestBuilder uninstallCommand = manager.getCommands().getUninstallCommand();
            executeCommand( node, uninstallCommand );
        }

        startCluster( master, environment, config );
    }


    private void stopCluster( final EnvironmentContainerHost master, final Environment environment,
                              final SparkClusterConfig config )
            throws ClusterConfigurationException, ContainerHostNotFoundException
    {
        po.addLog( "Stopping cluster..." );

        executeCommand( master, manager.getCommands().getStopMasterCommand() );

        Set<EnvironmentContainerHost> slaves = environment.getContainerHostsByIds( config.getSlaveIds() );

        for ( final EnvironmentContainerHost slave : slaves )
        {
            executeCommand( slave, manager.getCommands().getStopSlaveCommand() );
        }
    }


    private void startCluster( final EnvironmentContainerHost master, final Environment environment,
                               final SparkClusterConfig config )
            throws ClusterConfigurationException, ContainerHostNotFoundException
    {
        po.addLog( "Stopping cluster..." );

        executeCommand( master, manager.getCommands().getStartMasterCommand() );

        Set<EnvironmentContainerHost> slaves = environment.getContainerHostsByIds( config.getSlaveIds() );

        for ( final EnvironmentContainerHost slave : slaves )
        {
            executeCommand( slave, manager.getCommands().getStartSlaveCommand( master.getHostname() ) );
        }
    }


    public void addNode( final EnvironmentContainerHost master, final EnvironmentContainerHost node,
                         final SparkClusterConfig config, final Environment environment )
            throws ContainerHostNotFoundException, ClusterConfigurationException
    {
        // Stopping cluster
        stopCluster( master, environment, config );

        po.addLog( "Registering slave with master..." );

        // Configure new slave
        configureNode( node );

        Set<EnvironmentContainerHost> slaves = environment.getContainerHostsByIds( config.getSlaveIds() );
        String slaveIPs = collectSlavesIp( slaves );

        // set slaves
        executeCommand( master, Commands.getSetSlavesCommand( slaveIPs ) );

        startCluster( master, environment, config );
    }
}
