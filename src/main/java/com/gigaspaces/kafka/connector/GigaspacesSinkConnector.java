 package com.gigaspaces.kafka.connector;

import com.gigaspaces.kafka.connector.internal.GigaspacesConnectionServiceFactory;
import com.gigaspaces.kafka.connector.internal.Loader;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.openspaces.core.GigaSpace;

import java.util.*;
import java.util.logging.Logger;


/**
 * GigaspacesSinkConnector implements SinkConnector for Kafka Connect framework.
 * expects configuration: including topic names, partition numbers, Gigaspaces
 * connection info and credentials info
 * provides configuration to SinkTasks running on Kafka Connect Workers.
 */
public class GigaspacesSinkConnector extends SinkConnector
{
  public static final java.util.logging.Logger logger = Logger.getLogger(GigaspacesSinkConnector.class.getName());
  private Map<String, String> config; // connector configuration, provided by
  // user through kafka connect framework
  private String connectorName;       // unique name of this connector instance
  private GigaSpace gigaspace;
  private long connectorStartTime;



  /**
   * No-Arg constructor.
   * Required by Kafka Connect framework
   */
  public GigaspacesSinkConnector()
  {
  }


  /**
   * start method will only be called on a clean connector,
   * i.e. it has either just been instantiated and initialized or stop ()
   * has been invoked.
   * loads configuration and validates
   *
   * @param parsedConfig has the configuration settings
   */
  @Override
  public void start(final Map<String, String> parsedConfig)
  {
    Utils.checkConnectorVersion();
    logger.info("GigaspacesSinkConnector:start");
    connectorStartTime = System.currentTimeMillis();


    config = new HashMap<>(parsedConfig);

    connectorName = Utils.validateConfig(config);

    // config as a side effect
    gigaspace = GigaspacesConnectionServiceFactory
      .builder()
      .setProperties(parsedConfig)
      .build();
  }


  /**
   * stop method will be called to stop a connector,
   */
  @Override
  public void stop()
  {
    logger.info("GigaspacesSinkConnector:stop");
  }

  @Override
  public ConfigDef config() {
    return GigaspacesSinkConnectorConfig.configDef();
  }

  // TODO (post GA): override reconfigure(java.util.Map<java.lang.String,java
  // .lang.String> props)
  // Default implementation shuts down all external network connections.

  /**
   * @return Sink task class
   */
  @Override
  public Class<? extends Task> taskClass()
  {
    return GigaspacesSinkTask.class;
  }

  /**
   * taskConfigs method returns a set of configurations for SinkTasks based
   * on the current configuration,
   * producing at most 'maxTasks' configurations
   *
   * @param maxTasks maximum number of SinkTasks for this instance of
   *                 GigaspacesSinkConnector
   * @return a list containing 'maxTasks' copies of the configuration
   */
  @Override
  public List<Map<String, String>> taskConfigs(final int maxTasks)
  {

    List<Map<String, String>> taskConfigs = new ArrayList<>(maxTasks);
    for (int i = 0; i < maxTasks; i++)
    {
      Map<String, String> conf = new HashMap<>(config);
      conf.put(Utils.TASK_ID, i + "");
      taskConfigs.add(conf);
    }
    return taskConfigs;
  }

  /**
   * @return connector version
   */
  @Override
  public String version()
  {
    return Utils.VERSION;
  }
}
