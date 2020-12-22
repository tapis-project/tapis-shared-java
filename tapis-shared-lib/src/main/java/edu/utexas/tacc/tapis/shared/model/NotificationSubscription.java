package edu.utexas.tacc.tapis.shared.model;

import java.util.ArrayList;
import java.util.List;

public class NotificationSubscription 
{
    private String filter;
    private List<NotificationMechanism> notificationMechanisms = new ArrayList<NotificationMechanism>();
    
    // Constructors.
    public NotificationSubscription() {}
    public NotificationSubscription(edu.utexas.tacc.tapis.apps.client.gen.model.NotificationSubscription appSub)
    {
        filter = appSub.getFilter();
        if (appSub.getNotificationMechanisms() != null)
            for (var appMech : appSub.getNotificationMechanisms()) {
                var mech = new NotificationMechanism(appMech);
                notificationMechanisms.add(mech);
            }
    }
    
    // Accessors.
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
