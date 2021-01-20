package edu.utexas.tacc.tapis.sharedq;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.sharedq.exceptions.TapisQueueException;

public class VHostManager 
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    public static final String ALL_PERMISSIONS  = ".*";
    
    /* **************************************************************************** */
    /*                                    Fields                                    */
    /* **************************************************************************** */
    // Constructor parameters.
    private final VHostParms _parms;
    
    // Values cached on first use.
    private String     _cmdPrefix;
    private HttpClient _client;

    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public VHostManager(VHostParms parms)
    {
        if (parms == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "VHostManager", "parms");
            throw new TapisRuntimeException(msg);
        }
        _parms = parms;
    }
    
    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* initVHost:                                                                   */
    /* ---------------------------------------------------------------------------- */
    /** Create a new vhost with its own administrative user.  The administrative
     * user is assigned all permissions.  
     * 
     * It is not an error if the vhost or user already exist.
     * 
     * @param vhost the new vhost to be created
     * @param user the admin user for that vhost
     */
    public void initVHost(String vhost, String user, String userPassword)
     throws Exception
    {
        // Create the vhost if necessary.
        if (!hasVHost(vhost)) createVHost(vhost);
        
        // Create the admin user if necessary.
        if (!hasUser(user)) createUser(user, userPassword);
        
        // Assign permissions if necessary.
        if (!hasPerms(vhost, user)) assignPerms(vhost, user);
    }

    /* **************************************************************************** */
    /*                               Private Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* hasVHost:                                                                    */
    /* ---------------------------------------------------------------------------- */
    private boolean hasVHost(String vhost) 
     throws Exception
    {
        // List the existing vhosts.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(makeListVHostsUri()))
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = getClient().send(request, BodyHandlers.ofString());
        
        // Check http status code.
        int code = response.statusCode();
        if (code != 200 && code != 201) {
            String msg = MsgUtils.getMsg("QMG_HTTP_LIST_ERROR", "virtual hosts", code);
            throw new TapisQueueException(msg);
        }
            
        // Retrieve response payload.
        String vhosts = response.body();
        if (StringUtils.isBlank(vhosts)) {
            String msg = MsgUtils.getMsg("QMG_HTTP_NO_LIST_ERROR", "virtual hosts");
            throw new TapisQueueException(msg);
        }
        
        // Parse the json payload.
        var gsonArray = TapisGsonUtils.getGson().fromJson(vhosts, JsonArray.class);
        for (JsonElement obj : gsonArray) {
            if (!obj.isJsonObject()) continue;
            var nameElem = ((JsonObject)obj).get("name");
            if (nameElem == null) continue;
            if (nameElem.getAsString().equals(vhost)) return true;
        }
        
        // Not found if we get here.
        return false;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* hasUser:                                                                     */
    /* ---------------------------------------------------------------------------- */
    private boolean hasUser(String user) 
     throws Exception
    {
        // List the existing vhosts.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(makeListUsersUri()))
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = getClient().send(request, BodyHandlers.ofString());
        
        // Check http status code.
        int code = response.statusCode();
        if (code != 200 && code != 201) {
            String msg = MsgUtils.getMsg("QMG_HTTP_LIST_ERROR", "users", code);
            throw new TapisQueueException(msg);
        }
            
        // Retrieve response payload.
        String users = response.body();
        if (StringUtils.isBlank(users)) {
            String msg = MsgUtils.getMsg("QMG_HTTP_NO_LIST_ERROR", "users");
            throw new TapisQueueException(msg);
        }
        
        // Parse the json payload.
        var gsonArray = TapisGsonUtils.getGson().fromJson(users, JsonArray.class);
        for (JsonElement obj : gsonArray) {
            if (!obj.isJsonObject()) continue;
            var nameElem = ((JsonObject)obj).get("name");
            if (nameElem == null) continue;
            if (nameElem.getAsString().equals(user)) return true;
        }
        
        // Not found if we get here.
        return false;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* hasPerms:                                                                    */
    /* ---------------------------------------------------------------------------- */
    private boolean hasPerms(String vhost, String user)
     throws IOException, InterruptedException, TapisException
    {
        // List the existing vhosts.
        HttpRequest request = HttpRequest.newBuilder()
                      .uri(URI.create(makeListUserPermsUri(user)))
                      .header("Accept", "application/json")
                      .build();
        HttpResponse<String> response = getClient().send(request, BodyHandlers.ofString());
        
        // Check http status code.
        int code = response.statusCode();
        if (code != 200 && code != 201) {
            String msg = MsgUtils.getMsg("QMG_HTTP_LIST_ERROR", "users", code);
            throw new TapisQueueException(msg);
        }

        // Retrieve response payload.
        String users = response.body();
        if (StringUtils.isBlank(users)) {
            String msg = MsgUtils.getMsg("QMG_HTTP_NO_LIST_ERROR", "users");
            throw new TapisQueueException(msg);
        }
               
        // Parse the json payload.
        var gsonArray = TapisGsonUtils.getGson().fromJson(users, JsonArray.class);
        for (JsonElement obj : gsonArray) {
            if (!obj.isJsonObject()) continue;
            var userElem = ((JsonObject)obj).get("user");
            if (userElem == null || !userElem.getAsString().equals(user)) continue;
            var vhostElem = ((JsonObject)obj).get("vhost");
            if (vhostElem == null || !vhostElem.getAsString().equals(vhost)) continue;
                  
            // We have the user@vhost record.
            var configElem = ((JsonObject)obj).get("configure");
            var writeElem  = ((JsonObject)obj).get("write");
            var readElem   = ((JsonObject)obj).get("read");
                  
            // Make sure all perms are allowed.
            if (configElem == null || !configElem.getAsString().equals(ALL_PERMISSIONS)) return false;
            if (writeElem == null  || !writeElem.getAsString().equals(ALL_PERMISSIONS)) return false;
            if (readElem == null   || !readElem.getAsString().equals(ALL_PERMISSIONS)) return false;
            return true;
        }
                      
        // Not found if we get here.
        return false;
    }    
    
    /* ---------------------------------------------------------------------------- */
    /* createVHost:                                                                 */
    /* ---------------------------------------------------------------------------- */
    private void createVHost(String vhost)
     throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(makeCreateVHostUri(vhost)))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(BodyPublishers.ofString(""))
                .build();
        HttpResponse<String> response = getClient().send(request, BodyHandlers.ofString());
        int code = response.statusCode();
        if (code != 200 && code != 201) {
            String msg = MsgUtils.getMsg("QMG_HTTP_CREATE_ERROR", "virtual host", vhost, code);
            throw new TapisQueueException(msg);
        }
    }
    
    /* ---------------------------------------------------------------------------- */
    /* createUser:                                                                  */
    /* ---------------------------------------------------------------------------- */
    private void createUser(String user, String userPassword)
     throws Exception
    {
        String body = """
            {"password":%s, "tags":"administrator"}
            """.formatted("\"" + userPassword + "\"");
            
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(makeCreateUserUri(user)))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = getClient().send(request, BodyHandlers.ofString());
        int code = response.statusCode();
        if (code != 200 && code != 201) {
            String msg = MsgUtils.getMsg("QMG_HTTP_CREATE_ERROR", "user", user, code);
            throw new TapisQueueException(msg);
        }
    }
    
    /* ---------------------------------------------------------------------------- */
    /* assignPerms:                                                                 */
    /* ---------------------------------------------------------------------------- */
    private void assignPerms(String vhost, String user)
     throws IOException, InterruptedException, TapisException
    {
        // Hardcoded the ALL_PERMISSIONS here for simplicity.
        String body = """
            {"configure":".*", "write":".*", "read":".*"}
            """;
                   
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(makeAssignUserPermsUri(vhost, user)))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = getClient().send(request, BodyHandlers.ofString());
        int code = response.statusCode();
        if (code != 200 && code != 201) {
            String msg = MsgUtils.getMsg("QMG_HTTP_ASSIGN_ERROR", "permissions", vhost, user, code);
            throw new TapisQueueException(msg);
        }
    }
           
    /* ---------------------------------------------------------------------------- */
    /* getClient:                                                                   */
    /* ---------------------------------------------------------------------------- */
    private HttpClient getClient()
    {
        if (_client == null)
            _client = HttpClient.newBuilder()
                        .authenticator(new PasswordAuth())
                        .build();
        return _client;
    }
    
    /* **************************************************************************** */
    /*                             URL Parsing Methods                              */
    /* **************************************************************************** */
    private String makeListUserPermsUri(String user)
    {return makeHttpCmdPrefix() + "users" + "/" + user + "/permissions";}

    private String makeAssignUserPermsUri(String vhost, String user)
    {return makeHttpCmdPrefix() + "permissions" + "/" + vhost + "/" + user;}
    
    private String makeListUsersUri()
    {return makeHttpCmdPrefix() + "users";}
    
    private String makeCreateUserUri(String user)
    {return makeHttpCmdPrefix() + "users" + "/" + user;}
    
    private String makeListVHostsUri()
    {return makeHttpCmdPrefix() + "vhosts";}
    
    private String makeCreateVHostUri(String vhost)
    {return makeHttpCmdPrefix() + "vhosts" + "/" + vhost;}
    
    private String makeHttpCmdPrefix() 
    {
        if (_cmdPrefix == null)
            _cmdPrefix = "http://" + _parms.getHost() + ":" + _parms.getPort() + "/api/";
        return _cmdPrefix;
    }
    
    /* **************************************************************************** */
    /*                             PasswordAuth Class                               */
    /* **************************************************************************** */
    private final class PasswordAuth
    extends Authenticator
   {
       @Override
       public PasswordAuthentication getPasswordAuthentication()
       {
           return new PasswordAuthentication(_parms.getAdminUser(), _parms.getAdminPassword().toCharArray());
       }
   }
}
