package org.safehaus.subutai.plugin.oozie.impl;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.safehaus.subutai.common.command.CommandResult;
import org.safehaus.subutai.common.command.RequestBuilder;
import org.safehaus.subutai.common.environment.Environment;
import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.env.api.EnvironmentManager;
import org.safehaus.subutai.core.lxc.quota.api.QuotaManager;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.common.PluginDAO;
import org.safehaus.subutai.plugin.common.api.ClusterException;
import org.safehaus.subutai.plugin.common.api.ClusterSetupStrategy;
import org.safehaus.subutai.plugin.hadoop.api.Hadoop;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.oozie.api.OozieClusterConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;


@RunWith( MockitoJUnitRunner.class )
public class OozieImplTest
{
    private OozieImpl oozieImpl;
    private UUID uuid;
    @Mock
    OozieClusterConfig oozieClusterConfig;
    @Mock
    Commands commands;
    @Mock
    Tracker tracker;
    @Mock
    EnvironmentManager environmentManager;
    @Mock
    TrackerOperation trackerOperation;
    @Mock
    Environment environment;
    @Mock
    ContainerHost containerHost;
    @Mock
    CommandResult commandResult;
    @Mock
    ClusterSetupStrategy clusterSetupStrategy;
    @Mock
    PluginDAO pluginDAO;
    @Mock
    Hadoop hadoop;
    @Mock
    HadoopClusterConfig hadoopClusterConfig;
    @Mock
    RequestBuilder requestBuilder;
    @Mock
    org.safehaus.subutai.core.metric.api.Monitor monitor;
    @Mock
    ExecutorService executorService;
    @Mock
    DataSource dataSource;
    @Mock
    Connection connection;
    @Mock
    PreparedStatement preparedStatement;
    @Mock
    ResultSet resultSet;
    @Mock
    ResultSetMetaData resultSetMetaData;
    @Mock
    QuotaManager quotaManager;


    @Before
    public void setUp() throws Exception
    {
        uuid = new UUID( 50, 50 );
        // mock InstallClusterHandler
        when( tracker.createTrackerOperation( anyString(), anyString() ) ).thenReturn( trackerOperation );
        when( trackerOperation.getId() ).thenReturn( uuid );
        when( pluginDAO.getInfo( OozieClusterConfig.PRODUCT_KEY, "test", OozieClusterConfig.class ) )
                .thenReturn( oozieClusterConfig );

        // mock init
        when( dataSource.getConnection() ).thenReturn( connection );
        when( connection.prepareStatement( any( String.class ) ) ).thenReturn( preparedStatement );
        when( preparedStatement.executeQuery() ).thenReturn( resultSet );
        when( resultSet.getMetaData() ).thenReturn( resultSetMetaData );
        when( resultSetMetaData.getColumnCount() ).thenReturn( 1 );

        oozieImpl = new OozieImpl( tracker, environmentManager, hadoop, monitor );
        oozieImpl.setTracker( tracker );
        oozieImpl.setExecutor( executorService );
        oozieImpl.setPluginDao( pluginDAO );
        oozieImpl.setEnvironmentManager( environmentManager );
        oozieImpl.setHadoopManager( hadoop );
        oozieImpl.setQuotaManager( quotaManager );
    }


    @Test
    public void testGetTracker() throws Exception
    {
        oozieImpl.getTracker();

        // assertions
        assertEquals( tracker, oozieImpl.getTracker() );
        assertNotNull( oozieImpl.getTracker() );
    }


    @Test
    public void testGetPluginDao() throws Exception
    {
        oozieImpl.getPluginDao();

        // assertions
        assertEquals( pluginDAO, oozieImpl.getPluginDao() );
        assertNotNull( oozieImpl.getPluginDao() );
    }


    @Test
    public void testGetEnvironmentManager() throws Exception
    {
        oozieImpl.getEnvironmentManager();

        // assertions
        assertEquals( environmentManager, oozieImpl.getEnvironmentManager() );
        assertNotNull( oozieImpl.getEnvironmentManager() );
    }


    @Test
    public void testGetHadoopManager() throws Exception
    {
        oozieImpl.getHadoopManager();

        // assertions
        assertEquals( hadoop, oozieImpl.getHadoopManager() );
        assertNotNull( oozieImpl.getHadoopManager() );
    }


    @Test
    public void testGetExecutor() throws Exception
    {
        oozieImpl.getExecutor();

        // assertions
        assertEquals( executorService, oozieImpl.getExecutor() );
        assertNotNull( oozieImpl.getExecutor() );
    }


    @Test
    public void testGetMonitor() throws Exception
    {
        oozieImpl.getMonitor();
    }


    @Test
    public void testGetAlertSettings() throws Exception
    {
        oozieImpl.getAlertSettings();
    }


    @Test
    public void testDestroy() throws Exception
    {
        oozieImpl.destroy();
    }


    @Test
    public void testInstallCluster() throws Exception
    {
        when( oozieClusterConfig.getClusterName() ).thenReturn( "test" );
        UUID id = oozieImpl.installCluster( oozieClusterConfig );

        // assertions
        assertNotNull( oozieImpl.installCluster( oozieClusterConfig ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testUninstallCluster() throws Exception
    {
        UUID id = oozieImpl.uninstallCluster( "test" );

        // assertions
        assertNotNull( oozieImpl.uninstallCluster( "test" ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testGetClusters() throws Exception
    {
        List<OozieClusterConfig> myList = new ArrayList<>();
        myList.add( oozieClusterConfig );
        when( pluginDAO.getInfo( OozieClusterConfig.PRODUCT_KEY, OozieClusterConfig.class ) ).thenReturn( myList );


        oozieImpl.getClusters();

        // assertions
        assertNotNull( oozieImpl.getClusters() );
        assertEquals( myList, oozieImpl.getClusters() );
    }


    @Test
    public void testGetCluster() throws Exception
    {
        oozieImpl.getCluster( "test" );

        // assertions
        assertNotNull( oozieImpl.getCluster( "test" ) );
        assertEquals( oozieClusterConfig, oozieImpl.getCluster( "test" ) );
    }


    @Test
    public void testAddNode() throws Exception
    {
        UUID id = oozieImpl.addNode( "test", "test" );

        // assertions
        assertNotNull( oozieImpl.addNode( "test", "test" ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testStartNode() throws Exception
    {
        UUID id = oozieImpl.startNode( "test", "test" );

        // assertions
        assertNotNull( oozieImpl.startNode( "test", "test" ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testStopNode() throws Exception
    {
        UUID id = oozieImpl.stopNode( "test", "test" );

        // assertions
        assertNotNull( oozieImpl.stopNode( "test", "test" ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testCheckNode() throws Exception
    {
        UUID id = oozieImpl.checkNode( "test", "test" );

        // assertions
        assertNotNull( oozieImpl.checkNode( "test", "test" ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testGetClusterSetupStrategy() throws Exception
    {
        oozieImpl.getClusterSetupStrategy( oozieClusterConfig, trackerOperation );

        // assertions
        assertNotNull( oozieImpl.getClusterSetupStrategy( oozieClusterConfig, trackerOperation ) );
    }


    @Test
    public void testDestroyNode() throws Exception
    {
        UUID id = oozieImpl.destroyNode( "test", "test" );

        // assertions
        assertNotNull( oozieImpl.destroyNode( "test", "test" ) );
        assertEquals( uuid, id );
    }


    @Test
    public void testGetQuotaManager() throws Exception
    {
        oozieImpl.getQuotaManager();
    }


    @Test
    public void testSubscribeToAlerts() throws Exception
    {
        oozieImpl.subscribeToAlerts( containerHost );
    }


    @Test
    public void testSubscribeToAlerts1() throws Exception
    {
        oozieImpl.subscribeToAlerts( environment );
    }


    @Test
    public void testUnsubscribeFromAlerts() throws Exception
    {
        oozieImpl.unsubscribeFromAlerts( environment );
    }


    @Test
    public void testOnEnvironmentCreated()
    {
        oozieImpl.onEnvironmentCreated( environment );
    }


    @Test
    public void testOnEnvironmentGrown()
    {
        Set<ContainerHost> mySet = new HashSet<>();
        mySet.add( containerHost );

        oozieImpl.onEnvironmentGrown( environment, mySet );
    }


    @Test
    public void testSaveConfig() throws ClusterException
    {
        when( pluginDAO.saveInfo( anyString(), anyString(), any() ) ).thenReturn( true );

        oozieImpl.saveConfig( oozieClusterConfig );
    }


    @Test( expected = ClusterException.class )
    public void testSaveConfigCouldNotSave() throws ClusterException
    {
        when( pluginDAO.saveInfo( anyString(), anyString(), any() ) ).thenReturn( false );

        oozieImpl.saveConfig( oozieClusterConfig );
    }


    @Test
    public void testDeleteConfig() throws ClusterException
    {
        when( pluginDAO.deleteInfo( anyString(), anyString() ) ).thenReturn( true );

        oozieImpl.deleteConfig( oozieClusterConfig );
    }


    @Test( expected = ClusterException.class )
    public void testDeleteConfigCouldNotDelete() throws ClusterException
    {
        when( pluginDAO.deleteInfo( anyString(), anyString() ) ).thenReturn( false );

        oozieImpl.deleteConfig( oozieClusterConfig );
    }


    @Test
    public void testOnContainerDestroyed()
    {
        List<OozieClusterConfig> myList = new ArrayList<>();
        myList.add( oozieClusterConfig );
        when( pluginDAO.getInfo( OozieClusterConfig.PRODUCT_KEY, OozieClusterConfig.class ) ).thenReturn( myList );
        oozieImpl.getClusters();
        when( environment.getId() ).thenReturn( uuid );
        when( oozieClusterConfig.getEnvironmentId() ).thenReturn( uuid );
        Set<UUID> myUUID = new HashSet<>();
        myUUID.add( uuid );
        when( oozieClusterConfig.getAllNodes() ).thenReturn( myUUID );
        when( oozieClusterConfig.getClients() ).thenReturn( myUUID );
        when( pluginDAO.saveInfo( anyString(), anyString(), any() ) ).thenReturn( true );

        oozieImpl.onContainerDestroyed( environment, uuid );
    }


    @Test
    public void testOnContainerDestroyedNotSaved()
    {
        List<OozieClusterConfig> myList = new ArrayList<>();
        myList.add( oozieClusterConfig );
        when( pluginDAO.getInfo( OozieClusterConfig.PRODUCT_KEY, OozieClusterConfig.class ) ).thenReturn( myList );
        oozieImpl.getClusters();
        when( environment.getId() ).thenReturn( uuid );
        when( oozieClusterConfig.getEnvironmentId() ).thenReturn( uuid );
        Set<UUID> myUUID = new HashSet<>();
        myUUID.add( uuid );
        when( oozieClusterConfig.getAllNodes() ).thenReturn( myUUID );
        when( oozieClusterConfig.getClients() ).thenReturn( myUUID );
        when( pluginDAO.saveInfo( anyString(), anyString(), any() ) ).thenReturn( false );

        oozieImpl.onContainerDestroyed( environment, uuid );
    }


    @Test
    public void testOnEnvironmentDestroyedNotDelete()
    {
        List<OozieClusterConfig> myList = new ArrayList<>();
        myList.add( oozieClusterConfig );
        when( pluginDAO.getInfo( OozieClusterConfig.PRODUCT_KEY, OozieClusterConfig.class ) ).thenReturn( myList );
        oozieImpl.getClusters();
        when( environment.getId() ).thenReturn( uuid );
        when( oozieClusterConfig.getEnvironmentId() ).thenReturn( uuid );

        oozieImpl.onEnvironmentDestroyed( uuid );
    }

    @Test
    public void testOnEnvironmentDestroyed()
    {
        List<OozieClusterConfig> myList = new ArrayList<>();
        myList.add( oozieClusterConfig );
        when( pluginDAO.getInfo( OozieClusterConfig.PRODUCT_KEY, OozieClusterConfig.class ) ).thenReturn( myList );
        oozieImpl.getClusters();
        when( environment.getId() ).thenReturn( uuid );
        when( oozieClusterConfig.getEnvironmentId() ).thenReturn( uuid );
        when( pluginDAO.deleteInfo( anyString(), anyString()) ).thenReturn( true );

        oozieImpl.onEnvironmentDestroyed( uuid );
    }

}