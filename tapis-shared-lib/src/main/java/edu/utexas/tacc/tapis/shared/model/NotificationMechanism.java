package edu.utexas.tacc.tapis.shared.model;

public class NotificationMechanism 
{
    public enum Delivery {WEBHOOK, EMAIL, QUEUE, ACTOR}
    
    private Delivery mechanism;
    private String   webhookURL;
    private String   emailAddress;
    
    // Constructors.
    public NotificationMechanism() {}
    public NotificationMechanism(edu.utexas.tacc.tapis.apps.client.gen.model.NotificationMechanism appMech)
    {
        mechanism = Delivery.valueOf(appMech.getMechanism().name());
        webhookURL = appMech.getWebhookURL();
        emailAddress = appMech.getEmailAddress();
    }
    
    // Accessors.
    public Delivery getMechanism() {
        return mechanism;
    }
    public void setMechanism(Delivery mechanism) {
        this.mechanism = mechanism;
    }
    public String getWebhookURL() {
        return webhookURL;
    }
    public void setWebhookURL(String webhookURL) {
        this.webhookURL = webhookURL;
    }
    public String getEmailAddress() {
        return emailAddress;
    }
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
