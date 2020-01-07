package com.gigaspaces.kafka.connector.internal;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * Background service of data sink
 */
public interface GigaspacesSinkService
{
  /**
   * Create new ingestion task from existing table and stage,
   * try to reused existing pipe and recovery previous task,
   * otherwise, create a new pipe.
   * @param tableName destination table name
   * @param topic topic name
   * @param partition partition index
   */
  void startTask(String tableName, String topic, int partition);

  /**
   * call pipe to insert a JSON record
   * @param record record content
   */
  void insert(SinkRecord record);

  /**
   * retrieve offset of last loaded record for given pipe name
   * @param topicPartition topic and partition
   * @return offset, or -1 for empty
   */
  long getOffset(TopicPartition topicPartition);

  /**
   * terminate all tasks and close this service instance
   */
  void close();

  /**
   * retrieve sink service status
   * @return true is closed
   */
  boolean isClosed();

  /**
   * change maximum number of record cached in buffer to control the flush rate,
   * 0 for unlimited
   * @param num a non negative long number represents number of record limitation
   */
  void setRecordNumber(long num);

  /**
   * change maximum data size of buffer to control the flush rate,
   * the maximum file size is controlled by
   * @param size a non negative long number represents data size limitation
   */
  void setFileSize(long size);

  /**
   * change flush rate of sink service
   * the minimum flush time is controlled by
   * @param time a non negative long number represents service flush time in seconds
   */
  void setFlushTime(long time);

  /**
   * @return current number of record limitation
   */
  long getRecordNumber();

  /**
   * @return current flush time in seconds
   */
  long getFlushTime();

  /**
   * @return current file size limitation
   */
  long getFileSize();

}
