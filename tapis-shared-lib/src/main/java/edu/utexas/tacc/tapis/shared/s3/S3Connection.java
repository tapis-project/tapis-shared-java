package edu.utexas.tacc.tapis.shared.s3;


import javax.ws.rs.core.UriBuilder;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class represents an S3 connection to a host+bucket.
 */
public class S3Connection implements AutoCloseable
{
  /* ********************************************************************** */
  /*                               Constants                                */
  /* ********************************************************************** */
  private static final Logger log = LoggerFactory.getLogger(S3Connection.class);

  /* ********************************************************************** */
  /*                                Fields                                  */
  /* ********************************************************************** */
  private final S3Client client;
  private final String bucket;
  private final String host;
  private final int port;
  private final String effectiveUserId;
  private String accessKey;
  private String accessSecret;

  @Override
  public void close() { if (client != null) client.close(); }

  public S3Connection(String host1, int port1, String bucket1, String username1,
                      String accessKey1, String accessSecret1)
          throws TapisException
  {
    // Check for missing inputs. This is unlikely, so no need yet to determine which ones.
    if (StringUtils.isBlank(host1) || StringUtils.isBlank(bucket1) || StringUtils.isBlank(username1) ||
        StringUtils.isBlank(accessKey1) || StringUtils.isBlank(accessSecret1))
    {
      // TODO create msg
      throw new TapisException(MsgUtils.getMsg("TAPIS_S3_CLIENT_MISSING_INPUT"));
    }
    host = host1;
    port = port1;
    bucket = bucket1;
    effectiveUserId = username1;
    accessKey = accessKey1;
    accessSecret = accessSecret1;

    // Determine the s3 region
    String reg = S3Utils.getS3Region(host);
    Region region;
    //For minio/other S3 compliant APIs, the region is not needed
    if (reg == null) region = Region.US_EAST_1;
    else region = Region.of(reg);

    // Determine endpoint and construct credentials
    AwsCredentials cred;
    URI endpoint;
    // We catch Exception here because AwsBasicCredentials.create() throws various exceptions.
    try
    {
      endpoint = configEndpoint(host, port);
      cred = AwsBasicCredentials.create(accessKey, accessSecret);
    }
    catch (Exception e)
    {
      // TODO create msg
      String msg = MsgUtils.getMsg("TAPIS_S3_CLIENT_ERR", host, port, bucket, effectiveUserId, e.getMessage());
      log.warn(msg);
      throw new TapisException(msg, e);
    }
    // If AWS returned null for credentials we cannot go on
    if (cred == null)
    {
      // TODO create msg
      String msg = MsgUtils.getMsg("TAPIS_S3_CLIENT_ERR", host, port, bucket, effectiveUserId,
                                   "AwsBasicCredentials.create returned null");
      log.warn(msg);
      throw new TapisException(msg);
    }
    S3ClientBuilder builder =
            S3Client.builder().region(region).credentialsProvider(StaticCredentialsProvider.create(cred));

    // Have to do the endpoint override if it is not a real AWS route, as in the case for a minio instance
    if (!S3Utils.isAWSUrl(host))
    {
      // TODO create msg
      log.debug(MsgUtils.getMsg("TAPIS_S3_CLIENT_EP_OVER", host, port, bucket, effectiveUserId, reg, endpoint.toString()));
      builder.endpointOverride(endpoint);
    }
    // Log info about client we are building
    // TODO create msg
    log.debug(MsgUtils.getMsg("TAPIS_S3_CLIENT_BUILD", host, port, bucket, effectiveUserId, reg, endpoint.toString()));
    // Build the client
    try
    {
      client = builder.build();
    }
    catch (Exception e)
    {
      // TODO create msg
      String msg = MsgUtils.getMsg("TAPIS_S3_CLIENT_ERR", host, port, bucket, effectiveUserId, e.getMessage());
      log.warn(msg);
      throw new TapisException(msg, e);
    }
  }

  /**
   * Build a URI using host, scheme, port
   *
   * @param host Host from the System
   * @param port port from the System
   * @return a URI
   * @throws URISyntaxException on error
   */
  URI configEndpoint(String host, int port) throws URISyntaxException
  {
    URI tmpURI = new URI(host);
    // Build a URI setting host, scheme, port
    UriBuilder uriBuilder = UriBuilder.fromUri("");
    uriBuilder.host(tmpURI.getHost()).scheme(tmpURI.getScheme());
    if (port > 0) uriBuilder.port(port);
    if (StringUtils.isBlank(tmpURI.getHost())) uriBuilder.host(host);
    //Make sure there is a scheme, and default to https if not.
    if (StringUtils.isBlank(tmpURI.getScheme())) uriBuilder.scheme("https");
    return uriBuilder.build();
  }
}