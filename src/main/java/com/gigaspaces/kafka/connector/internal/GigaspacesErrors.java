package com.gigaspaces.kafka.connector.internal;

import com.gigaspaces.kafka.connector.GigaspacesSinkConnectorConfig;
import com.gigaspaces.kafka.connector.Utils;

import java.util.logging.Logger;

public enum GigaspacesErrors
{

  // connector configuration issues 0---
  ERROR_0001(
    "0001",
    "Invalid input connector configuration",
    "input kafka connector configuration is null, missing required values, " +
      "or wrong input value"
  ),
  ERROR_0002(
    "0002",
    "Missing space name",
    String.format("Gigaspaces space name is mandatory ({})", GigaspacesSinkConnectorConfig.GS_SPACE_NAME)
  ),
  ERROR_0003(
    "0003",
    "Missing required parameter",
    "one or multiple required parameters haven't be provided"
  ),
  ERROR_0004(
    "0004",
    "Empty Stage name",
    "Input Stage name is empty string or null"
  ),
  ERROR_0005(
    "0005",
    "Empty Table name",
    "Input Table name is empty string or null"
  ),
  ERROR_0006(
    "0006",
    "Empty Pipe name",
    "Input Pipe name is empty String or null"
  ),
  ERROR_0008(
    "0008",
    "Invalid staging file name",
    "File name format: <app_name>/<table_name>/<partition_number" +
      ">/<start_offset>_<end_offset>_<timestamp>.json.gz"
  ),
  ERROR_0010(
    "0010",
    "Invalid input record",
    "Input record value can't be parsed"
  ),
  ERROR_0011(
    "0011",
    "Failed to load schema from Schema Registry",
    "Schema ID doesn't exist"
  ),
  ERROR_0012(
    "0012",
    "Failed to connect schema registry service",
    "Schema registry service is not available"
  )
  ;


  //properties

  private final String name;
  private final String detail;
  private final String code;
  private static final Logger logger = Logger.getLogger(GigaspacesKafkaConnectorException.class.getName());

  GigaspacesErrors(String code, String name, String detail)
  {
    this.code = code;
    this.name = name;
    this.detail = detail;
  }

  public GigaspacesKafkaConnectorException getException()
  {
    return getException("");
  }
  public GigaspacesKafkaConnectorException getException(Exception e)
  {
    return getException(e);
  }


  public GigaspacesKafkaConnectorException getException(
    String msg)
  {

    if(msg == null || msg.isEmpty())
    {
      return new GigaspacesKafkaConnectorException(toString(), code);
    }
    else
    {
      String log_msg = String.format("Exception: {}\nError Code: {}\nDetail: {}\nMessage: {}",
              name, code, detail, msg);
      logger.info(log_msg);
      return new GigaspacesKafkaConnectorException(log_msg
              , code
      );
    }
  }

  public String getCode()
  {
    return code;
  }

  public String getDetail()
  {
    return this.detail;
  }

  @Override
  public String toString()
  {
    String log_msg = String.format("Exception: {}\nError Code: {}\nDetail: {}",
            name, code, detail);
    logger.info(log_msg);
    return log_msg;
  }
}

