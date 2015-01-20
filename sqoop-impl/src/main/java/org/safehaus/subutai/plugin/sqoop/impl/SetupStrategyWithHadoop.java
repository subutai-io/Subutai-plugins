package org.safehaus.subutai.plugin.sqoop.impl;


import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.common.peer.PeerException;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.plugin.common.api.ClusterSetupException;
import org.safehaus.subutai.plugin.common.api.ConfigBase;
import org.safehaus.subutai.plugin.sqoop.api.SqoopConfig;


class SetupStrategyWithHadoop extends SqoopSetupStrategy
{

    public SetupStrategyWithHadoop( SqoopImpl manager, SqoopConfig config, Environment environment, TrackerOperation to )
    {
        super( manager, config, environment, to );
    }


    @Override
    public ConfigBase setup() throws ClusterSetupException
    {

        checkConfig();

        if ( environment.getContainerHosts() == null || environment.getContainerHosts().isEmpty() )
        {
            throw new ClusterSetupException( "Environment has no nodes" );
        }

        config.setEnvironmentId( environment.getId() );
        config.getNodes().clear();
        config.getHadoopNodes().clear();

        for ( ContainerHost n : environment.getContainerHosts() )
        {
            if ( !n.isConnected() )
            {
                throw new ClusterSetupException( "Node is not connected: " + n.getHostname() );
            }
            try
            {
                config.getHadoopNodes().add( n.getId() );
                if ( n.getTemplate().getProducts().contains( CommandFactory.PACKAGE_NAME ) )
                {
                    config.getNodes().add( n.getId() );
                }
            }
            catch ( PeerException ex )
            {
                throw new ClusterSetupException( ex );
            }
        }
        if ( config.getNodes().isEmpty() )
        {
            throw new ClusterSetupException( "Environment has no nodes with Sqoop installed" );
        }

        to.addLog( "Saving to db..." );
        boolean saved = manager.getPluginDao().saveInfo( SqoopConfig.PRODUCT_KEY, config.getClusterName(), config );
        if ( saved )
        {
            to.addLog( "Cluster info successfully saved" );
            configure();
        }
        else
        {
            throw new ClusterSetupException( "Failed to save installation info" );
        }

        return config;
    }
}

