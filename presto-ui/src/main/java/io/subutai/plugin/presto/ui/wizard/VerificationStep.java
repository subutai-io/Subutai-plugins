package io.subutai.plugin.presto.ui.wizard;


import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Window;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.common.ui.ConfigView;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.presto.api.Presto;
import io.subutai.plugin.presto.api.PrestoClusterConfig;
import io.subutai.server.ui.component.ProgressWindow;


public class VerificationStep extends Panel
{
    private final static Logger LOGGER = LoggerFactory.getLogger( VerificationStep.class );


    public VerificationStep( final Presto presto, final Hadoop hadoop, final ExecutorService executorService,
                             final Tracker tracker, EnvironmentManager environmentManager, final Wizard wizard )
    {

        setSizeFull();

        GridLayout grid = new GridLayout( 1, 5 );
        grid.setSpacing( true );
        grid.setMargin( true );
        grid.setSizeFull();

        Label confirmationLbl = new Label( "<strong>Please verify the installation settings "
                + "(you may change them by clicking on Back button)</strong><br/>" );
        confirmationLbl.setContentMode( ContentMode.HTML );

        final PrestoClusterConfig config = wizard.getConfig();

        ConfigView cfgView = new ConfigView( "Installation configuration" );
        cfgView.addStringCfg( "Installation name", config.getClusterName() );
        final HadoopClusterConfig hc = hadoop.getCluster( wizard.getConfig().getHadoopClusterName() );
        Environment hadoopEnvironment;
        try
        {
            hadoopEnvironment = environmentManager.loadEnvironment( hc.getEnvironmentId() );
        }
        catch ( EnvironmentNotFoundException e )
        {
            LOGGER.error( "Error getting environment by id: " + hc.getEnvironmentId(), e );
            return;
        }
        EnvironmentContainerHost coordinator;
        try
        {
            coordinator = hadoopEnvironment.getContainerHostById( wizard.getConfig().getCoordinatorNode() );
        }
        catch ( ContainerHostNotFoundException e )
        {
            LOGGER.error( "Container host not found", e );
            return;
        }
        Set<EnvironmentContainerHost> workers;
        try
        {
            workers = hadoopEnvironment.getContainerHostsByIds( wizard.getConfig().getWorkers() );
        }
        catch ( ContainerHostNotFoundException e )
        {
            LOGGER.error( "Container hosts not found", e );
            return;
        }
        cfgView.addStringCfg( "Hadoop cluster Name", wizard.getConfig().getHadoopClusterName() );
        cfgView.addStringCfg( "Master Node", coordinator.getHostname() );
        for ( EnvironmentContainerHost worker : workers )
        {
            cfgView.addStringCfg( "Slave nodes", worker.getHostname() + "" );
        }


        Button install = new Button( "Install" );
        install.setId( "PresVerInstall" );
        install.addStyleName( "default" );
        install.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                UUID trackId = presto.installCluster( config );

                ProgressWindow window =
                        new ProgressWindow( executorService, tracker, trackId, PrestoClusterConfig.PRODUCT_KEY );

                window.getWindow().addCloseListener( new Window.CloseListener()
                {
                    @Override
                    public void windowClose( Window.CloseEvent closeEvent )
                    {
                        wizard.init();
                    }
                } );
                getUI().addWindow( window.getWindow() );
            }
        } );

        Button back = new Button( "Back" );
        back.setId( "PresVerBack" );
        back.addStyleName( "default" );
        back.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                wizard.back();
            }
        } );

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent( back );
        buttons.addComponent( install );

        grid.addComponent( confirmationLbl, 0, 0 );

        grid.addComponent( cfgView.getCfgTable(), 0, 1, 0, 3 );

        grid.addComponent( buttons, 0, 4 );

        setContent( grid );
    }
}
