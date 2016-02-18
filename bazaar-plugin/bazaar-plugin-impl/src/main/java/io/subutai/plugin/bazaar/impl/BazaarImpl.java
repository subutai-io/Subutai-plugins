package io.subutai.plugin.bazaar.impl;


import io.subutai.common.dao.DaoManager;
import io.subutai.plugin.bazaar.api.Bazaar;
import io.subutai.plugin.bazaar.api.dao.ConfigDataService;
import io.subutai.plugin.bazaar.api.model.Plugin;
import io.subutai.plugin.bazaar.impl.dao.ConfigDataServiceImpl;
import io.subutai.plugin.hub.api.HubPluginException;
import io.subutai.plugin.hub.api.Integration;

import java.util.List;

public class BazaarImpl implements Bazaar
{

	private Integration integration;
	private DaoManager daoManager;
	private ConfigDataService configDataService;


	public BazaarImpl (final Integration integration, final DaoManager daoManager)
	{
		this.daoManager = daoManager;
		this.configDataService = new ConfigDataServiceImpl (this.daoManager);
		this.integration = integration;
		try
		{
			this.integration.registerPeer ("hub.subut.ai");
		}
		catch (HubPluginException e)
		{
			e.printStackTrace ();
		}
	}


	@Override
	public String getProducts()
	{
		try
		{
			String result = this.integration.getProducts();
			return result;
		}
		catch (HubPluginException e)
		{
			e.printStackTrace ();
		}
		return "";
	}

	@Override
	public List<Plugin> getPlugins ()
	{
		return this.configDataService.getPlugins();
	}

	@Override
	public void installPlugin (String name, String version, String kar, String url) throws HubPluginException
	{
		this.integration.installPlugin (kar);
		this.configDataService.savePlugin (name, version, kar, url);
	}

	@Override
	public void uninstallPlugin (Long id, String kar)
	{
		this.integration.uninstallPlugin (kar);
		this.configDataService.deletePlugin (id);
	}
}