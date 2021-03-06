package io.subutai.plugin.hipi.cli;


import java.util.UUID;

import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.hipi.api.Hipi;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;


@Command( scope = "hipi", name = "uninstall-cluster", description = "Command to uninstall Lucene cluster" )
public class UninstallClusterCommand extends OsgiCommandSupport
{

    @Argument( index = 0, name = "clusterName", description = "The name of the cluster.", required = true,
            multiValued = false ) String clusterName = null;
    private Hipi hipiManager;
    private Tracker tracker;


    protected Object doExecute()
    {
        UUID uuid = hipiManager.uninstallCluster( clusterName );
        System.out.println(
                "Uninstall operation is " + InstallClusterCommand.waitUntilOperationFinish( tracker, uuid ) + "." );
        return null;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public void setTracker( Tracker tracker )
    {
        this.tracker = tracker;
    }


    public Hipi getHipiManager()
    {
        return hipiManager;
    }


    public void setHipiManager( Hipi hipiManager )
    {
        this.hipiManager = hipiManager;
    }
}
