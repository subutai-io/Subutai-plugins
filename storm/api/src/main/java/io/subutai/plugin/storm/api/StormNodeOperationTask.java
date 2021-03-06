package io.subutai.plugin.storm.api;


import java.util.UUID;

import io.subutai.common.peer.ContainerHost;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.core.plugincommon.api.CompleteEvent;
import io.subutai.core.plugincommon.api.NodeOperationType;
import io.subutai.core.plugincommon.impl.AbstractNodeOperationTask;


public class StormNodeOperationTask extends AbstractNodeOperationTask implements Runnable
{
    private final String clusterName;
    private final ContainerHost containerHost;
    private final Storm storm;
    private NodeOperationType nodeOperationType;


    public StormNodeOperationTask( Storm storm, Tracker tracker, String clusterName, ContainerHost containerHost,
                                   NodeOperationType operationType, CompleteEvent completeEvent, UUID trackID )
    {
        super( tracker, storm.getCluster( clusterName ), completeEvent, trackID, containerHost );
        this.storm = storm;
        this.clusterName = clusterName;
        this.containerHost = containerHost;
        this.nodeOperationType = operationType;
    }


    @Override
    public UUID runTask()
    {
        UUID trackID = null;
        switch ( nodeOperationType )
        {
            case START:
                trackID = storm.startNode( clusterName, containerHost.getHostname() );
                break;
            case STOP:
                trackID = storm.stopNode( clusterName, containerHost.getHostname() );
                break;
            case STATUS:
                trackID = storm.checkNode( clusterName, containerHost.getHostname() );
                break;
        }
        return trackID;
    }


    @Override
    public String getProductStoppedIdentifier()
    {
        return "is not running";
    }


    @Override
    public String getProductRunningIdentifier()
    {
        return "is running";
    }
}
