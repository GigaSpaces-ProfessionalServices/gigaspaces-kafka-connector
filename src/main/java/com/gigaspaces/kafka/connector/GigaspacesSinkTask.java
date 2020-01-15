package com.gigaspaces.kafka.connector;

import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.kafka.connector.GigaspacesSinkConnectorConfig;
import com.gigaspaces.kafka.connector.internal.GigaspacesConnectionServiceFactory;
import com.gigaspaces.kafka.connector.internal.GigaspacesSinkService;
import com.gigaspaces.kafka.connector.internal.GigaspacesErrors;
import com.gigaspaces.kafka.connector.internal.Loader;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.openspaces.core.GigaSpace;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;


/**
 * GigaspacesSinkTask implements SinkTask for Kafka Connect framework.
 * expects configuration from GigaspacesSinkConnector
 * creates sink service instance
 * takes records loaded from those Kafka partitions,
 */
public class GigaspacesSinkTask extends SinkTask
{
  private static final long WAIT_TIME = 5 * 1000;//5 sec
  private static final int REPEAT_TIME = 12; //60 sec

  // connector configuration
  private Map<String, String> config = null;

  // config buffer.count.records -- how many records to buffer
  private long bufferCountRecords;
  // config buffer.size.bytes -- aggregate size in bytes of all records to buffer
  private long bufferSizeBytes;
  private long bufferFlushTime;


  private Map<String, String> topicsToClasses;
  private Map<String, Map<String, Parser >> classFields;

//  private GigaspacesSinkService sink = null;

  private GigaSpace conn = null;

  private static final Logger logger = Logger.getLogger(GigaspacesSinkTask.class.getName());

  /**
   * default constructor, invoked by kafka connect framework
   */
  public GigaspacesSinkTask()
  {
    topicsToClasses = new HashMap<>();

    //nothing
  }

  private static class Parser {
    private final Class<?> type;
    private final Function<String, Object> parser;

    private Parser(Class<?> type, Function<String, Object> parser) {
      this.type = type;
      this.parser = parser;
    }

    public static BigDecimal parseBigDecimal(String str){
      return new BigDecimal(str);
    }
  }
  private static Map<String, Parser> initDefaultParsers() {
    Map<String, Parser> result = new HashMap<>();

    result.put(String.class.getName(), new Parser(String.class, s -> s));
    result.put(boolean.class.getName(), new Parser(boolean.class, Boolean::parseBoolean));
    result.put(Boolean.class.getName(), new Parser(Boolean.class, Boolean::parseBoolean));
    result.put(byte.class.getName(), new Parser(byte.class, Byte::parseByte));
    result.put(Byte.class.getName(), new Parser(Byte.class, Byte::parseByte));
    result.put(short.class.getName(), new Parser(short.class, Short::parseShort));
    result.put(Short.class.getName(), new Parser(Short.class, Short::parseShort));
    result.put(int.class.getName(), new Parser(int.class, Integer::parseInt));
    result.put(Integer.class.getName(), new Parser(Integer.class, Integer::parseInt));
    result.put(long.class.getName(), new Parser(long.class, Long::parseLong));
    result.put(Long.class.getName(), new Parser(Long.class, Long::parseLong));
    result.put(float.class.getName(), new Parser(float.class, Float::parseFloat));
    result.put(Float.class.getName(), new Parser(Float.class, Float::parseFloat));
    result.put(double.class.getName(), new Parser(double.class, Double::parseDouble));
    result.put(Double.class.getName(), new Parser(Double.class, Double::parseDouble));
    result.put(BigDecimal.class.getName(), new Parser(BigDecimal.class, Parser::parseBigDecimal));
    result.put(char.class.getName(), new Parser(char.class, s -> s.charAt(0)));
    result.put(Character.class.getName(), new Parser(Character.class, s -> s.charAt(0)));
    result.put(java.time.LocalDate.class.getName(), new Parser(java.time.LocalDate.class, java.time.LocalDate::parse));
    result.put(java.time.LocalTime.class.getName(), new Parser(java.time.LocalTime.class, java.time.LocalTime::parse));
    result.put(java.time.LocalDateTime.class.getName(), new Parser(java.time.LocalDateTime.class, java.time.LocalDateTime::parse));
    result.put("string", result.get(String.class.getName()));
    result.put("date", result.get(java.time.LocalDate.class.getName()));
    result.put("time", result.get(java.time.LocalTime.class.getName()));
    result.put("datetime", result.get(java.time.LocalDateTime.class.getName()));

    return result;
  }


  /**
   * start method handles configuration parsing and one-time setup of the
   * task. loads configuration
   * @param parsedConfig - has the configuration settings
   */
  @Override
  public void start(final Map<String, String> parsedConfig)
  {
    logger.info(String.format("GigaspacesSinkTask:start"));

    this.config = parsedConfig;



    conn = GigaspacesConnectionServiceFactory
      .builder()
      .setProperties(parsedConfig)
      .build();
    Map<String, Class> classes =null;
    try{
      classes  = Loader.loadJar(config.get(GigaspacesSinkConnectorConfig.GS_MODEL_JAR_PATH));
      for(Map.Entry<String, Class> entry: classes.entrySet()){
        conn.readIfExistsById(entry.getValue(), "");
        topicsToClasses.put(entry.getValue().getSimpleName(), entry.getKey());
      }

    } catch (Exception e){
      logger.severe("Could not load jar " + config.get(GigaspacesSinkConnectorConfig.GS_MODEL_JAR_PATH));
      return;
    }
    Map<String, Parser> parsers = initDefaultParsers();
    for(Map.Entry<String, Class> entry: classes.entrySet()) {
      Field[] fields = entry.getValue().getDeclaredFields();
      Map<String, Parser> nameToParser = new HashMap<String, Parser>();
      for (Field f: fields){
        nameToParser.put(f.getName(), parsers.get(f.getType().getName()));
        logger.info("Add parser for field " + f.getName() + " for type " + f.getType().getName());
      }
      classFields.put(entry.getKey(), nameToParser);
    }



  }

  /**
   * stop method is invoked only once outstanding calls to other methods
   * have completed.
   * e.g. after current put, and a final preCommit has completed.
   */
  @Override
  public void stop()
  {
    logger.info("GigaspacesSinkTask:stop");

  }

  /**
   * init ingestion task in Sink service
   *
   * @param partitions - The list of all partitions that are now assigned to
   *                   the task
   */
  @Override
  public void open(final Collection<TopicPartition> partitions)
  {
    logger.info(String.format(
      "GigaspacesSinkTask:open, TopicPartitions: {}", partitions
    ));


    partitions.forEach(
      partition -> {
        if(!topicsToClasses.containsKey(partition.topic())){
          logger.info(String.format("Skipping topic {} since we do not have the model pojo for it", partition.topic()));
        }
      }
    );
  }


  /**
   * close sink service
   * close all running task because the parameter of open function contains all
   * partition info but not only the new partition
   * @param partitions - The list of all partitions that were assigned to the
   *                   task
   */
  @Override
  public void close(final Collection<TopicPartition> partitions)
  {
    logger.info("GigaspacesSinkTask:close");
  }

  /**
   *
   * @param records - collection of records from kafka topic/partitions for
   *                this connector
   */
  @Override
  public void put(final Collection<SinkRecord> records)
  {
    List<SpaceDocument> writeDocs = new ArrayList<>();
    List<SpaceDocument> takeDocs = new ArrayList<>();
    records.forEach((record)-> {
      String classname = topicsToClasses.get(record.topic());
      boolean write = true;
      if(null == classname) {
        logger.info(String.format("Ignoring topic {}", record.topic()));
      } else {
        if(record.valueSchema().type() == Schema.Type.MAP){
          SpaceDocument doc = new SpaceDocument();
          Map<String, Parser> fields = classFields.get(doc.getTypeName());
          if(null == fields){
            logger.severe("Could not find the class " + doc.getTypeName());
            return;
          }
          Map<String, String> payload = (Map<String,String>)record.value();
          for (Map.Entry<String, String> entry: payload.entrySet()) {
            if(entry.getKey().startsWith("__")) {
              if (entry.getKey().equals("__delete")) {
                write = false;
              }
            } else {
              Parser p = fields.get(entry.getKey());
              if(null == p){
                logger.warning("Could not find parser for type " + entry.getKey());
                continue;
              }
              doc.setProperty(entry.getKey(), p.parser.apply(entry.getValue()));
              doc.setProperty(entry.getKey(), entry.getValue());
            }
          }
          if(write)
            writeDocs.add(doc);
          else
            takeDocs.add(doc);
        } else {
          logger.info(String.format("Ignoring topic {} because schema type is {}", record.topic(), record.valueSchema().type().getName()));
        }
      }
    });
    if(writeDocs.size()>0){
      this.conn.writeMultiple(writeDocs.toArray());
    }
    if(takeDocs.size()>0){
      this.conn.takeMultiple(takeDocs.toArray());
    }
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
