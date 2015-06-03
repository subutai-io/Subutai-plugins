package org.safehaus.subutai.plugin.mysql.impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.safehaus.subutai.common.command.CommandException;
import org.safehaus.subutai.common.command.CommandResult;
import org.safehaus.subutai.common.command.RequestBuilder;
import org.safehaus.subutai.common.environment.ContainerHostNotFoundException;
import org.safehaus.subutai.common.environment.Environment;
import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.plugin.common.api.ClusterConfigurationException;
import org.safehaus.subutai.plugin.common.api.ClusterException;
import org.safehaus.subutai.plugin.mysql.impl.common.Commands;
import org.safehaus.subutai.plugin.mysqlc.api.MySQLClusterConfig;
import org.safehaus.subutai.plugin.mysqlc.api.MySQLDataNodeConfig;
import org.safehaus.subutai.plugin.mysqlc.api.MySQLManagerNodeConfig;


/**
 * Created by tkila on 5/13/15.
 */
public class ClusterConfig
{
    private TrackerOperation po;
    private MySQLCImpl manager;


    public ClusterConfig( final TrackerOperation trackerOperation, final MySQLCImpl manager )
    {
        this.po = trackerOperation;
        this.manager = manager;
    }


    public void configureCluster( MySQLClusterConfig config, Environment environment )
            throws ClusterConfigurationException
    {
        po.addLog( String.format( "Configuring cluster :%s", config.getClusterName() ) );

        MySQLManagerNodeConfig mySQLManagerNodeConfig = new MySQLManagerNodeConfig();

        List<String> dataNodeIPAddressList = getIps( config.getDataNodes(), environment );
        List<String> managerNodeIpAddressList = getIps( config.getManagerNodes(), environment );

        mySQLManagerNodeConfig.setDataNodesIPAddresses( dataNodeIPAddressList );
        mySQLManagerNodeConfig.setManagerNodeHost( managerNodeIpAddressList );
        mySQLManagerNodeConfig.setDataDir( config.getDataManNodeDir() );
        mySQLManagerNodeConfig.setDataNodeDataDir( config.getDataNodeDataDir() );
        mySQLManagerNodeConfig.setDataNodesIPAddresses( dataNodeIPAddressList );

        MySQLDataNodeConfig mySQLDataNodeConfig =
                new MySQLDataNodeConfig( getIpsSeperatedByComma( config.getManagerNodes(), environment ) );
        //configure manager node(s)
        for ( UUID uuid : config.getManagerNodes() )
        {
            try
            {
                ContainerHost containerHost = environment.getContainerHostById( uuid );
                po.addLog( "Configuring manager node: " + containerHost.getHostname() );

                //create data/conf dir
                CommandResult commandResult =
                        containerHost.execute( new RequestBuilder( Commands.createDir + config.getDataManNodeDir() ) );
                po.addLog( commandResult.getStdOut() );

                //create config.ini file
                commandResult = containerHost.execute( new RequestBuilder(
                        String.format( Commands.recreateConfFile, config.getConfManNodeFile(),
                                config.getConfManNodeFile() ) ) );
                po.addLog( commandResult.getStdOut() );

                //write manager node configuration to config file
                commandResult = containerHost.execute( new RequestBuilder(
                        Commands.writeToConfFile + mySQLManagerNodeConfig.exportConfing() + " " + config
                                .getConfManNodeFile() ) );
                po.addLog( commandResult.getStdOut() );

                config.getRequiresReloadConf().put( containerHost.getHostname(), true );
            }
            catch ( ContainerHostNotFoundException | CommandException e )
            {
                e.printStackTrace();
            }
        }
        //configure data node(s)
        for ( UUID uuid : config.getDataNodes() )
        {
            ContainerHost containerHost = null;
            try
            {
                containerHost = environment.getContainerHostById( uuid );
                po.addLog( "Configuring data node:" + containerHost.getHostname() );
                CommandResult commandResult;
                commandResult =
                        containerHost.execute( new RequestBuilder( Commands.createDir + config.getDataNodeDataDir() ) );
                po.addLog( commandResult.getStdOut() );
                //clear config my.cnf
                commandResult = containerHost.execute( new RequestBuilder(
                        String.format( Commands.recreateConfFile, config.getConfNodeDataFile(),
                                config.getConfNodeDataFile() ) ) );
                po.addLog( commandResult.getStdOut() );

                //add config to data nodes
                commandResult = containerHost.execute( new RequestBuilder(
                        Commands.writeToConfFile + mySQLDataNodeConfig.exportConfig() + " " + config
                                .getConfNodeDataFile() ) );

                po.addLog( commandResult.getStdOut() );
                //chmod 644 my.cnf
                commandResult =
                        containerHost.execute( new RequestBuilder( Commands.chmod644 + config.getConfNodeDataFile() ) );
                po.addLog( commandResult.getStdOut() );
                containerHost.execute(
                        new RequestBuilder( String.format( "cp %s /etc/my.cnf", config.getConfNodeDataFile() ) ) );

                config.getRequiresReloadConf().put( containerHost.getHostname(), true );
                if ( !config.getIsInitialStart().containsKey( containerHost.getHostname() ) )
                {
                    //first time ndbd launch requires --initial flag
                    config.getIsInitialStart().put( containerHost.getHostname(), true );
                }
                if ( !config.getIsSqlInstalled().containsKey( containerHost.getHostname() ) )
                {
                    //keep info if sql was installed on data node
                    config.getIsSqlInstalled().put( containerHost.getHostname(), false );
                }
            }
            catch ( ContainerHostNotFoundException | CommandException e )
            {
                e.printStackTrace();
            }
        }
        try
        {
            manager.saveConfig( config );
        }

        catch ( ClusterException e )
        {
            e.printStackTrace();
        }
        po.addLog( "MySQL Cluster data saved into database" );
    }


    private String getIpsSeperatedByComma( Collection<UUID> collection, Environment environment )
    {
        StringBuilder sb = new StringBuilder();
        for ( UUID uuid : collection )
        {
            ContainerHost containerHost;

            try
            {
                containerHost = environment.getContainerHostById( uuid );
                sb.append( containerHost.getIpByInterfaceName( "eth0" ) ).append( "," );
            }
            catch ( ContainerHostNotFoundException e )
            {
                e.printStackTrace();
            }
        }
        if ( !sb.toString().isEmpty() )
        {
            sb.replace( sb.toString().length() - 1, sb.toString().length(), "" );
        }
        return sb.toString();
    }


    private List<String> getIps( Collection<UUID> collection, Environment environment )
    {

        List<String> dataNodeIPAddressList = new ArrayList<>();

        for ( UUID uuid : collection )
        {
            ContainerHost containerHost;

            try
            {
                containerHost = environment.getContainerHostById( uuid );
                dataNodeIPAddressList.add( containerHost.getIpByInterfaceName( "eth0" ) );
            }
            catch ( ContainerHostNotFoundException e )
            {
                e.printStackTrace();
            }
        }
        return dataNodeIPAddressList;
    }
}
