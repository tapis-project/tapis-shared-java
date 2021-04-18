package edu.utexas.tacc.tapis.shared.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.gson.JsonObject;

import java.io.IOException;

/*
 * Custom Jackson serializer for JsonObject attribute. For example use see attribute "notes" in model
 * class TSystem from tapis-systems and model class App from tapis-apps.
 */
public class JsonObjectSerializer extends JsonSerializer<JsonObject> {

  @Override
  public void serialize(JsonObject jsonObject, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException
  {
    jsonGenerator.writeRawValue(jsonObject.toString());
  }
}