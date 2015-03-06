package org.safehaus.subutai.plugin.accumulo.impl.handler;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.safehaus.subutai.common.command.CommandException;
import org.safehaus.subutai.common.command.CommandResult;
import org.safehaus.subutai.common.command.RequestBuilder;
import org.safehaus.subutai.common.environment.Environment;
import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.env.api.EnvironmentManager;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.accumulo.api.AccumuloClusterConfig;
import org.safehaus.subutai.plugin.accumulo.impl.AccumuloImpl;
import org.safehaus.subutai.plugin.accumulo.impl.Commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith( MockitoJUnitRunner.class )
public class AddPropertyOperationHandlerTest
{
    @Mock AccumuloImpl accumuloImpl;
    @Mock AccumuloClusterConfig accumuloClusterConfig;
    @Mock Tracker tracker;
    @Mock EnvironmentManager environmentManager;
    @Mock TrackerOperation trackerOperation;
    @Mock Environment environment;
    @Mock ContainerHost containerHost;
    @Mock CommandResult commandResult;
    private AddPropertyOperationHandler addPropertyOperationHandler;
    private UUID uuid;


    @Before
    public void setUp() throws Exception
    {
        uuid = UUID.randomUUID();
        when( accumuloImpl.getCluster( anyString() ) ).thenReturn( accumuloClusterConfig );
        when( accumuloImpl.getTracker() ).thenReturn( tracker );
        when( tracker.createTrackerOperation( anyString(), anyString() ) ).thenReturn( trackerOperation );
        when( trackerOperation.getId() ).thenReturn( uuid );

        addPropertyOperationHandler =
                new AddPropertyOperationHandler( accumuloImpl, "testCluster", "testProperty", "testPropertyValue" );

        // mock run method
        Set<ContainerHost> mySet = new HashSet<>();
        mySet.add( containerHost );
        when( accumuloImpl.getEnvironmentManager() ).thenReturn( environmentManager );
        when( environmentManager.findEnvironment( any( UUID.class ) ) ).thenReturn( environment );
        when( environment.getContainerHostsByIds( anySetOf( UUID.class ) ) ).thenReturn( mySet );
        when( containerHost.execute(
                new RequestBuilder( Commands.getAddPropertyCommand( "testProperty", "testPropertyValue" ) ) ) )
                .thenReturn( commandResult );
    }


    @Test
    public void testGetTrackerId() throws Exception
    {
        UUID id = addPropertyOperationHandler.getTrackerId();

        // assertions
        assertNotNull( uuid );
        assertEquals( uuid, id );
    }


    @Test
    public void testRunCommandResultNotSucceeded() throws Exception
    {
        addPropertyOperationHandler.run();
    }


    @Test
    public void testRun() throws Exception
    {
        when( commandResult.hasSucceeded() ).thenReturn( true );
        when( environment.getContainerHostById( any( UUID.class ) ) ).thenReturn( containerHost );
        when( containerHost.execute( Commands.stopCommand ) ).thenReturn( commandResult );
        when( containerHost.execute( Commands.startCommand ) ).thenReturn( commandResult );


        addPropertyOperationHandler.run();

        // assertions
        assertNotNull( accumuloImpl.getCluster( anyString() ) );
        verify( containerHost )
                .execute( new RequestBuilder( Commands.getAddPropertyCommand( "testProperty", "testPropertyValue" ) ) );
        assertTrue( commandResult.hasSucceeded() );
        verify( trackerOperation ).addLog( "Property added successfully to node " + containerHost.getHostname() );
        verify( containerHost ).execute( Commands.stopCommand );
        verify( containerHost ).execute( Commands.startCommand );
        verify( trackerOperation ).addLogDone( "Done" );
    }


    @Test

    public void testRunShouldThrowsCommandException() throws Exception
    {
        when( commandResult.hasSucceeded() ).thenReturn( true );
        when( environment.getContainerHostById( any( UUID.class ) ) ).thenReturn( containerHost );
        when( containerHost.execute( Commands.stopCommand ) ).thenThrow( CommandException.class );
        when( containerHost.execute( Commands.startCommand ) ).thenThrow( CommandException.class );


        addPropertyOperationHandler.run();

        // assertions
        assertNotNull( accumuloImpl.getCluster( anyString() ) );
        verify( containerHost )
                .execute( new RequestBuilder( Commands.getAddPropertyCommand( "testProperty", "testPropertyValue" ) ) );
        assertTrue( commandResult.hasSucceeded() );
        verify( trackerOperation ).addLog( "Property added successfully to node " + containerHost.getHostname() );
    }
}