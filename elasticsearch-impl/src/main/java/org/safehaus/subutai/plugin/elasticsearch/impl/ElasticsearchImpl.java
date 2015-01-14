package org.safehaus.subutai.plugin.elasticsearch.impl;


import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.environment.api.EnvironmentManager;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.core.lxc.quota.api.QuotaManager;
import org.safehaus.subutai.core.metric.api.Monitor;
import org.safehaus.subutai.core.metric.api.MonitorException;
import org.safehaus.subutai.core.metric.api.MonitoringSettings;
import org.safehaus.subutai.core.peer.api.ContainerHost;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.common.api.AbstractOperationHandler;
import org.safehaus.subutai.plugin.common.api.ClusterException;
import org.safehaus.subutai.plugin.common.api.ClusterOperationType;
import org.safehaus.subutai.plugin.common.api.ClusterSetupStrategy;
import org.safehaus.subutai.plugin.common.api.NodeOperationType;
import org.safehaus.subutai.plugin.elasticsearch.api.Elasticsearch;
import org.safehaus.subutai.plugin.elasticsearch.api.ElasticsearchClusterConfiguration;
import org.safehaus.subutai.plugin.elasticsearch.impl.alert.EsAlertListener;
import org.safehaus.subutai.plugin.elasticsearch.impl.dao.PluginDAO;
import org.safehaus.subutai.plugin.elasticsearch.impl.handler.ClusterOperationHandler;
import org.safehaus.subutai.plugin.elasticsearch.impl.handler.NodeOperationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


public class ElasticsearchImpl implements Elasticsearch
{
    private static final Logger LOG = LoggerFactory.getLogger( ElasticsearchImpl.class.getName() );
    private final MonitoringSettings alertSettings = new MonitoringSettings().withIntervalBetweenAlertsInMin( 45 );

    private Tracker tracker;
    protected ExecutorService executor;
    private EnvironmentManager environmentManager;
    private PluginDAO pluginDAO;
    private DataSource dataSource;
    private Monitor monitor;
    private QuotaManager quotaManager;
    private EsAlertListener alertListener;

    Commands commands = new Commands();


    public ElasticsearchImpl( final Tracker tracker, final EnvironmentManager environmentManager,
                              final DataSource dataSource, final Monitor monitor, final QuotaManager quotaManager )
    {
        this.tracker = tracker;
        this.environmentManager = environmentManager;
        this.dataSource = dataSource;
        this.monitor = monitor;
        this.quotaManager = quotaManager;

        alertListener = new EsAlertListener( this );
        monitor.addAlertListener( alertListener );
    }


    public Commands getCommands()
    {
        return commands;
    }


    public MonitoringSettings getAlertSettings()
    {
        return alertSettings;
    }


    public Monitor getMonitor()
    {
        return monitor;
    }


    public QuotaManager getQuotaManager()
    {
        return quotaManager;
    }


    public void subscribeToAlerts( Environment environment ) throws MonitorException
    {
        getMonitor().startMonitoring( alertListener, environment, alertSettings );
    }


    public void subscribeToAlerts( ContainerHost host ) throws MonitorException
    {
        getMonitor().activateMonitoring( host, alertSettings );
    }


    public void unsubscribeFromAlerts( final Environment environment ) throws MonitorException
    {
        getMonitor().stopMonitoring( alertListener, environment );
    }


    public PluginDAO getPluginDAO()
    {
        return pluginDAO;
    }


    public EnvironmentManager getEnvironmentManager()
    {
        return environmentManager;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public void init()
    {
        try
        {
            this.pluginDAO = new PluginDAO( dataSource );
        }
        catch ( SQLException e )
        {
            LOG.error( e.getMessage(), e );
        }
        executor = Executors.newCachedThreadPool();
    }


    public void destroy()
    {
        executor.shutdown();
    }


    @Override
    public UUID installCluster( final ElasticsearchClusterConfiguration config )
    {
        Preconditions.checkNotNull( config, "Configuration is null" );

        AbstractOperationHandler operationHandler =
                new ClusterOperationHandler( this, config, ClusterOperationType.INSTALL );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public UUID uninstallCluster( String clusterName )
    {
        AbstractOperationHandler operationHandler =
                new ClusterOperationHandler( this, getCluster( clusterName ), ClusterOperationType.UNINSTALL );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public List<ElasticsearchClusterConfiguration> getClusters()
    {
        return pluginDAO
                .getInfo( ElasticsearchClusterConfiguration.PRODUCT_KEY, ElasticsearchClusterConfiguration.class );
    }


    @Override
    public ElasticsearchClusterConfiguration getCluster( String clusterName )
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( clusterName ), "Cluster name is null or empty" );
        return pluginDAO.getInfo( ElasticsearchClusterConfiguration.PRODUCT_KEY, clusterName,
                ElasticsearchClusterConfiguration.class );
    }


    @Override
    public UUID addNode( final String clusterName, final String hostname )
    {
        AbstractOperationHandler operationHandler =
                new NodeOperationHandler( this, clusterName, hostname, NodeOperationType.INSTALL );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public UUID destroyNode( final String clusterName, final String hostname )
    {
        AbstractOperationHandler operationHandler =
                new NodeOperationHandler( this, clusterName, hostname, NodeOperationType.UNINSTALL );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public UUID checkNode( final String clusterName, final String hostname )
    {
        AbstractOperationHandler operationHandler =
                new NodeOperationHandler( this, clusterName, hostname, NodeOperationType.STATUS );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public UUID startNode( final String clusterName, final String hostname )
    {
        AbstractOperationHandler operationHandler =
                new NodeOperationHandler( this, clusterName, hostname, NodeOperationType.START );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public UUID stopNode( final String clusterName, final String hostname )
    {
        AbstractOperationHandler operationHandler =
                new NodeOperationHandler( this, clusterName, hostname, NodeOperationType.STOP );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public ClusterSetupStrategy getClusterSetupStrategy(
            final ElasticsearchClusterConfiguration elasticsearchClusterConfiguration, final TrackerOperation po )
    {

        Preconditions.checkNotNull( elasticsearchClusterConfiguration, "Zookeeper cluster config is null" );
        Preconditions.checkNotNull( po, "Product operation is null" );

        return new ESSetupStrategy( elasticsearchClusterConfiguration, po, this );
    }


    @Override
    public void saveConfig( final ElasticsearchClusterConfiguration config ) throws ClusterException
    {
        Preconditions.checkNotNull( config );

        if ( !getPluginDAO()
                .saveInfo( ElasticsearchClusterConfiguration.PRODUCT_KEY, config.getClusterName(), config ) )
        {
            throw new ClusterException( "Could not save cluster info" );
        }
    }
}
