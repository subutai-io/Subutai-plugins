package org.safehaus.subutai.plugin.mongodb.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;

import org.safehaus.subutai.common.environment.Environment;
import org.safehaus.subutai.common.environment.EnvironmentNotFoundException;
import org.safehaus.subutai.common.tracker.OperationState;
import org.safehaus.subutai.common.tracker.TrackerOperationView;
import org.safehaus.subutai.common.util.CollectionUtil;
import org.safehaus.subutai.common.util.JsonUtil;
import org.safehaus.subutai.core.env.api.EnvironmentManager;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.mongodb.api.Mongo;
import org.safehaus.subutai.plugin.mongodb.api.MongoClusterConfig;
import org.safehaus.subutai.plugin.mongodb.api.NodeType;

import com.google.common.base.Preconditions;

import sun.security.provider.certpath.OCSPResponse;


/**
 * REST implementation of MongoDB API
 */

public class RestServiceImpl implements RestService
{

    private Mongo mongo;
    private Tracker tracker;
    private EnvironmentManager environmentManager;

    public RestServiceImpl( final Mongo mongo )
    {
        this.mongo = mongo;
    }



    @Override
    public Response listClusters()
    {
        List<MongoClusterConfig> configs = mongo.getClusters();
        List<String> clusterNames = new ArrayList<>();
        for ( MongoClusterConfig config : configs )
        {
            clusterNames.add( config.getClusterName() );
        }
        String clusters = JsonUtil.toJson( clusterNames );
        return Response.status( Response.Status.OK ).entity( clusters ).build();
    }


    @Override
    public Response getCluster( final String clusterName )
    {
        MongoClusterConfig config = mongo.getCluster( clusterName );
        if( config == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found " ).build();
        }
        String cluster = JsonUtil.toJson( mongo.getCluster( clusterName ) );
        return Response.status( Response.Status.OK ).entity( cluster ).build();
    }


    @Override
    public Response configureCluster( final String config )
    {
        TrimmedMongodbConfig trimmedConfig = JsonUtil.fromJson( config, TrimmedMongodbConfig.class );
        MongoClusterConfig mongoConfig = mongo.newMongoClusterConfigInstance();
        mongoConfig.setDomainName( trimmedConfig.getDomainName() );
        mongoConfig.setReplicaSetName( trimmedConfig.getReplicaSetName() );
        mongoConfig.setRouterPort( trimmedConfig.getRouterPort() );
        mongoConfig.setDataNodePort( trimmedConfig.getDataNodePort() );
        mongoConfig.setCfgSrvPort( trimmedConfig.getCfgSrvPort() );
        mongoConfig.setEnvironmentId( UUID.fromString( trimmedConfig.getEnvironmentId() ) );
        mongoConfig.setClusterName( trimmedConfig.getClusterName() );

        if ( !CollectionUtil.isCollectionEmpty( trimmedConfig.getConfigNodes() ) )
        {
            Set<UUID> nodes = new HashSet<>();
            for ( String hostname : trimmedConfig.getConfigNodes() )
            {
                nodes.add( UUID.fromString( hostname ) );
            }
            mongoConfig.getConfigHostIds().addAll( nodes );
        }

        if ( !CollectionUtil.isCollectionEmpty( trimmedConfig.getDataNodes() ) )
        {
            Set<UUID> nodes = new HashSet<>();
            for ( String hostname : trimmedConfig.getDataNodes() )
            {
                nodes.add( UUID.fromString( hostname ) );
            }
            mongoConfig.getDataHostIds().addAll( nodes );
        }

        if ( !CollectionUtil.isCollectionEmpty( trimmedConfig.getRouterNodes() ) )
        {
            Set<UUID> nodes = new HashSet<>();
            for ( String hostname : trimmedConfig.getRouterNodes() )
            {
                nodes.add( UUID.fromString( hostname ) );
            }
            mongoConfig.getRouterHostIds().addAll( nodes );
        }
        UUID uuid = mongo.configureEnvironmentCluster( mongoConfig );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );

    }


    //    @Override
//    public Response configureCluster( @FormParam( "environmentId" ) final String environmentId,
//                                      @FormParam( "clusterName" ) final String clusterName,
//                                      @FormParam( "domainName" ) final String domainName,
//                                      @FormParam( "replicaSetName" ) final String replicaSetName,
//                                      @FormParam( "configServersPort" ) final int configServersPort,
//                                      @FormParam( "dataNodePort" ) final int dataNodePort,
//                                      @FormParam( "routerPort" ) final int routerPort,
//                                      @FormParam( "dataNodes" ) final String dataNodes,
//                                      @FormParam( "configNodes" ) final String configNodes,
//                                      @FormParam( "routerNodes" ) final String routerNodes )
//    {
//        Preconditions.checkNotNull( environmentId );
//        Preconditions.checkNotNull( clusterName );
//        Preconditions.checkNotNull( domainName );
//        Preconditions.checkNotNull( replicaSetName );
//        Preconditions.checkNotNull( configServersPort );
//        Preconditions.checkNotNull( dataNodePort );
//        Preconditions.checkNotNull( configNodes );
//        Preconditions.checkNotNull( routerNodes );
//        Preconditions.checkNotNull( dataNodes );
//        Environment environment = null;
//        try
//        {
//            environment = environmentManager.findEnvironment( UUID.fromString( environmentId ) );
//        }
//        catch ( EnvironmentNotFoundException e )
//        {
//            e.printStackTrace();
//        }
//        if ( environment == null ){
//            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
//                    entity( "Could not find environment with id : " + environmentId ).build();
//        }
//
//        if ( mongo.getCluster( clusterName ) != null ){
//            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
//                    entity( "There is already a cluster with same name !" ).build();
//        }
//        MongoClusterConfig config = mongo.newMongoClusterConfigInstance();
//        config.setEnvironmentId( UUID.fromString( environmentId ) );
//        config.setClusterName( clusterName );
//        config.setCfgSrvPort( configServersPort );
//        config.setDataNodePort( dataNodePort );
//        config.setRouterPort( routerPort );
//        config.setReplicaSetName( replicaSetName );
//        config.setDomainName( domainName );
//
////        MongoConfigNode configNode = null;
////        MongoDataNodeImpl dataNode = null;
////        MongoRouterNodeImpl routerNode = null;
//
//
//            String[] nodeList = configNodes.replaceAll("\\s+", "" ).split( "," );
//            for( String node : nodeList )
//            {
//
////                configNode = new MongoConfigNodeImpl( environment.getContainerHostByHostname( node ), domainName,
////                        Integer.parseInt( configServersPort ) );
//                config.getConfigHostIds().add( UUID.fromString( node ) );
//                //config.getConfigServers().add( configNode );
//
//            }
//            nodeList = dataNodes.replaceAll( "\\s+", "" ).split( "," );
//            for( String node : nodeList )
//            {
//
////                dataNode = new MongoDataNodeImpl( environment.getContainerHostByHostname( node ), domainName,
////                        Integer.parseInt( dataNodePort ) );
//                config.getDataHostIds().add( UUID.fromString( node ) );
//                //config.getDataNodes().add( dataNode );
//
//            }
//            nodeList = routerNodes.replaceAll( "\\s+", "" ).split( "," );
//            for( String node : nodeList )
//            {
//
////                routerNode = new MongoRouterNodeImpl( environment.getContainerHostByHostname( node ), domainName,
////                        Integer.parseInt( routerPort ), Integer.parseInt( configServersPort ) );
//                config.getRouterHostIds().add( UUID.fromString( node ));
//                //config.getRouterServers().add( routerNode );
//
//            }
//
//
//
//
////        catch ( ContainerHostNotFoundException e )
////        {
////            e.printStackTrace();
////        }
//
//        UUID uuid = mongo.configureEnvironmentCluster( config );
//        OperationState state = waitUntilOperationFinish( uuid );
//        return createResponse( uuid, state );
//
//    }



    @Override
    public Response destroyCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if( mongo.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = mongo.uninstallCluster( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startNode( final String clusterName, final String lxcHostname )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( mongo.getCluster( clusterName ) == null ){
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = mongo.startNode( clusterName, lxcHostname );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response stopNode( final String clusterName, final String lxcHostname )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( mongo.getCluster( clusterName ) == null ){
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = mongo.stopNode( clusterName, lxcHostname );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( mongo.getCluster( clusterName ) == null ){
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = mongo.startAllNodes( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response stopCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( mongo.getCluster( clusterName ) == null ){
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = mongo.stopAllNodes( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response destroyNode( final String clusterName, final String lxcHostname )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( mongo.getCluster( clusterName ) == null ){
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = mongo.destroyNode( clusterName, lxcHostname );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response checkNode( final String clusterName, final String lxcHostname )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( mongo.getCluster( clusterName ) == null ){
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = mongo.checkNode( clusterName, lxcHostname );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response addNode( final String clusterName, final String nodeType )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( nodeType );
        if ( mongo.getCluster( clusterName ) == null ){
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        NodeType type = null;
        if( nodeType.contains( "config" ))
        {
            type = NodeType.CONFIG_NODE;
        }
        else if( nodeType.contains( "data" ))
        {
            type = NodeType.DATA_NODE;
        }
        else if( nodeType.contains( "router" ))
        {
            type = NodeType.ROUTER_NODE;
        }
        UUID uuid = mongo.addNode( clusterName, type );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }

    private Response createResponse( UUID uuid, OperationState state ){
        TrackerOperationView po = tracker.getTrackerOperation( MongoClusterConfig.PRODUCT_KEY, uuid );
        if ( state == OperationState.FAILED ){
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( po.getLog() ).build();
        }
        else if ( state == OperationState.SUCCEEDED ){
            return Response.status( Response.Status.OK ).entity( po.getLog() ).build();
        }
        else {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( "Timeout" ).build();
        }
    }

    private OperationState waitUntilOperationFinish( UUID uuid ){
        OperationState state = null;
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( MongoClusterConfig.PRODUCT_KEY, uuid );
            if ( po != null )
            {
                if ( po.getState() != OperationState.RUNNING )
                {
                    state = po.getState();
                    break;
                }
            }
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException ex )
            {
                break;
            }
            if ( System.currentTimeMillis() - start > ( 200 * 1000 ) )
            {
                break;
            }
        }
        return state;
    }

    public Tracker getTracker(){
        return tracker;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    public EnvironmentManager getEnvironmentManager()
    {
        return environmentManager;
    }


    public void setEnvironmentManager( final EnvironmentManager environmentManager )
    {
        this.environmentManager = environmentManager;
    }
}
