package edu.utexas.tacc.tapis.shared.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.gson.JsonArray;

import java.io.IOException;

/*
 * Custom Jackson serializer for a JsonArray attribute. For example use see attribute "notes" in model
 * class TSystem from tapis-systems and model class App from tapis-apps.
 */
public class JsonArraySerializer extends JsonSerializer<JsonArray> {

  @Override
  public void serialize(JsonArray jsonArray, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException
  {
    jsonGenerator.writeRawValue(jsonArray.toString());
  }
}