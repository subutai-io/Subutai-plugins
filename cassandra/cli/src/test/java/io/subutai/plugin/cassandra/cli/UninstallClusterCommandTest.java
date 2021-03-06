package io.subutai.plugin.cassandra.cli;


import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.cassandra.api.Cassandra;
import io.subutai.plugin.cassandra.cli.UninstallClusterCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UninstallClusterCommandTest
{
    private UninstallClusterCommand uninstallClusterCommand;
    @Mock
    Cassandra cassandra;
    @Mock
    Tracker tracker;

    @Before
    public void setUp() 
    {
        uninstallClusterCommand = new UninstallClusterCommand();
        uninstallClusterCommand.setCassandraManager(cassandra);
        uninstallClusterCommand.setTracker(tracker);
    }

    @Test
    public void testGetTracker() 
    {
        uninstallClusterCommand.setTracker(tracker);
        uninstallClusterCommand.getTracker();

        // assertions
        assertNotNull(uninstallClusterCommand.getTracker());
        assertEquals(tracker, uninstallClusterCommand.getTracker());

    }

    @Test
    public void testGetCassandraManager() 
    {
        uninstallClusterCommand.setCassandraManager(cassandra);
        uninstallClusterCommand.getCassandraManager();

        // assertions
        assertNotNull(uninstallClusterCommand.getCassandraManager());
        assertEquals(cassandra, uninstallClusterCommand.getCassandraManager());

    }

    @Test
    public void test()
    {
        when(cassandra.uninstallCluster(null)).thenReturn(UUID.randomUUID());
        uninstallClusterCommand.doExecute();
    }
}