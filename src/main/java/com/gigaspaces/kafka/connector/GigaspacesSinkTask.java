package com.gigaspaces.kafka.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.kafka.connector.internal.GigaspacesConnectionServiceFactory;
import com.gigaspaces.kafka.connector.internal.Loader;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;
import com.gigaspaces.metadata.index.SpaceIndexType;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.openspaces.core.GigaSpace;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.logging.Level;


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
  private Map<String, SpaceTypeDescriptor> topicsToTypeDescriptor;

//  private GigaspacesSinkService sink = null;

  private GigaSpace conn = null;

  private static final Logger logger = Logger.getLogger(GigaspacesSinkTask.class.getName());

  /**
   * default constructor, invoked by kafka connect framework
   */
  public GigaspacesSinkTask()
  {
    topicsToClasses = new HashMap<>();
    topicsToTypeDescriptor = new HashMap<>();
    classFields = new HashMap<>();
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
    String jar = config.get(GigaspacesSinkConnectorConfig.GS_MODEL_JAR_PATH);
    String jsonModel = config.get(GigaspacesSinkConnectorConfig.GS_MODEL_JSON_PATH);
    try{
      if(null != jar && jar.length() > 0) {
        classes = Loader.loadJar(config.get(GigaspacesSinkConnectorConfig.GS_MODEL_JAR_PATH));
        for (Map.Entry<String, Class> entry : classes.entrySet()) {
          this.conn.getTypeManager().registerTypeDescriptor(entry.getValue());
          topicsToClasses.put(entry.getValue().getSimpleName(), entry.getValue().getName());
        }
      } else if(null != jsonModel && jsonModel.length() > 0) {
        Path path = Paths.get(jsonModel);
        if (Files.exists(path)) {
          List<Map<String, Object>> models = new ObjectMapper().readValue(path.toFile(), ArrayList.class);
          for (Map<String, Object> json : models
          ) {
            String typeName = json.get("type").toString();
            SpaceTypeDescriptorBuilder builder = new SpaceTypeDescriptorBuilder(typeName);
            Map<String, String> fixedProps = (Map<String, String>) json.get("FixedProperties");
            fixedProps.forEach((k, v) -> builder.addFixedProperty(k, v));
            Map<String, Map<String, Object>> indexes = (Map<String, Map<String, Object>>) json.get("Indexes");
            indexes.forEach((k, v) -> {
              String[] properties = ((List<String>) v.get("properties")).toArray(new String[0]);
              if (properties.length == 1) {
                builder.addPropertyIndex(properties[0],
                        SpaceIndexType.valueOf((String) v.get("type")), (Boolean)v.get("unique"));
              } else {
                if (! ((String) v.get("type")).equalsIgnoreCase(SpaceIndexType.EQUAL.name()))
                  logger.warning("only EQUAL index type is supported for compoundindex");
                builder.addCompoundIndex(properties, (Boolean) v.get("unique"));
              }
            });
            if(json.containsKey("Id")) {
              Map<String, Object> id = (Map<String, Object>) json.get("Id");
              boolean autoGenerated = false;
              if(id.containsKey("autogenerate"))
                autoGenerated = (Boolean)id.get("autogenerate");
              builder.idProperty((String) id.get("field"), autoGenerated);
            }
            if(json.containsKey("RoutingProperty")) {
              builder.routingProperty((String) json.get("RoutingProperty"));
            } else {
              logger.warning("RoutingProperty is missing. Will not be able to use partitioned space");
            }
            SpaceTypeDescriptor std = builder.create();
            this.conn.getTypeManager().registerTypeDescriptor(std);
            String[] parts = typeName.split("[.]");
            String simpleTypeName = parts[parts.length - 1];
            topicsToTypeDescriptor.put(simpleTypeName, std);
            topicsToClasses.put(simpleTypeName, typeName);
          }
        }
      }

    } catch (Exception e){
      logger.log(Level.SEVERE, "Could not load model data", e);
      return;
    }
    Map<String, Parser> parsers = initDefaultParsers();
    if(null != classes){
      for(Map.Entry<String, Class> entry: classes.entrySet()) {
        logger.info(String.format("Registering type %s", entry.getKey() ));
        Field[] fields = entry.getValue().getDeclaredFields();
        Map<String, Parser> nameToParser = new HashMap<String, Parser>();
        for (Field f: fields){
          nameToParser.put(f.getName(), parsers.get(f.getType().getName()));
          logger.info("Add parser for field " + f.getName() + " for type " + f.getType().getName());
        }
        classFields.put(entry.getKey(), nameToParser);
      }
    } else {
      Map<String, Parser> nameToParser = new HashMap<String, Parser>();
      this.topicsToTypeDescriptor.entrySet().forEach(entry->{
        String[] names = entry.getValue().getPropertiesNames();
        String[] types = entry.getValue().getPropertiesTypes();
        for (int i = 0; i < names.length; i++) {
          nameToParser.put(names[i], parsers.get(types[i]));
        }
        classFields.put(entry.getKey(), nameToParser);
      });
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
      "GigaspacesSinkTask:open, TopicPartitions: %s", partitions
    ));


    partitions.forEach(
      partition -> {
        if(!topicsToClasses.containsKey(partition.topic()) && !topicsToTypeDescriptor.containsKey(partition.topic())){
          logger.info(String.format("Skipping topic %s (%d) since we do not have the model pojo or json file for it", partition.topic(), partition.partition()));
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
    logger.info(String.format("Call put with record count %d", records.size()));
    List<SpaceDocument> writeDocs = new ArrayList<>();
    List<SpaceDocument> takeDocs = new ArrayList<>();
    Map<String, Parser> parsers = initDefaultParsers();
    records.forEach((record)-> {
      String classname = topicsToClasses.get(record.topic());
      SpaceTypeDescriptor descriptor = this.topicsToTypeDescriptor.containsKey(record.topic())?
              this.topicsToTypeDescriptor.get(record.topic()): null;
      boolean write = true;
      if(null == classname) {
        logger.info(String.format("Ignoring topic %s", record.topic()));
        logger.info(String.format("Topics registered are %s", topicsToClasses.keySet()));
      } else {
        SpaceDocument doc = new SpaceDocument(classname);
        Map<String, Parser> fields = null;
        if(classFields.containsKey(record.topic())){
          fields= classFields.get(record.topic());
        }
        else if(null != record.valueSchema() && record.valueSchema().type() == Schema.Type.MAP) {
          fields = classFields.get(doc.getTypeName());
        }
        if (null == fields) {
          logger.severe("Could not find the class " + doc.getTypeName());
          return;
        }
        String payloadStr;
        if(record.value() instanceof String){
          payloadStr = record.value().toString();
        } else {
          payloadStr = ((Map<String, String>)record.value()).get("payload");
        }
        Map<String, Object> payload;
        try {
          payload = new ObjectMapper().readValue(payloadStr, HashMap.class);
        } catch (IOException e) {
          logger.severe("Could not map payload to Map" + e.getMessage());
          return;
        }
        for (Map.Entry<String, Object> entry: payload.entrySet()) {
            Parser p = fields.get(entry.getKey());
            if(null == p){
                doc.setProperty(entry.getKey(), entry.getValue());
            } else {
              Object v = entry.getValue();
              if (v instanceof String)
                doc.setProperty(entry.getKey(), p.parser.apply((String) v));
              else
                doc.setProperty(entry.getKey(), entry.getValue());
            }
        }
        if(write)
          writeDocs.add(doc);
        else
          takeDocs.add(doc);
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
