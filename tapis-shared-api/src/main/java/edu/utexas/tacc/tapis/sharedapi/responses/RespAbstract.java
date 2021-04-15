package edu.utexas.tacc.tapis.sharedapi.responses;

import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultMetadata;

public abstract class RespAbstract
{
    public String status;
    public String message;
    public String version;
    public ResultMetadata metadata;
}
