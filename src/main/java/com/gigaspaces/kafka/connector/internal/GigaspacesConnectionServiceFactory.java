package com.gigaspaces.kafka.connector.internal;

import com.gigaspaces.kafka.connector.GigaspacesSinkConnectorConfig;
import com.gigaspaces.kafka.connector.Utils;
import com.gigaspaces.kafka.connector.internal.GigaspacesErrors;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.AbstractSpaceConfigurer;
import org.openspaces.core.space.EmbeddedSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.util.Map;
import java.util.Properties;

public class GigaspacesConnectionServiceFactory
{
  public static GigaspacesConnectionServiceBuilder builder()
  {
    return new GigaspacesConnectionServiceBuilder();
  }
  public static class GigaspacesConnectionServiceBuilder
  {
    private Properties prop;
    private String spaceName;
    private String locators;
    private String groups;

    public GigaspacesConnectionServiceBuilder setProperties(Properties prop)
    {
      this.prop = prop;
      return this;
    }

    public GigaspacesConnectionServiceBuilder setURL(String spaceName)
    {
      this.spaceName = spaceName;
      return this;
    }

    public GigaspacesConnectionServiceBuilder setLocators(String locators)
    {
      this.locators = locators;
      return this;
    }

    public GigaspacesConnectionServiceBuilder setGroups(String groups)
    {
      this.groups = groups;
      return this;
    }

    public GigaspacesConnectionServiceBuilder setProperties(Map<String, String> conf)
    {
      if(null == this.prop)
        this.prop = new Properties();
      this.prop.putAll(conf);
      if(!conf.containsKey(GigaspacesSinkConnectorConfig.GS_SPACE_NAME))
      {
        throw GigaspacesErrors.ERROR_0002.getException();
      }
      this.spaceName = conf.get(GigaspacesSinkConnectorConfig.GS_SPACE_NAME);
      return this;
    }

    public GigaSpace build()
    {
      //Mainlly
      if(prop.getProperty(GigaspacesSinkConnectorConfig.GS_IS_EMBEDDED_SPACE).equalsIgnoreCase("true")){
        EmbeddedSpaceConfigurer proxy = new EmbeddedSpaceConfigurer(prop.getProperty(GigaspacesSinkConnectorConfig.GS_SPACE_NAME));
        GigaSpace space = new GigaSpaceConfigurer(proxy).gigaSpace();
        return space;
      }
      SpaceProxyConfigurer proxy = new SpaceProxyConfigurer(prop.getProperty(GigaspacesSinkConnectorConfig.GS_SPACE_NAME));
      String locators = prop.getProperty(GigaspacesSinkConnectorConfig.GS_LOCATORS);
      if(null != locators )
        proxy.lookupLocators(locators);
      String groups = prop.getProperty(GigaspacesSinkConnectorConfig.GS_GROUPS);
      if(null != groups )
        proxy.lookupGroups(groups);
      String username = prop.getProperty(GigaspacesSinkConnectorConfig.GS_USERNAME);
      if(null != username )
        proxy.credentials(username, prop.getProperty(GigaspacesSinkConnectorConfig.GS_PASSWORD));
      GigaSpace space = new GigaSpaceConfigurer(proxy).gigaSpace();
      return space;
    }
  }
}
