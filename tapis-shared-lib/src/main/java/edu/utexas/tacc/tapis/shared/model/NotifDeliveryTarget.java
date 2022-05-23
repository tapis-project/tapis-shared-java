package edu.utexas.tacc.tapis.shared.model;

public class NotifDeliveryTarget 
{
    // Not all types may be implemented.
    public enum DeliveryMethod {WEBHOOK, EMAIL, QUEUE, ACTOR}
    
    // Delivery coordinates specifying where notifications are sent.
    private DeliveryMethod deliveryMethod;
    private String         deliveryAddress;
    
    // Constructors.
    public NotifDeliveryTarget() {}
    public NotifDeliveryTarget(edu.utexas.tacc.tapis.apps.client.gen.model.NotifDeliveryTarget appTarget)
    {
        deliveryMethod  = DeliveryMethod.valueOf(appTarget.getDeliveryMethod().name());
        deliveryAddress = appTarget.getDeliveryAddress();
    }
    
    // Accessors.
    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }
    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
    public String getDeliveryAddress() {
        return deliveryAddress;
    }
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
}
