package edu.utexas.tacc.tapis.sharedapi.responses;

import edu.utexas.tacc.tapis.sharedapi.responses.results.ResultBoolean;

public final class RespBoolean
 extends RespAbstract
{
    public RespBoolean(ResultBoolean result) {this.result = result;}
    
    public ResultBoolean result;
}
