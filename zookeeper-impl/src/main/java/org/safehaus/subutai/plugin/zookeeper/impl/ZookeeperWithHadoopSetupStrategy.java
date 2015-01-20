package org.safehaus.subutai.plugin.zookeeper.impl;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.common.peer.PeerException;
import org.safehaus.subutai.common.settings.Common;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.core.metric.api.MonitorException;
import org.safehaus.subutai.plugin.common.api.ClusterConfigurationException;
import org.safehaus.subutai.plugin.common.api.ClusterSetupException;
import org.safehaus.subutai.plugin.common.api.ClusterSetupStrategy;
import org.safehaus.subutai.plugin.zookeeper.api.ZookeeperClusterConfig;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


/**
 * ZK cluster setup strategy using combo template ZK+Hadoop
 */
public class ZookeeperWithHadoopSetupStrategy implements ClusterSetupStrategy
{

    private final ZookeeperClusterConfig zookeeperClusterConfig;
    private final TrackerOperation po;
    private final ZookeeperImpl zookeeperManager;
    private final Environment environment;


    public ZookeeperWithHadoopSetupStrategy( final Environment environment,
                                             final ZookeeperClusterConfig zookeeperClusterConfig,
                                             final TrackerOperation po, final ZookeeperImpl zookeeperManager )
    {
        Preconditions.checkNotNull( zookeeperClusterConfig, "ZK cluster config is null" );
        Preconditions.checkNotNull( environment, "Environment is null" );
        Preconditions.checkNotNull( po, "Product operation tracker is null" );
        Preconditions.checkNotNull( zookeeperManager, "ZK manager is null" );

        this.zookeeperClusterConfig = zookeeperClusterConfig;
        this.po = po;
        this.zookeeperManager = zookeeperManager;
        this.environment = environment;
    }


    @Override
    public ZookeeperClusterConfig setup() throws ClusterSetupException
    {
        if ( Strings.isNullOrEmpty( zookeeperClusterConfig.getClusterName() ) ||
                Strings.isNullOrEmpty( zookeeperClusterConfig.getTemplateName() ) ||
                zookeeperClusterConfig.getNumberOfNodes() <= 0 )
        {
            throw new ClusterSetupException( "Malformed configuration" );
        }

        if ( zookeeperManager.getCluster( zookeeperClusterConfig.getClusterName() ) != null )
        {
            throw new ClusterSetupException(
                    String.format( "Cluster with name '%s' already exists", zookeeperClusterConfig.getClusterName() ) );
        }

        if ( environment.getContainerHosts().size() < zookeeperClusterConfig.getNumberOfNodes() )
        {
            throw new ClusterSetupException( String.format( "Environment needs to have %d nodes but has only %d nodes",
                    zookeeperClusterConfig.getNumberOfNodes(), environment.getContainerHosts().size() ) );
        }


        Set<ContainerHost> zookeeperNodes = new HashSet<>();
        for ( ContainerHost containerHost : environment.getContainerHosts() )
        {
            try
            {
                if ( containerHost.getTemplate().getProducts()
                                  .contains( Common.PACKAGE_PREFIX + ZookeeperClusterConfig.PRODUCT_NAME ) )
                {
                    zookeeperNodes.add( containerHost );
                }
            }
            catch ( PeerException e )
            {
                e.printStackTrace();
            }
        }

        if ( zookeeperNodes.size() < zookeeperClusterConfig.getNumberOfNodes() )
        {
            throw new ClusterSetupException( String.format(
                    "Environment needs to have %d nodes with ZK installed but has only %d nodes with ZK installed",
                    zookeeperClusterConfig.getNumberOfNodes(), zookeeperNodes.size() ) );
        }

        Set<UUID> zookeeperIDs = new HashSet<>();
        for ( ContainerHost containerHost : zookeeperNodes )
        {
            zookeeperIDs.add( containerHost.getId() );
        }
        zookeeperClusterConfig.setNodes( zookeeperIDs );

        //check if node agent is connected
        for ( ContainerHost node : zookeeperNodes )
        {
            if ( environment.getContainerHostByHostname( node.getHostname() ) == null )
            {
                throw new ClusterSetupException( String.format( "Node %s is not connected", node.getHostname() ) );
            }
        }

        //configure ZK cluster
        try
        {
            new ClusterConfiguration( zookeeperManager, po ).configureCluster( zookeeperClusterConfig, environment );

            po.addLog( "Saving cluster information to database..." );

            zookeeperClusterConfig.setEnvironmentId( environment.getId() );

            zookeeperManager.getPluginDAO()
                            .saveInfo( ZookeeperClusterConfig.PRODUCT_KEY, zookeeperClusterConfig.getClusterName(),
                                    zookeeperClusterConfig );
            po.addLog( "Cluster information saved to database" );
            zookeeperManager.subscribeToAlerts( environment );
        }
        catch ( MonitorException | ClusterConfigurationException e )
        {
            throw new ClusterSetupException( e.getMessage() );
        }
        return zookeeperClusterConfig;
    }
}
