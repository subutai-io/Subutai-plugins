package org.safehaus.subutai.plugin.presto.ui.manager;


import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.common.tracker.OperationState;
import org.safehaus.subutai.common.tracker.TrackerOperationView;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.presto.api.Presto;
import org.safehaus.subutai.plugin.presto.api.PrestoClusterConfig;

import com.google.common.base.Strings;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Window;


public class AddNodeWindow extends Window
{

    private final TextArea outputTxtArea;
    private final Label indicator;
    private volatile boolean track = true;


    public AddNodeWindow( final Presto presto, final ExecutorService executorService, final Tracker tracker,
                          final PrestoClusterConfig config, Set<ContainerHost> nodes )
    {
        super( "Add New Node" );
        setModal( true );

        setWidth( 600, Unit.PIXELS );
        setHeight( 400, Unit.PIXELS );

        GridLayout content = new GridLayout( 1, 3 );
        content.setSizeFull();
        content.setMargin( true );
        content.setSpacing( true );

        HorizontalLayout topContent = new HorizontalLayout();
        topContent.setSpacing( true );

        content.addComponent( topContent );
        topContent.addComponent( new Label( "Nodes:" ) );

        final ComboBox hadoopNodes = new ComboBox();
        hadoopNodes.setId( "PressHadoopNodesCombo" );
        hadoopNodes.setImmediate( true );
        hadoopNodes.setTextInputAllowed( false );
        hadoopNodes.setNullSelectionAllowed( false );
        hadoopNodes.setRequired( true );
        hadoopNodes.setWidth( 200, Unit.PIXELS );
        for ( ContainerHost node : nodes )
        {
            hadoopNodes.addItem( node );
            hadoopNodes.setItemCaption( node, node.getHostname() );
        }
        hadoopNodes.setValue( nodes.iterator().next() );

        topContent.addComponent( hadoopNodes );

        final Button addNodeBtn = new Button( "Add" );
        addNodeBtn.setId( "PresAddSelectedNode" );
        addNodeBtn.addStyleName( "default" );
        topContent.addComponent( addNodeBtn );

        final Button ok = new Button( "Ok" );
        ok.setId( "btnOk" );
        ok.addStyleName( "default" );
        ok.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                //close window
                track = false;
                close();
            }
        } );

        addNodeBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                addNodeBtn.setEnabled( false );
                showProgress();
                ContainerHost host = ( ContainerHost ) hadoopNodes.getValue();
                final UUID trackID = presto.addWorkerNode( config.getClusterName(), host.getHostname() );
                ok.setEnabled( false );
                executorService.execute( new Runnable()
                {

                    @Override
                    public void run()
                    {
                        while ( track )
                        {
                            TrackerOperationView po =
                                    tracker.getTrackerOperation( PrestoClusterConfig.PRODUCT_KEY, trackID );
                            if ( po != null )
                            {
                                setOutput(
                                        po.getDescription() + "\nState: " + po.getState() + "\nLogs:\n" + po.getLog() );
                                if ( po.getState() != OperationState.RUNNING )
                                {
                                    hideProgress();
                                    ok.setEnabled( true );
                                    break;
                                }
                            }
                            else
                            {
                                setOutput( "Product operation not found. Check logs" );
                                break;
                            }
                            try
                            {
                                Thread.sleep( 1000 );
                            }
                            catch ( InterruptedException ex )
                            {
                                break;
                            }
                        }
                    }
                } );
            }
        } );

        outputTxtArea = new TextArea( "Operation output" );
        outputTxtArea.setId( "outputTxtArea" );
        outputTxtArea.setRows( 10 );
        outputTxtArea.setColumns( 30 );
        outputTxtArea.setImmediate( true );
        outputTxtArea.setWordwrap( true );

        content.addComponent( outputTxtArea );

        indicator = new Label();
        indicator.setId( "indicator" );
        indicator.setIcon( new ThemeResource( "img/spinner.gif" ) );
        indicator.setContentMode( ContentMode.HTML );
        indicator.setHeight( 11, Unit.PIXELS );
        indicator.setWidth( 50, Unit.PIXELS );
        indicator.setVisible( false );


        HorizontalLayout bottomContent = new HorizontalLayout();
        bottomContent.addComponent( indicator );
        bottomContent.setComponentAlignment( indicator, Alignment.MIDDLE_RIGHT );
        bottomContent.addComponent( ok );

        content.addComponent( bottomContent );
        content.setComponentAlignment( bottomContent, Alignment.MIDDLE_RIGHT );

        setContent( content );
    }


    private void showProgress()
    {
        indicator.setVisible( true );
    }


    private void setOutput( String output )
    {
        if ( !Strings.isNullOrEmpty( output ) )
        {
            outputTxtArea.setValue( output );
            outputTxtArea.setCursorPosition( outputTxtArea.getValue().length() - 1 );
        }
    }


    private void hideProgress()
    {
        indicator.setVisible( false );
    }


    @Override
    public void close()
    {
        super.close();
        track = false;
    }
}
