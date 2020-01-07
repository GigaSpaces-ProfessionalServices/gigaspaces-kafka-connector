package com.gigaspaces.kafka.connector.internal;

import com.gigaspaces.kafka.connector.internal.GigaspacesErrors;

public class GigaspacesKafkaConnectorException extends RuntimeException
{
  private final String code;
  GigaspacesKafkaConnectorException(String msg, String code)
  {
    super(msg);
    this.code = code;
  }

  public String getCode()
  {
    return code;
  }

  public boolean checkErrorCode(GigaspacesErrors error)
  {
    return this.code.equals(error.getCode());
  }
}
