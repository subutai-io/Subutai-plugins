package io.subutai.plugin.presto.ui.wizard;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.VerticalLayout;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.util.CollectionUtil;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.presto.api.PrestoClusterConfig;


public class ConfigurationStep extends Panel
{
    private final static Logger LOGGER = LoggerFactory.getLogger( ConfigurationStep.class );

    private final Hadoop hadoop;
    Property.ValueChangeListener coordinatorComboChangeListener;
    Property.ValueChangeListener workersSelectChangeListener;
    private ComboBox hadoopClustersCombo;
    private TwinColSelect workersSelect;
    private ComboBox coordinatorNodeCombo;
    private Environment hadoopEnvironment;
    private final EnvironmentManager environmentManager;
    private Wizard wizard;


    public ConfigurationStep( final Hadoop hadoop, final Wizard wizard, final EnvironmentManager environmentManager )
    {

        this.hadoop = hadoop;
        this.environmentManager = environmentManager;
        this.wizard = wizard;
        setSizeFull();

        GridLayout content = new GridLayout( 1, 4 );
        content.setSizeFull();
        content.setSpacing( true );
        content.setMargin( true );

        TextField nameTxt = new TextField( "Cluster name" );
        nameTxt.setId( "PrestoClusterName" );
        nameTxt.setInputPrompt( "Cluster name" );
        nameTxt.setRequired( true );
        nameTxt.addValueChangeListener( new Property.ValueChangeListener()
        {

            @Override
            public void valueChange( Property.ValueChangeEvent e )
            {
                wizard.getConfig().setClusterName( e.getProperty().getValue().toString().trim() );
            }
        } );
        nameTxt.setValue( wizard.getConfig().getClusterName() );

        Button next = new Button( "Next" );
        next.setId( "PresConfNext" );
        next.addStyleName( "default" );
        next.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                nextClickHandler( wizard );
            }
        } );

        Button back = new Button( "Back" );
        back.setId( "PresConfBack" );
        back.addStyleName( "default" );
        back.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                wizard.back();
            }
        } );

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing( true );
        layout.addComponent( new Label( "Please, specify installation settings" ) );
        layout.addComponent( content );

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent( back );
        buttons.addComponent( next );

        content.addComponent( nameTxt );
        PrestoClusterConfig config = wizard.getConfig();

        addSettingsControls( content, config );

        content.addComponent( buttons );

        setContent( layout );
    }


    private void addSettingsControls( ComponentContainer parent, final PrestoClusterConfig config )
    {

        hadoopClustersCombo = new ComboBox( "Hadoop cluster" );
        hadoopClustersCombo.setId( "PresHadoopClusterCb" );
        coordinatorNodeCombo = new ComboBox( "Coordinator" );
        coordinatorNodeCombo.setId( "PresCoordinatorCb" );
        workersSelect = new TwinColSelect( "Workers", new ArrayList<EnvironmentContainerHost>() );
        workersSelect.setId( "PresSelect" );

        coordinatorNodeCombo.setImmediate( true );
        coordinatorNodeCombo.setTextInputAllowed( false );
        coordinatorNodeCombo.setRequired( true );
        coordinatorNodeCombo.setNullSelectionAllowed( false );

        hadoopClustersCombo.setImmediate( true );
        hadoopClustersCombo.setTextInputAllowed( false );
        hadoopClustersCombo.setRequired( true );
        hadoopClustersCombo.setNullSelectionAllowed( false );

        workersSelect.setItemCaptionPropertyId( "hostname" );
        workersSelect.setRows( 7 );
        workersSelect.setMultiSelect( true );
        workersSelect.setImmediate( true );
        workersSelect.setLeftColumnCaption( "Available Nodes" );
        workersSelect.setRightColumnCaption( "Selected Nodes" );
        workersSelect.setWidth( 100, Unit.PERCENTAGE );
        workersSelect.setRequired( true );

        List<HadoopClusterConfig> clusters = hadoop.getClusters();

        //populate hadoop clusters combo
        if ( !clusters.isEmpty() )
        {
            for ( HadoopClusterConfig hadoopClusterInfo : clusters )
            {
                hadoopClustersCombo.addItem( hadoopClusterInfo );
                hadoopClustersCombo.setItemCaption( hadoopClusterInfo, hadoopClusterInfo.getClusterName() );
            }
        }

        if ( Strings.isNullOrEmpty( config.getHadoopClusterName() ) )
        {
            if ( !clusters.isEmpty() )
            {
                hadoopClustersCombo.setValue( clusters.iterator().next() );
            }
        }
        else
        {
            HadoopClusterConfig info = hadoop.getCluster( config.getHadoopClusterName() );
            if ( info != null )
            //restore cluster
            {
                hadoopClustersCombo.setValue( info );
            }
            else if ( !clusters.isEmpty() )
            {
                hadoopClustersCombo.setValue( clusters.iterator().next() );
            }
        }

        //populate selection controls
        if ( hadoopClustersCombo.getValue() != null )
        {
            HadoopClusterConfig hadoopInfo = ( HadoopClusterConfig ) hadoopClustersCombo.getValue();
            config.setHadoopClusterName( hadoopInfo.getClusterName() );
            try
            {
                hadoopEnvironment = environmentManager.loadEnvironment( hadoopInfo.getEnvironmentId() );
            }
            catch ( EnvironmentNotFoundException e )
            {
                LOGGER.error( "Error getting environment by id: " + hadoopInfo.getEnvironmentId(), e );
                return;
            }
            Set<EnvironmentContainerHost> hadoopNodes = Sets.newHashSet();
            try
            {
                for ( String nodeId : filterNodes( hadoopInfo.getAllNodes() ) )
                {
                    hadoopNodes.add( hadoopEnvironment.getContainerHostById( nodeId ) );
                }
            }
            catch ( ContainerHostNotFoundException e )
            {
                LOGGER.error( "Container host not found", e );
            }
            workersSelect
                    .setContainerDataSource( new BeanItemContainer<>( EnvironmentContainerHost.class, hadoopNodes ) );

            for ( EnvironmentContainerHost hadoopNode : hadoopNodes )
            {
                coordinatorNodeCombo.addItem( hadoopNode );
                coordinatorNodeCombo.setItemCaption( hadoopNode, hadoopNode.getHostname() );
            }
        }
        //restore coordinator
        if ( config.getCoordinatorNode() != null )
        {
            coordinatorNodeCombo.setValue( config.getCoordinatorNode() );
            workersSelect.getContainerDataSource().removeItem( config.getCoordinatorNode() );
        }

        //restore workers
        if ( !CollectionUtil.isCollectionEmpty( config.getWorkers() ) )
        {
            workersSelect.setValue( config.getWorkers() );
            for ( String worker : config.getWorkers() )
            {
                coordinatorNodeCombo.removeItem( worker );
            }
        }

        //hadoop cluster selection change listener
        hadoopClustersCombo.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                if ( event.getProperty().getValue() != null )
                {
                    HadoopClusterConfig hadoopInfo = ( HadoopClusterConfig ) event.getProperty().getValue();
                    config.setHadoopClusterName( hadoopInfo.getClusterName() );
                    try
                    {
                        hadoopEnvironment = environmentManager.loadEnvironment( hadoopInfo.getEnvironmentId() );
                    }
                    catch ( EnvironmentNotFoundException e )
                    {
                        LOGGER.error( "Error getting environment by id: " + hadoopInfo.getEnvironmentId(), e );
                        return;
                    }
                    Set<EnvironmentContainerHost> hadoopNodes = Sets.newHashSet();
                    try
                    {
                        for ( String nodeId : filterNodes( hadoopInfo.getAllNodes() ) )
                        {
                            hadoopNodes.add( hadoopEnvironment.getContainerHostById( nodeId ) );
                        }
                    }
                    catch ( ContainerHostNotFoundException e )
                    {
                        LOGGER.error( "Container host not found", e );
                    }
                    workersSelect.setValue( null );
                    workersSelect.setContainerDataSource(
                            new BeanItemContainer<>( EnvironmentContainerHost.class, hadoopNodes ) );
                    coordinatorNodeCombo.setValue( null );
                    coordinatorNodeCombo.removeAllItems();
                    for ( EnvironmentContainerHost hadoopNode : hadoopNodes )
                    {
                        coordinatorNodeCombo.addItem( hadoopNode );
                        coordinatorNodeCombo.setItemCaption( hadoopNode, hadoopNode.getHostname() );
                    }
                    config.setHadoopClusterName( hadoopInfo.getClusterName() );
                    config.setWorkers( new HashSet<String>() );
                    config.setCoordinatorNode( null );
                }
            }
        } );

        //coordinator selection change listener
        coordinatorComboChangeListener = new Property.ValueChangeListener()
        {

            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                if ( event.getProperty().getValue() != null )
                {
                    EnvironmentContainerHost coordinator = ( EnvironmentContainerHost ) event.getProperty().getValue();
                    config.setCoordinatorNode( coordinator.getId() );

                    //clear workers
                    HadoopClusterConfig hadoopInfo = ( HadoopClusterConfig ) hadoopClustersCombo.getValue();
                    if ( !CollectionUtil.isCollectionEmpty( config.getWorkers() ) )
                    {
                        config.getWorkers().remove( coordinator.getId() );
                    }
                    try
                    {
                        hadoopEnvironment = environmentManager.loadEnvironment( hadoopInfo.getEnvironmentId() );
                    }
                    catch ( EnvironmentNotFoundException e )
                    {
                        LOGGER.error( "Error getting environment by id: " + hadoopInfo.getEnvironmentId(), e );
                        return;
                    }
                    Set<EnvironmentContainerHost> hadoopNodes = Sets.newHashSet();
                    try
                    {
                        for ( String nodeId : filterNodes( hadoopInfo.getAllNodes() ) )
                        {
                            hadoopNodes.add( hadoopEnvironment.getContainerHostById( nodeId ) );
                        }
                    }
                    catch ( ContainerHostNotFoundException e )
                    {
                        LOGGER.error( "Container host not found", e );
                    }
                    hadoopNodes.remove( coordinator );
                    workersSelect.getContainerDataSource().removeAllItems();
                    for ( EnvironmentContainerHost hadoopNode : hadoopNodes )
                    {
                        workersSelect.getContainerDataSource().addItem( hadoopNode );
                    }
                    Collection ls = workersSelect.getListeners( Property.ValueChangeListener.class );
                    Property.ValueChangeListener h =
                            ls.isEmpty() ? null : ( Property.ValueChangeListener ) ls.iterator().next();
                    if ( h != null )
                    {
                        workersSelect.removeValueChangeListener( workersSelectChangeListener );
                    }

                    try
                    {
                        List<EnvironmentContainerHost> containerHosts = new ArrayList<>();
                        for ( String id : config.getWorkers() )
                        {
                            containerHosts.add( hadoopEnvironment.getContainerHostById( id ) );
                        }
                        workersSelect.setValue( containerHosts );
                    }
                    catch ( ContainerHostNotFoundException e )
                    {
                        LOGGER.error( "Container hosts not found", e );
                    }
                    if ( h != null )
                    {
                        workersSelect.addValueChangeListener( workersSelectChangeListener );
                    }
                }
            }
        };
        coordinatorNodeCombo.addValueChangeListener( coordinatorComboChangeListener );

        //add value change handler
        workersSelect.addValueChangeListener( new Property.ValueChangeListener()
        {
            public void valueChange( Property.ValueChangeEvent event )
            {
                if ( event.getProperty().getValue() != null )
                {
                    Set<EnvironmentContainerHost> nodes =
                            ( Set<EnvironmentContainerHost> ) event.getProperty().getValue();
                    Set<String> workerList = new HashSet<>();
                    for ( EnvironmentContainerHost host : nodes )
                    {
                        workerList.add( host.getId() );
                    }
                    config.setWorkers( workerList );

                    //clear workers
                    if ( config.getCoordinatorNode() != null && config.getWorkers()
                                                                      .contains( config.getCoordinatorNode() ) )
                    {
                        config.setCoordinatorNode( null );
                        coordinatorNodeCombo.removeValueChangeListener( coordinatorComboChangeListener );
                        coordinatorNodeCombo.setValue( null );
                        coordinatorNodeCombo.addValueChangeListener( coordinatorComboChangeListener );
                    }
                }
            }
        } );

        parent.addComponent( hadoopClustersCombo );
        parent.addComponent( coordinatorNodeCombo );
        parent.addComponent( workersSelect );
    }


    //exclude hadoop nodes that are already in another presto cluster
    private List<String> filterNodes( List<String> hadoopNodes )
    {
        List<String> prestoNodes = new ArrayList<>();
        List<String> filteredNodes = new ArrayList<>();
        for ( PrestoClusterConfig prestoConfig : wizard.getPrestoManager().getClusters() )
        {
            prestoNodes.addAll( prestoConfig.getAllNodes() );
        }
        for ( String node : hadoopNodes )
        {
            if ( !prestoNodes.contains( node ) )
            {
                filteredNodes.add( node );
            }
        }
        return filteredNodes;
    }


    private void nextClickHandler( Wizard wizard )
    {
        PrestoClusterConfig config = wizard.getConfig();
        if ( Strings.isNullOrEmpty( config.getClusterName() ) )
        {
            show( "Enter cluster name" );
        }
        else if ( Strings.isNullOrEmpty( config.getHadoopClusterName() ) )
        {
            show( "Please, select Hadoop cluster" );
        }
        else if ( config.getCoordinatorNode() == null )
        {
            show( "Please, select coordinator node" );
        }
        else if ( CollectionUtil.isCollectionEmpty( config.getWorkers() ) )
        {
            show( "Please, select worker nodes" );
        }
        else
        {
            wizard.next();
        }
    }


    private void show( String notification )
    {
        Notification.show( notification );
    }
}