package org.safehaus.subutai.plugin.hipi.ui.wizard;


import org.safehaus.subutai.common.util.FileUtil;
import org.safehaus.subutai.plugin.hipi.ui.HipiPortalModule;

import com.vaadin.server.FileResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;


public class WelcomeStep extends Panel
{

    public WelcomeStep( final Wizard wizard )
    {

        setSizeFull();

        GridLayout grid = new GridLayout( 10, 6 );
        grid.setSpacing( true );
        grid.setMargin( true );
        grid.setSizeFull();

        Label welcomeMsg = new Label( "<center><h2>Welcome to Hipi Installation Wizard!</h2>" );
        welcomeMsg.setContentMode( ContentMode.HTML );
        grid.addComponent( welcomeMsg, 3, 1, 6, 2 );

        Label logoImg = new Label();
        logoImg.setIcon( new FileResource( FileUtil.getFile( HipiPortalModule.MODULE_IMAGE, this ) ) );
        logoImg.setContentMode( ContentMode.HTML );
        logoImg.setHeight( 200, Unit.PIXELS );
        logoImg.setWidth( 180, Unit.PIXELS );
        grid.addComponent( logoImg, 1, 3, 2, 5 );

        Button next = new Button( "Start installation" );
        next.setId( "hipiStartOverHadoop" );
        next.addStyleName( "default" );
        grid.addComponent( next, 4, 4, 4, 4 );
        grid.setComponentAlignment( next, Alignment.BOTTOM_RIGHT );

        next.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                clickHandler( wizard );
            }
        } );


        setContent( grid );
    }


    private void clickHandler( Wizard wizard )
    {
        wizard.init();
        wizard.next();
    }
}