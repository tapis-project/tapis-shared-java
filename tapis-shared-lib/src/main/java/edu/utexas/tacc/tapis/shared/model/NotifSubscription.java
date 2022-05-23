package edu.utexas.tacc.tapis.shared.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

public class NotifSubscription 
{
    // Local logger.
    private static final Logger _log = LoggerFactory.getLogger(NotifSubscription.class);
    
    // Basic identity fields.
    private String  name;
    private String  owner;
    private String  description;
    private boolean enabled;
    private String  uuid;
    
    // Search and delivery values.
    private int     ttlMinutes;
    private String  typeFilter;
    private String  subjectFilter;
    private List<NotifDeliveryTarget> deliveryTargets = new ArrayList<NotifDeliveryTarget>();
    
    // Values only assigned internally.
    private Instant expiry;
    private Instant created;
    private Instant updated;
    
    // Constructors.
    public NotifSubscription() {}
    public NotifSubscription(edu.utexas.tacc.tapis.apps.client.gen.model.NotifSubscription appSub)
    {
        name        = appSub.getName();
        owner       = appSub.getOwner();
        description = appSub.getDescription();
        enabled     = appSub.getEnabled() == null ? true : appSub.getEnabled();
        uuid        = appSub.getUuid();  
        
        ttlMinutes    = appSub.getTtlMinutes() == null ? 0 : appSub.getTtlMinutes();
        typeFilter    = appSub.getTypeFilter();
        subjectFilter = appSub.getSubjectFilter();
        if (appSub.getDeliveryTargets() != null)
            for (var appTarget : appSub.getDeliveryTargets()) {
                var target = new NotifDeliveryTarget(appTarget);
                deliveryTargets.add(target);
            }
        
        // These should all be non-null timestamps.
        expiry  = getInstant(appSub.getExpiry());
        created = getInstant(appSub.getCreated());
        updated = getInstant(appSub.getUpdated());
    }
    
    /** Safely convert String to Instant.
     * Return null if cannot convert. 
     * @param timestamp string representation of a java instant
     * @return null or an instant
     */
    private Instant getInstant(String timestamp)
    {
        // Expecting a UTC timestamp.
        try {return Instant.parse(timestamp);} 
            catch (Exception e) {
                // This should not happen!
                _log.error(MsgUtils.getMsg("TAPIS_INVALID_PARAMETER", "getInstant",
                                           "timestamp", timestamp));
                return null;
            } 
    }
    
    // Accessors.
    public String getTypeFilter() {
        return typeFilter;
    }
    public void setTypeFilter(String typeFilter) {
        this.typeFilter = typeFilter;
    }
    public String getSubjectFilter() {
        return subjectFilter;
    }
    public void setSubjectFilter(String subjectFilter) {
        this.subjectFilter = subjectFilter;
    }
    public List<NotifDeliveryTarget> getNotificationMechanisms() {
        return deliveryTargets;
    }
    public void setNotificationMechanisms(List<NotifDeliveryTarget> notificationMechanisms) {
        this.deliveryTargets = notificationMechanisms;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getOwner() {
        return owner;
    }
    public void setOwner(String owner) {
        this.owner = owner;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public int getTtlMinutes() {
        return ttlMinutes;
    }
    public void setTtlMinutes(int ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }
    public Instant getExpiry() {
        return expiry;
    }
    public Instant getCreated() {
        return created;
    }
    public Instant getUpdated() {
        return updated;
    }
    public List<NotifDeliveryTarget> getDeliveryTargets() {
        return deliveryTargets;
    }
    public void setDeliveryTargets(List<NotifDeliveryTarget> deliveryTargets) {
        this.deliveryTargets = deliveryTargets;
    }
    public String getUuid() {
        return uuid;
    }
}
