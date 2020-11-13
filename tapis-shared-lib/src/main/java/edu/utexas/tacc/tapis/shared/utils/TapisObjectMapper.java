package edu.utexas.tacc.tapis.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Object mapper that serializes timestamps in a standard string format (e.g. 2020-11-11T22:07:05.304Z) instead
 *  of a complex object.
 * This class is a singleton for getting the default object mapper for the files service. This way, we can
 * use the mapper the web api and also when serializing objects in rabbitmq etc.
 */
public class TapisObjectMapper
{
  private static ObjectMapper mapper;
  static
  {
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }
  public static ObjectMapper getMapper() { return mapper; }
}
