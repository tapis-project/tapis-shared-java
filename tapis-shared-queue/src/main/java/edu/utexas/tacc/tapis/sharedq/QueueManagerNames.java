package edu.utexas.tacc.tapis.sharedq;

import com.rabbitmq.client.AMQP.BasicProperties;

public class QueueManagerNames 
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  // ----- RabbitMQ naming constants.
  // Binding key to use with fanout exchanges.
  public static final String DEFAULT_BINDING_KEY = "";
  
  // The prefix to all queuing components for the jobs service.
  private static final String TAPIS_QUEUE_PREFIX = "tapis.service.";
  
  // Alternate exchange and queue name components.
  private static final String ALT_EXCHANGE_NAME = TAPIS_QUEUE_PREFIX + "alt.Exchange";
  private static final String ALT_QUEUE_NAME    = TAPIS_QUEUE_PREFIX + "alt.Queue";
  
  // Dead letter exchange and queue name components.
  private static final String DEADLETTER_EXCHANGE_NAME = TAPIS_QUEUE_PREFIX + "dead.Exchange";
  private static final String DEADLETTER_QUEUE_NAME    = TAPIS_QUEUE_PREFIX + "dead.Queue";
  
  // ----- RabbitMQ naming constants.
  // Used to build connection names.
  private static final String OUT_CONNECTION_SUFFIX = "-OutConnection";
  private static final String IN_CONNECTION_SUFFIX  = "-InConnection";
  
  // ----- RabbitMQ pre-configured properties objects.
  public static final BasicProperties PERSISTENT_JSON =
                        new BasicProperties("application/json",
                                            null,
                                            null,
                                            2,
                                            0, null, null, null,
                                            null, null, null, null,
                                            null, null);
  
  public static final BasicProperties PERSISTENT_TEXT =
                        new BasicProperties("text/plain",
                                            null,
                                            null,
                                            2,
                                            0, null, null, null,
                                            null, null, null, null,
                                            null, null);
 
  /* ********************************************************************** */
  /*                              Public Methods                            */
  /* ********************************************************************** */
  /* ---------------------------------------------------------------------- */
  /* getOutConnectionName:                                                  */
  /* ---------------------------------------------------------------------- */
  public static String getOutConnectionName(String instanceName)
  {
      return instanceName + OUT_CONNECTION_SUFFIX;
  }
  
  /* ---------------------------------------------------------------------- */
  /* getInConnectionName:                                                   */
  /* ---------------------------------------------------------------------- */
  public static String getInConnectionName(String instanceName)
  {
      return instanceName + IN_CONNECTION_SUFFIX;
  }
  
  /* ---------------------------------------------------------------------- */
  /* getAltQueueName:                                                       */
  /* ---------------------------------------------------------------------- */
  /** Create the multi-tenant queue name used to access unrouteable messages.
   * 
   * @return the tenant alternate route queue name
   */
  public static String getAltQueueName()
  {
    return ALT_QUEUE_NAME;
  }
  
  /* ---------------------------------------------------------------------- */
  /* getDeadLetterQueueName:                                                */
  /* ---------------------------------------------------------------------- */
  /** Create the multi-tenant topic queue name used to access dead letter
   * messages.
   * 
   * @return the tenant dead letter queue name
   */
  public static String getDeadLetterQueueName()
  {
    return DEADLETTER_QUEUE_NAME;
  }
  
  /* ---------------------------------------------------------------------- */
  /* getAltExchangeName:                                                    */
  /* ---------------------------------------------------------------------- */
  /** Create the multi-tenant exchange name used to communicate with
   * the tenant's alternate exchange queue.  This exchange captures
   * message that would otherwise be unrouteable.
   * 
   * @return the tenant alternate exchange name
   */
  public static String getAltExchangeName()
  {
    return ALT_EXCHANGE_NAME;
  }
  
  /* ---------------------------------------------------------------------- */
  /* getDeadLetterExchangeName:                                             */
  /* ---------------------------------------------------------------------- */
  /** Create the multi-tenant exchange name used to communicate with
   * the tenant's dead letter queue.  This exchanges captures messages that
   * have either:
   * 
   *    - Been rejected (basic.reject or basic.nack) with requeue=false,
   *    - Have their TTL expires, or
   *    - Would have caused a queue length limit to be exceeded.
   * 
   * @return the tenant dead letter exchange name
   */
  public static String getDeadLetterExchangeName()
  {
    return DEADLETTER_EXCHANGE_NAME;
  }
  
}
