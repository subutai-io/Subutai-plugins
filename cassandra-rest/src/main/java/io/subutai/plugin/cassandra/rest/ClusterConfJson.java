package io.subutai.plugin.cassandra.rest;

import java.util.Set;


public class ClusterConfJson
{
    private String name;
    private String domainName;
    private String dataDir;
    private String commitDir;
    private String cacheDir;
    private Set<String> containers;
    private Set<String> seeds;


    public String getName()
    {
        return name;
    }


    public void setName( final String name )
    {
        this.name = name;
    }


    public String getDomainName()
    {
        return domainName;
    }


    public void setDomainName( final String domainName )
    {
        this.domainName = domainName;
    }


    public String getDataDir()
    {
        return dataDir;
    }


    public void setDataDir( final String dataDir )
    {
        this.dataDir = dataDir;
    }


    public String getCommitDir()
    {
        return commitDir;
    }


    public void setCommitDir( final String commitDir )
    {
        this.commitDir = commitDir;
    }


    public String getCacheDir()
    {
        return cacheDir;
    }


    public void setCacheDir( final String cacheDir )
    {
        this.cacheDir = cacheDir;
    }


    public Set<String> getContainers()
    {
        return containers;
    }


    public void setContainers( final Set<String> containers )
    {
        this.containers = containers;
    }


    public Set<String> getSeeds()
    {
        return seeds;
    }


    public void setSeeds( final Set<String> seeds )
    {
        this.seeds = seeds;
    }
}
