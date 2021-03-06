package io.subutai.plugin.solr.impl.handler;


import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.subutai.common.environment.Environment;
import io.subutai.common.environment.Topology;
import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.core.plugincommon.api.ClusterOperationType;
import io.subutai.core.plugincommon.api.ClusterSetupException;
import io.subutai.core.plugincommon.api.ClusterSetupStrategy;
import io.subutai.core.plugincommon.api.PluginDAO;
import io.subutai.plugin.solr.api.SolrClusterConfig;
import io.subutai.plugin.solr.impl.SolrImpl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith( MockitoJUnitRunner.class )
public class ClusterOperationHandlerTest
{
    private ClusterOperationHandler clusterOperationHandler;
    private ClusterOperationHandler clusterOperationHandler2;
    private UUID uuid;
    @Mock
    SolrImpl solrImpl;
    @Mock
    SolrClusterConfig solrClusterConfig;
    @Mock
    Tracker tracker;
    @Mock
    EnvironmentManager environmentManager;
    @Mock
    TrackerOperation trackerOperation;
    @Mock
    Environment environment;
    @Mock
    ClusterSetupStrategy clusterSetupStrategy;
    @Mock
    PluginDAO pluginDAO;
    @Mock
    Topology topology;


    @Before
    public void setUp() throws Exception
    {
        // mock constructor
        uuid = UUID.randomUUID();
        when( solrImpl.getTracker() ).thenReturn( tracker );
        when( tracker.createTrackerOperation( anyString(), anyString() ) ).thenReturn( trackerOperation );
        when( trackerOperation.getId() ).thenReturn( uuid );

        clusterOperationHandler =
                new ClusterOperationHandler( solrImpl, solrClusterConfig, ClusterOperationType.INSTALL_OVER_ENV );
        clusterOperationHandler2 =
                new ClusterOperationHandler( solrImpl, solrClusterConfig, ClusterOperationType.UNINSTALL );

        // mock setupCluster
        when( solrImpl.getEnvironmentManager() ).thenReturn( environmentManager );
        when( environmentManager.loadEnvironment( any( String.class ) ) ).thenReturn( environment );
        when( solrImpl.getClusterSetupStrategy( environment, solrClusterConfig, trackerOperation ) )
                .thenReturn( clusterSetupStrategy );
    }





    @Test
    public void testRunOperationTypeInstallSetupFailed() throws Exception
    {
        when( clusterSetupStrategy.setup() ).thenThrow( ClusterSetupException.class );

        clusterOperationHandler.run();
    }


    @Test
    public void testRunOpertaionTypeUninstall()
    {
        when( solrImpl.getCluster( anyString() ) ).thenReturn( solrClusterConfig );
        when( solrImpl.getPluginDAO() ).thenReturn( pluginDAO );

        clusterOperationHandler2.run();

        // assertions
        verify( solrImpl ).getPluginDAO();
        verify( trackerOperation ).addLogDone( "Cluster removed from database" );
        assertNotNull( solrImpl.getCluster( anyString() ) );
    }


    @Test
    public void testRunOpertaionTypeUninstallSolrClusterConfigNull()
    {
        when( solrImpl.getCluster( anyString() ) ).thenReturn( null );

        clusterOperationHandler2.run();

        // assertions
        verify( trackerOperation )
                .addLogFailed( String.format( "Cluster with name %s does not exist. Operation aborted", null ) );
    }
}