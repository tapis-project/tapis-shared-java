package edu.utexas.tacc.tapis.shared.notifications;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class Notification implements INotification {

    private final String tenant;
    private final Instant created;
    private final String recipient;
    private final String creator;
    private final String body;
    private final String level;
    private final String eventType; //FILE_TRANSFER_PROGRESS
    private final NotificationMechanism notificationMechanism;


    private Notification(Builder builder) {
        this.tenant = builder.tenant;
        this.body = builder.body;
        this.recipient = builder.recipient;
        this.creator = builder.creator;
        this.level = builder.level;
        this.eventType = builder.eventType;
        this.notificationMechanism = builder.mechanism;
        this.created = Instant.now();
    }


    public static class Builder {
        private String tenant;
        private String creator;
        private String body;
        private String level;
        private String recipient;
        private String eventType;
        private NotificationMechanism mechanism;

        private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        public Builder setTenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public Builder setRecipient(String username) {
            this.recipient = username;
            return this;
        }

        public Builder setCreator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public Builder setLevel(String level) {
            this.level = level;
            return this;
        }


        public Builder setEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder setMechanism(NotificationMechanism mechanism) {
            this.mechanism = mechanism;
            return this;
        }


        public Notification build() {

            Notification notification = new Notification(this);
            Set<ConstraintViolation<Notification>> violations =
                validator.validate(notification);

            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(
                    new HashSet<ConstraintViolation<?>>(violations));
            }

            return notification;
        }
    }

    @Override
    @NotNull
    public String getEventType() {
        return eventType;
    }

    @Override
    @NotNull
    public Instant getCreated() {
        return created;
    }

    @Override
    @NotNull
    public String getRecipient() {
        return recipient;
    }

    @Override
    @NotNull
    public String getCreator() {
        return creator;
    }

    @Override
    @NotNull
    public String getBody() {
        return body;
    }

    @Override
    @NotNull
    public String getLevel() {
        return level;
    }

    @Override
    @NotNull
    public String getTenant() {
        return tenant;
    }

    @Nullable
    @Valid
    public NotificationMechanism getNotificationMechanism(){
        return notificationMechanism;
    }


}
