package org.safehaus.subutai.plugin.accumulo.impl;


import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.safehaus.subutai.common.command.CommandException;
import org.safehaus.subutai.common.command.CommandResult;
import org.safehaus.subutai.common.command.CommandUtil;
import org.safehaus.subutai.common.command.RequestBuilder;
import org.safehaus.subutai.common.environment.ContainerHostNotFoundException;
import org.safehaus.subutai.common.environment.Environment;
import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.common.peer.Host;
import org.safehaus.subutai.common.settings.Common;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.common.util.CollectionUtil;
import org.safehaus.subutai.core.metric.api.MonitorException;
import org.safehaus.subutai.plugin.accumulo.api.AccumuloClusterConfig;
import org.safehaus.subutai.plugin.common.api.ClusterConfigurationException;
import org.safehaus.subutai.plugin.common.api.ClusterSetupException;
import org.safehaus.subutai.plugin.common.api.ClusterSetupStrategy;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.zookeeper.api.ZookeeperClusterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


/**
 * This is an accumulo cluster setup strategy over existing Hadoop & ZK clusters
 */
public class AccumuloOverZkNHadoopSetupStrategy implements ClusterSetupStrategy
{

    private static final Logger LOGGER = LoggerFactory.getLogger( AccumuloOverZkNHadoopSetupStrategy.class );
    private final AccumuloImpl accumuloManager;
    private final TrackerOperation trackerOperation;
    private final AccumuloClusterConfig accumuloClusterConfig;
    private final HadoopClusterConfig hadoopClusterConfig;
    private final Environment environment;
    private CommandUtil commandUtil;


    public AccumuloOverZkNHadoopSetupStrategy( final Environment environment,
                                               final AccumuloClusterConfig accumuloClusterConfig,
                                               final HadoopClusterConfig hadoopClusterConfig,
                                               final TrackerOperation trackerOperation,
                                               final AccumuloImpl accumuloManager )
    {
        Preconditions.checkNotNull( accumuloClusterConfig, "Accumulo cluster config is null" );
        Preconditions.checkNotNull( trackerOperation, "Product operation tracker is null" );
        Preconditions.checkNotNull( accumuloManager, "Accumulo manager is null" );

        this.trackerOperation = trackerOperation;
        this.accumuloClusterConfig = accumuloClusterConfig;
        this.hadoopClusterConfig = hadoopClusterConfig;
        this.accumuloManager = accumuloManager;
        this.environment = environment;
        this.commandUtil = new CommandUtil();
    }


    public AccumuloClusterConfig setup1() throws ClusterSetupException
    {
        if ( accumuloClusterConfig.getMasterNode() == null || accumuloClusterConfig.getGcNode() == null
                || accumuloClusterConfig.getMonitor() == null ||
                Strings.isNullOrEmpty( accumuloClusterConfig.getClusterName() ) ||
                CollectionUtil.isCollectionEmpty( accumuloClusterConfig.getTracers() ) ||
                CollectionUtil.isCollectionEmpty( accumuloClusterConfig.getSlaves() ) ||
                Strings.isNullOrEmpty( accumuloClusterConfig.getInstanceName() ) ||
                Strings.isNullOrEmpty( accumuloClusterConfig.getPassword() ) )
        {
            throw new ClusterSetupException( "Malformed configuration" );
        }

        if ( accumuloManager.getCluster( accumuloClusterConfig.getClusterName() ) != null )
        {
            trackerOperation.addLogFailed( "There is already a cluster with that name" );
            throw new ClusterSetupException(
                    String.format( "Cluster with name '%s' already exists", accumuloClusterConfig.getClusterName() ) );
        }

        HadoopClusterConfig hadoopClusterConfig =
                accumuloManager.getHadoopManager().getCluster( accumuloClusterConfig.getHadoopClusterName() );

        if ( hadoopClusterConfig == null )
        {
            throw new ClusterSetupException( String.format( "Hadoop cluster with name '%s' not found",
                    accumuloClusterConfig.getHadoopClusterName() ) );
        }

        ZookeeperClusterConfig zookeeperClusterConfig =
                accumuloManager.getZkManager().getCluster( accumuloClusterConfig.getZookeeperClusterName() );
        if ( zookeeperClusterConfig == null )
        {
            throw new ClusterSetupException( String.format( "ZK cluster with name '%s' not found",
                    accumuloClusterConfig.getZookeeperClusterName() ) );
        }


        if ( !hadoopClusterConfig.getAllNodes().containsAll( accumuloClusterConfig.getAllNodes() ) )
        {
            throw new ClusterSetupException( String.format( "Not all supplied nodes belong to Hadoop cluster %s",
                    hadoopClusterConfig.getClusterName() ) );
        }


        /** start hadoop and zk clusters */
        accumuloManager.getHadoopManager().startNameNode( hadoopClusterConfig );
        accumuloManager.getZkManager().startAllNodes( zookeeperClusterConfig.getClusterName() );


        trackerOperation.addLog( "Installing Accumulo..." );

        Set<Host> hostSet = Util.getHosts( accumuloClusterConfig, environment );
        try
        {
            Map<Host, CommandResult> resultMap =
                    commandUtil.executeParallel( new RequestBuilder( Commands.checkIfInstalled ), hostSet );
            if ( !Util.isProductInstalledOnAllNodes( resultMap, hostSet, HadoopClusterConfig.PRODUCT_NAME ) )
            {
                trackerOperation.addLogFailed( "Hadoop is not installed on all nodes" );
                return null;
            }

            resultMap = commandUtil.executeParallel( new RequestBuilder( Commands.checkIfInstalled ), hostSet );
            if ( Util.isProductNotInstalledOnAllNodes( resultMap, hostSet, AccumuloClusterConfig.PRODUCT_NAME ) )
            {
                trackerOperation.addLogFailed( "Some nodes has already accumulo package installed. Aborting..." );
                return null;
            }

            resultMap = commandUtil.executeParallel( Commands.getInstallCommand(), hostSet );
            if ( Util.isAllSuccessful( resultMap, hostSet ) )
            {
                resultMap = commandUtil.executeParallel( new RequestBuilder( Commands.checkIfInstalled ), hostSet );
                if ( !Util.isProductInstalledOnAllNodes( resultMap, hostSet, AccumuloClusterConfig.PRODUCT_NAME ) )
                {
                    // if package are not installed completely, send uninstall command
                    commandUtil.executeParallel( new RequestBuilder( Commands.uninstallCommand ), hostSet );
                    trackerOperation.addLogFailed( "Accumulo could not get installed on all nodes" );
                    throw new ClusterSetupException( String.format( "Couldn't install Accumulo on all nodes" ) );
                }
                trackerOperation.addLog( "Accumulo package is installed on all nodes successfully" );
            }
            else
            {
                trackerOperation.addLogFailed( "Accumulo is NOT installed on all nodes successfully !" );
                throw new ClusterSetupException( String.format( "Couldn't install Accumulo on all nodes" ) );
            }
        }
        catch ( CommandException e )
        {
            e.printStackTrace();
        }

        try
        {
            accumuloManager.subscribeToAlerts( environment );
            new ClusterConfiguration( accumuloManager, trackerOperation )
                    .configureCluster( accumuloClusterConfig, environment );
        }
        catch ( ClusterConfigurationException | MonitorException e )
        {
            throw new ClusterSetupException( e.getMessage() );
        }
        return accumuloClusterConfig;
    }


    @Override
    public AccumuloClusterConfig setup() throws ClusterSetupException
    {
        if ( accumuloClusterConfig.getMasterNode() == null || accumuloClusterConfig.getGcNode() == null
                || accumuloClusterConfig.getMonitor() == null ||
                Strings.isNullOrEmpty( accumuloClusterConfig.getClusterName() ) ||
                CollectionUtil.isCollectionEmpty( accumuloClusterConfig.getTracers() ) ||
                CollectionUtil.isCollectionEmpty( accumuloClusterConfig.getSlaves() ) ||
                Strings.isNullOrEmpty( accumuloClusterConfig.getInstanceName() ) ||
                Strings.isNullOrEmpty( accumuloClusterConfig.getPassword() ) )
        {
            throw new ClusterSetupException( "Malformed configuration" );
        }

        if ( accumuloManager.getCluster( accumuloClusterConfig.getClusterName() ) != null )
        {
            trackerOperation.addLogFailed( "There is already a cluster with that name" );
            throw new ClusterSetupException(
                    String.format( "Cluster with name '%s' already exists", accumuloClusterConfig.getClusterName() ) );
        }

        HadoopClusterConfig hadoopClusterConfig =
                accumuloManager.getHadoopManager().getCluster( accumuloClusterConfig.getHadoopClusterName() );

        if ( hadoopClusterConfig == null )
        {
            throw new ClusterSetupException( String.format( "Hadoop cluster with name '%s' not found",
                    accumuloClusterConfig.getHadoopClusterName() ) );
        }

        ZookeeperClusterConfig zookeeperClusterConfig =
                accumuloManager.getZkManager().getCluster( accumuloClusterConfig.getZookeeperClusterName() );
        if ( zookeeperClusterConfig == null )
        {
            throw new ClusterSetupException( String.format( "ZK cluster with name '%s' not found",
                    accumuloClusterConfig.getZookeeperClusterName() ) );
        }


        if ( !hadoopClusterConfig.getAllNodes().containsAll( accumuloClusterConfig.getAllNodes() ) )
        {
            throw new ClusterSetupException( String.format( "Not all supplied nodes belong to Hadoop cluster %s",
                    hadoopClusterConfig.getClusterName() ) );
        }


        /** start hadoop and zk clusters */
        accumuloManager.getHadoopManager().startNameNode( hadoopClusterConfig );
        accumuloManager.getZkManager().startAllNodes( zookeeperClusterConfig.getClusterName() );

        trackerOperation.addLog( "Installing Accumulo..." );
        for ( UUID uuid : accumuloClusterConfig.getAllNodes() )
        {
            CommandResult result;
            ContainerHost host;
            try
            {
                host = environment.getContainerHostById( uuid );
            }
            catch ( ContainerHostNotFoundException e )
            {
                String msg =
                        String.format( "Container host with id: %s doesn't exists in environment: %s", uuid.toString(),
                                environment.getName() );
                trackerOperation.addLogFailed( msg );
                LOGGER.error( msg, e );
                return null;
            }
            if ( checkIfProductIsInstalled( host, HadoopClusterConfig.PRODUCT_NAME ) )
            {
                if ( !checkIfProductIsInstalled( host, AccumuloClusterConfig.PRODUCT_PACKAGE ) )
                {
                    try
                    {
                        host.execute( Commands.getInstallCommand() );
                        result = host.execute(
                                Commands.getPackageQueryCommand( AccumuloClusterConfig.PRODUCT_PACKAGE ) );
                        String output = result.getStdOut() + result.getStdErr();
                        if ( output.contains( "install ok installed" ) )
                        {
                            trackerOperation.addLog(
                                    AccumuloClusterConfig.PRODUCT_KEY + " is installed on node " + host.getHostname() );
                        }
                        else
                        {
                            trackerOperation.addLogFailed(
                                    AccumuloClusterConfig.PRODUCT_KEY + " is not installed on node " + host
                                            .getTemplateName() );
                            throw new ClusterSetupException( String.format( "Couldn't install %s package on node %s",
                                    Common.PACKAGE_PREFIX + AccumuloClusterConfig.PRODUCT_NAME, host.getHostname() ) );
                        }
                    }
                    catch ( CommandException e )
                    {
                        String msg = String.format( "Error executing install command on container host." );
                        trackerOperation.addLogFailed( msg );
                        LOGGER.error( msg, e );
                    }
                }
                else
                {
                    trackerOperation
                            .addLog( String.format( "Node %s already has Accumulo installed", host.getHostname() ) );
                }
            }
            else
            {
                throw new ClusterSetupException(
                        String.format( "Node %s has no Hadoop installation", host.getHostname() ) );
            }
        }

        try
        {
            accumuloManager.subscribeToAlerts( environment );
            new ClusterConfiguration( accumuloManager, trackerOperation )
                    .configureCluster( accumuloClusterConfig, environment );
        }
        catch ( ClusterConfigurationException | MonitorException e )
        {
            throw new ClusterSetupException( e.getMessage() );
        }
        return accumuloClusterConfig;
    }


    private boolean checkIfProductIsInstalled( ContainerHost containerHost, String productName )
    {
        boolean isInstalled = false;
        try
        {
            CommandResult result = containerHost.execute( new RequestBuilder( Commands.checkIfInstalled ) );
            if ( result.getStdOut().toLowerCase().contains( productName.toLowerCase() ) )
            {
                isInstalled = true;
            }
        }
        catch ( CommandException e )
        {
            e.printStackTrace();
        }
        return isInstalled;
    }
}
