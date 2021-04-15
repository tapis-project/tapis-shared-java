package edu.utexas.tacc.tapis.sharedapi.responses;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.utexas.tacc.tapis.shared.utils.JsonObjectSerializer;

public abstract class RespAbstract
{
    public String status;
    public String message;
    public String version;
    @JsonSerialize(using = JsonObjectSerializer.class)
    public Object metadata = new Object();
}
