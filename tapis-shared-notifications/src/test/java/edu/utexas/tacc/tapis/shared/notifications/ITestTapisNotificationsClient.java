package edu.utexas.tacc.tapis.shared.notifications;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import reactor.test.StepVerifier;

@Test(groups = {"integration"})
public class ITestTapisNotificationsClient {

    private TapisNotificationsClient client;

    @BeforeClass
    void init() {
        client = new TapisNotificationsClient();
    }

    @Test
    void testReceivesMessages() throws Exception {

        Notification notification1 = new Notification.Builder()
            .setBody("testBody1")
            .setCreator("testService")
            .setLevel("INFO")
            .setRecipient("testUser")
            .setEventType("TEST_EVENT_TYPE")
            .setTenant("testTenant")
            .build();

        client.sendNotification("testService.EVENTS", notification1);
    }


}
