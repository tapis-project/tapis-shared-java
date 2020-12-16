package edu.utexas.tacc.tapis.shared.model;

import java.util.List;

public class NotificationSubscription 
{
    private String filter;
    private List<NotificationMechanism> notificationMechanisms;
    
    
    public String getFilter() {
        return filter;
    }
    public void setFilter(String filter) {
        this.filter = filter;
    }
    public List<NotificationMechanism> getNotificationMechanisms() {
        return notificationMechanisms;
    }
    public void setNotificationMechanisms(List<NotificationMechanism> notificationMechanisms) {
        this.notificationMechanisms = notificationMechanisms;
    }
}
