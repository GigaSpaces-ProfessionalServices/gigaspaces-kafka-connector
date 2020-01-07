package com.gigaspaces.kafka.connector;


import org.apache.kafka.connect.errors.ConnectException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various arbitrary helper functions
 */
public class Utils
{

  public static final java.util.logging.Logger logger = Logger.getLogger(Utils.class.getName());
  //Connector version, change every release
  public static final String VERSION = "1.0.0";


  //constants strings
  private static final String KAFKA_OBJECT_PREFIX = "GS_KAFKA_CONNECTOR";

  //task id
  public static final String TASK_ID = "task_id";

  private static final String MVN_REPO =
    "http://repo1.maven.org/maven2/com/........";

  /**
   * check the connector version from Maven repo, report if any update
   * version is available.
   */
  static void checkConnectorVersion()
  {
    logger.info("Gigaspaces Kafka Connector Version: " + VERSION);
    try
    {
      String latestVersion = null;
      int largestNumber = 0;
      URL url = new URL(MVN_REPO);
      InputStream input = url.openStream();
      BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(input));
      String line;
      Pattern pattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+?)");
      while ((line = bufferedReader.readLine()) != null)
      {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find())
        {
          String version = matcher.group(1);
          String[] numbers = version.split("\\.");
          int num =
            Integer.parseInt(numbers[0]) * 10000 +
              Integer.parseInt(numbers[1]) * 100 +
              Integer.parseInt(numbers[2]);
          if (num > largestNumber)
          {
            largestNumber = num;
            latestVersion = version;
          }
        }
      }

      if (latestVersion == null)
      {
        throw new Exception("can't retrieve version number from Maven repo");
      }
      else if (!latestVersion.equals(VERSION))
      {
        logger.warning(String.format(" upgrade Kafka Connector ({} -> {}) " +
                        "Connector update is available, please" , VERSION,
          latestVersion));
      }
    } catch (Exception e)
    {
      logger.warning(String.format("can't verify latest connector version " +
        "from Maven Repo\n{}", e.getMessage()));
    }


  }

  /**
   * @param appName connector name
   * @return connector object prefix
   */
  private static String getObjectPrefix(String appName)
  {
    return KAFKA_OBJECT_PREFIX + "_" + appName;
  }

  /**
   * generate stage name by given table
   *
   * @param appName connector name
   * @param table   table name
   * @return stage name
   */
  public static String stageName(String appName, String table)
  {
    String stageName = getObjectPrefix(appName) + "_STAGE_" + table;

    logger.fine(String.format("generated stage name: {}", stageName));

    return stageName;
  }

  /**
   * generate pipe name by given table and partition
   *
   * @param appName   connector name
   * @param table     table name
   * @param partition partition name
   * @return pipe name
   */
  public static String pipeName(String appName, String table, int partition)
  {
    String pipeName = getObjectPrefix(appName) + "_PIPE_" + table + "_" +
      partition;

    logger.fine(String.format("generated pipe name: {}", pipeName));

    return pipeName;
  }

  
  /**
   * Validate input configuration
   *
   * @param config configuration Map
   * @return connector name
   */
  static String validateConfig(Map<String, String> config) throws ConnectException {
    boolean configIsValid = true; // verify all config

    // define the input parameters / keys in one place as static constants,
    // instead of using them directly
    // define the thresholds statically in one place as static constants,
    // instead of using the values directly

    // unique name of this connector instance
    String connectorName =
      config.getOrDefault(GigaspacesSinkConnectorConfig.NAME, "");
    if (connectorName.isEmpty() )
    {
      logger.severe(String.format("{} is empty or invalid. It " +
        "documentation.", GigaspacesSinkConnectorConfig.NAME));
      configIsValid = false;
    }
/*
    //verify buffer.flush.time
    if (!config.containsKey(GigaspacesSinkConnectorConfig.BUFFER_FLUSH_TIME_SEC))
    {
      logger.severe(String.format("{} is empty",
        GigaspacesSinkConnectorConfig.BUFFER_FLUSH_TIME_SEC));
      configIsValid = false;
    }
    else
    {
      try
      {
        long time = Long.parseLong(
          config.get(GigaspacesSinkConnectorConfig.BUFFER_FLUSH_TIME_SEC));
        if (time < GigaspacesSinkConnectorConfig.BUFFER_FLUSH_TIME_SEC_MIN)
        {
          logger.severe((String.format("{} is {}, it should be greater " +
              "than {}",
            GigaspacesSinkConnectorConfig.BUFFER_FLUSH_TIME_SEC, time,
            GigaspacesSinkConnectorConfig.BUFFER_FLUSH_TIME_SEC_MIN)));
          configIsValid = false;
        }
      } catch (Exception e)
      {
        logger.severe(String.format("{} should be an integer",
          GigaspacesSinkConnectorConfig.BUFFER_FLUSH_TIME_SEC));
        configIsValid = false;
      }
    }

    //verify buffer.count.records
    if (!config.containsKey(GigaspacesSinkConnectorConfig.BUFFER_COUNT_RECORDS))
    {
      logger.severe(String.format("{} is empty",
        GigaspacesSinkConnectorConfig.BUFFER_COUNT_RECORDS));
      configIsValid = false;
    }
    else
    {
      try
      {
        long num = Long.parseLong(
          config.get(GigaspacesSinkConnectorConfig.BUFFER_COUNT_RECORDS));
        if (num < 0)
        {
          logger.severe(String.format("{} is {}, it should not be negative",
            GigaspacesSinkConnectorConfig.BUFFER_COUNT_RECORDS, num));
          configIsValid = false;
        }
      } catch (Exception e)
      {
        logger.severe(String.format("{} should be an integer",
          GigaspacesSinkConnectorConfig.BUFFER_COUNT_RECORDS));
        configIsValid = false;
      }
    }

    //verify buffer.size.bytes
    if (config.containsKey(GigaspacesSinkConnectorConfig.BUFFER_SIZE_BYTES))
    {
      try
      {
        long bsb = Long.parseLong(config.get(GigaspacesSinkConnectorConfig
          .BUFFER_SIZE_BYTES));
        if (bsb > GigaspacesSinkConnectorConfig.BUFFER_SIZE_BYTES_MAX)   // 100mb
        {
          logger.severe(String.format("{} is too high at {}. It must be " +
              "{} or smaller.",
            GigaspacesSinkConnectorConfig.BUFFER_SIZE_BYTES, bsb,
            GigaspacesSinkConnectorConfig.BUFFER_SIZE_BYTES_MAX));
          configIsValid = false;
        }
      } catch (Exception e)
      {
        logger.severe(String.format("{} should be an integer",
          GigaspacesSinkConnectorConfig.BUFFER_SIZE_BYTES));
        configIsValid = false;
      }
    }
    else
    {
      logger.severe(String.format("{} is empty",
        GigaspacesSinkConnectorConfig.BUFFER_SIZE_BYTES));
      configIsValid = false;
    }
*/

    // sanity check
    if (!config.containsKey(GigaspacesSinkConnectorConfig.GS_SPACE_NAME))
    {
      logger.severe(String.format("{} cannot be empty.",
        GigaspacesSinkConnectorConfig.GS_SPACE_NAME));
      configIsValid = false;
    }


/*    if (!config.containsKey(GigaspacesSinkConnectorConfig.GS_USERNAME))
    {
      logger.severe(String.format("{} cannot be empty.",
        GigaspacesSinkConnectorConfig.GS_USERNAME));
      configIsValid = false;
    }
*/
    if (!config.containsKey(GigaspacesSinkConnectorConfig.GS_SPACE_NAME))
    {
      logger.severe(String.format("{} cannot be empty.",
        GigaspacesSinkConnectorConfig.GS_SPACE_NAME));
      configIsValid = false;
    }

    if (!configIsValid)
    {
      throw new ConnectException("Bad configuration");
    }

    return connectorName;
  }

}
