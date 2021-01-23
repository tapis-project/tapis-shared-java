package edu.utexas.tacc.tapis.sharedq;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.ConnectionFactory;

public class VHostConnectTest 
{
    private static final String HOST       = "localhost";
    private static final int    PORT       = 5672;
    private static final String JOBS_VHOST = "JobsHost";
    private static final String JOBS_USER  = "jobs";
    private static final String JOBS_PASS  = "password";
    
    private static final String CONN_NAME = "myconn";
    
    public static void main(String[] args) throws IOException, TimeoutException 
    {
        // Get a rabbitmq connection factory.
        var factory = new ConnectionFactory();
        
        // Set the factory parameters.
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(JOBS_USER);
        factory.setPassword(JOBS_PASS);
        factory.setVirtualHost(JOBS_VHOST);
        
        var conn = factory.newConnection(CONN_NAME);
        var props = conn.getClientProperties();
        for (var entry : props.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue().toString());
        }
        
    }
}
