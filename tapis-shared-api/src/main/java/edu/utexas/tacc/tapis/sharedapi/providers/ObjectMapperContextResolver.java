package edu.utexas.tacc.tapis.sharedapi.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utexas.tacc.tapis.shared.utils.TapisObjectMapper;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/*
 * Custom mapper for Jackson.
 * Uses TapisObjectMapper in order to properly handle timestamps.
 */
@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper>
{
  private final ObjectMapper mapper;

  public ObjectMapperContextResolver()
  {
    this.mapper = createObjectMapper();
  }

  @Override
  public ObjectMapper getContext(Class<?> type)
  {
    return mapper;
  }

  private ObjectMapper createObjectMapper()
  {
    ObjectMapper mapper = new TapisObjectMapper().getMapper();
    return mapper;
  }
}