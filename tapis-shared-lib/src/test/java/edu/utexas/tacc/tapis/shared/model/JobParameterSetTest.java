package edu.utexas.tacc.tapis.shared.model;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.exceptions.TapisJSONException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import edu.utexas.tacc.tapis.shared.schema.JsonValidator;
import edu.utexas.tacc.tapis.shared.schema.JsonValidatorSpec;
import edu.utexas.tacc.tapis.shared.utils.TapisGsonUtils;
import edu.utexas.tacc.tapis.shared.utils.gson.javatime.Converters;

/** This class tests the ability to marshal a jobs parameterSet json object into a 
 * JobParameterSet Java object.  Apps and Jobs share the parameterSet model.
 * 
 * Each test generates some json, validates that the json conforms to the parameterSet
 * schema and then marshals the json into a JobParameterSet object.
 * 
 * @author rcardone
 */
@Test(groups={"unit"})
public class JobParameterSetTest 
{
//    /* **************************************************************************** */
//    /*                                   Constants                                  */
//    /* **************************************************************************** */
//    // The schema file for job submit requests.
//    private static final String FILE_JOB_PARMSET_TEST = "/JobParameterSetTest.json";
//    
//    // We only need one gson object.
//    private final Gson _gson = TapisGsonUtils.getGson(true);
//
//    /* ********************************************************************** */
//    /*                                 Tests                                  */
//    /* ********************************************************************** */
//    @Test(enabled=false)
//    public void parseTest1() throws TapisException
//    {
//        // Test minimal input.
//        String json = getInputTest1();
//        parse(wrapForParsing(json));
//        JobParameterSet parmSet = _gson.fromJson(json, JobParameterSet.class);
//        Assert.assertNotNull(parmSet.getAppArgs());
//        Assert.assertEquals(parmSet.getAppArgs().size(), 2);
//    }
//    
//    @Test(enabled=false)
//    public void parseTest2() throws TapisException
//    {
//        // Test minimal input.
//        String json = getInputTest2();
//        parse(wrapForParsing(json));
//        JobParameterSet parmSet = _gson.fromJson(json, JobParameterSet.class);
//        Assert.assertNotNull(parmSet.getAppArgs());
//        Assert.assertEquals(parmSet.getAppArgs().size(), 2);
//        Assert.assertNotNull(parmSet.getContainerArgs());
//    }
//    
//    @Test(enabled=false)
//    public void parseTest3() throws TapisException
//    {
//        // Test minimal input.
//        String json = getInputTest3();
//        parse(wrapForParsing(json));
//        JobParameterSet parmSet = _gson.fromJson(json, JobParameterSet.class);
//        Assert.assertNotNull(parmSet.getAppArgs());
//        Assert.assertEquals(parmSet.getAppArgs().size(), 2);
//        Assert.assertNotNull(parmSet.getContainerArgs());
//        Assert.assertEquals(parmSet.getContainerArgs().size(), 2);
//        Assert.assertEquals(parmSet.getContainerArgs().get(1).getMeta().getKv().get(0).getKey(), "k1");
//        Assert.assertEquals(parmSet.getSchedulerOptions().size(), 2);
//        Assert.assertEquals(parmSet.getSchedulerOptions().get(1).getMeta().getKv().get(1).getValue(), "");
//        Assert.assertEquals(parmSet.getEnvVariables().size(), 2);
//        Assert.assertEquals(parmSet.getEnvVariables().get(1).getValue(), "");
//        Assert.assertNotNull(parmSet.getArchiveFilter());
//        Assert.assertEquals(parmSet.getArchiveFilter().getIncludes().get(1), "tapis*.log");
//    }
//    
//    @Test(enabled=false)
//    public void parseTest4() throws TapisException
//    {
//        // Test minimal input.
//        String json = getInputTest3();
//        parse(wrapForParsing(json));
//        JobParameterSet parmSet = _gson.fromJson(json, JobParameterSet.class);
//        
//        // Convert back to json and then to java again.
//        String json2 = getGsonIgnoreNulls(true).toJson(parmSet);
//        parse(wrapForParsing(json2));
//        JobParameterSet parmSet2 = _gson.fromJson(json2, JobParameterSet.class);
//        
//        // Now inspect the new java object.
//        Assert.assertNotNull(parmSet2.getAppArgs());
//        Assert.assertEquals(parmSet2.getAppArgs().size(), 2);
//        Assert.assertNotNull(parmSet2.getContainerArgs());
//        Assert.assertEquals(parmSet2.getContainerArgs().size(), 2);
//        Assert.assertEquals(parmSet2.getContainerArgs().get(1).getMeta().getKv().get(0).getKey(), "k1");
//        Assert.assertEquals(parmSet2.getSchedulerOptions().size(), 2);
//        Assert.assertEquals(parmSet2.getSchedulerOptions().get(1).getMeta().getKv().get(1).getValue(), "");
//        Assert.assertEquals(parmSet2.getEnvVariables().size(), 2);
//        Assert.assertEquals(parmSet2.getEnvVariables().get(1).getValue(), "");
//        Assert.assertNotNull(parmSet2.getArchiveFilter());
//        Assert.assertEquals(parmSet2.getArchiveFilter().getIncludes().get(1), "tapis*.log");
//    }
//    
//    @Test(enabled=false)
//    public void parseTest5() throws TapisException
//    {
//        // Test notification subscriptions.
//        String json = getInputTest5();
//        NotificationSubscription sub = _gson.fromJson(json, NotificationSubscription.class);
//        
//        // Convert back to json and then to java again.
//        String json2 = getGsonIgnoreNulls(true).toJson(sub);
//        NotificationSubscription sub2 = _gson.fromJson(json, NotificationSubscription.class);
//        
//        // Now inspect the new java object.
//        Assert.assertNotNull(sub.getFilter());
//        Assert.assertNotNull(sub2.getFilter());
//        Assert.assertEquals(sub2.getFilter(), sub.getFilter());
//        Assert.assertNotNull(sub.getNotificationMechanisms());
//        Assert.assertNotNull(sub2.getNotificationMechanisms());
//        Assert.assertEquals(sub.getNotificationMechanisms().size(), 2);
//        Assert.assertEquals(sub2.getNotificationMechanisms().size(), 2);
//        Assert.assertEquals(sub2.getNotificationMechanisms().get(0).getMechanism(), 
//                            sub.getNotificationMechanisms().get(0).getMechanism());
//        Assert.assertEquals(sub2.getNotificationMechanisms().get(0).getWebhookURL(), 
//                            sub.getNotificationMechanisms().get(0).getWebhookURL());
//        Assert.assertEquals(sub2.getNotificationMechanisms().get(0).getEmailAddress(), 
//                            sub.getNotificationMechanisms().get(0).getEmailAddress());
//    }
//    
//    /* ********************************************************************** */
//    /*                            Private Methods                             */
//    /* ********************************************************************** */
//    private void parse(String json) throws TapisException
//    {
//        // Create validator specification.
//        JsonValidatorSpec spec = new JsonValidatorSpec(json, FILE_JOB_PARMSET_TEST);
//        
//        // Make sure the json conforms to the expected schema.
//        try {JsonValidator.validate(spec);}
//          catch (TapisJSONException e) {
//            String msg = MsgUtils.getMsg("TAPIS_JSON_VALIDATION_ERROR", e.getMessage());
//            throw new TapisException(msg, e);
//          }
//    }
//    
//    // Wrap the json as a parameterSet json object for validation parsing.
//    private String wrapForParsing(String json) {return "{\"parameterSet\": " + json + "}";}
//    
//    // Create a gson object that does NOT serialize nulls.
//    private Gson getGsonIgnoreNulls(boolean prettyPrint)
//    {
//        GsonBuilder builder = new GsonBuilder().disableHtmlEscaping();
//        if (prettyPrint) builder.setPrettyPrinting();
//        Converters.registerAll(builder);
//        return builder.create();
//    }
//    
//    private String getInputTest1()
//    {
//        String s = "{\"appArgs\": [{\"arg\": \"x\"}, {\"arg\": \"-f y.txt\"}] "
//                   + "}"; 
//        return s;
//    }
//    
//    private String getInputTest2()
//    {
//        String s = "{\"appArgs\": [{\"arg\": \"x\"}, {\"arg\": \"-f y.txt\"}], "
//                   + "\"containerArgs\": []}";
//        return s;
//    }
//
//    private String getInputTest3()
//    {
//        // Note how the kv array is allowed to specify the same key (k1) twice
//        // as long as the values are different.  Code higher in the application 
//        // stack will deal with key duplication.
//        String s = "{\"appArgs\": [{\"arg\": \"x\"}, {\"arg\": \"-f y.txt\"}], "
//                   + "\"containerArgs\": [{\"arg\": \"?\"}, "
//                   + "                    {\"arg\": \"-23\", \"meta\": {\"name\": \"num\", \"required\": true, "
//                   + "                       \"kv\": [{\"key\": \"k1\", \"value\": \"val1\"}, "
//                   + "                                {\"key\": \"k1\", \"value\": \"val2\"}]"
//                   + "                    }} ], "
//                   + "\"schedulerOptions\": [{\"arg\": \"?\"}, "
//                   + "                    {\"arg\": \"xx\", \"meta\": {\"name\": \"num\", \"required\": false, "
//                   + "                       \"kv\": [{\"key\": \"k3\", \"value\": \"val1\"}, "
//                   + "                                {\"key\": \"k4\", \"value\": \"\"}]"
//                   + "                    }} ], "
//                   + "\"envVariables\": [{\"key\": \"k5\", \"value\": \"val1\"}, "
//                   + "                   {\"key\": \"k6\", \"value\": \"\"}], "
//                   + "\"archiveFilter\": {\"includes\": [\"*.txt\", \"tapis*.log\"], \"excludes\": [\"junk*\"]}"
//                   + "}";
//        return s;
//    }
//
//    private String getInputTest5()
//    {
//        String s = "{"
//                   + "\"filter\": \"tag = job123\", "
//                   + "\"notificationMechanisms\": [{\"mechanism\": \"WEBHOOK\", \"webhookURL\": \"https://bozolives.com\"}, "
//                   +                              "{\"mechanism\": \"EMAIL\", \"emailAddress\": \"bozo@bozolives.com\"}]"
//                   + "}";
//        return s;        
//    }
//    
}
