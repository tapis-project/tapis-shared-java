package edu.utexas.tacc.tapis.shared.notifications;

import org.hibernate.validator.constraints.URL;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class NotificationMechanism {
    
    private final String mechanism;
    private final String webhookURL;
    private final String emailAddress;
    
    public NotificationMechanism(Builder builder){
        this.mechanism = builder.mechanism;
        this.emailAddress = builder.emailAddress;
        this.webhookURL = builder.webhookURL;
    }

    public static class Builder {

        private String mechanism;
        private String webhookURL;
        private String emailAddress;

        private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        public Builder setMechanism(String mechanism) {
            this.mechanism = mechanism;
            return this;
        }

        public Builder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public Builder setWebhookURL(String webhookURL) {
            this.webhookURL = webhookURL;
            return this;
        }

        public NotificationMechanism build() {

            NotificationMechanism noteMech = new NotificationMechanism(this);
            Set<ConstraintViolation<NotificationMechanism>> violations =
                validator.validate(noteMech);

            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(
                    new HashSet<ConstraintViolation<?>>(violations));
            }
            return noteMech;
        }
    }


    /**
     * The validation api allows this AssertTrue, they MUST start with `is`
     * @return
     */
    @AssertTrue
    private boolean isValid() {
        if (mechanism.equals("email") && emailAddress == null) return false;
        if (mechanism.equals("webhook") && webhookURL == null) return false;
        return true;
    }

    @NotNull
    public String getMechanism() {
        return this.mechanism;
    }


    @Nullable
    @Email
    public String getEmailAddress() {
        return this.emailAddress;
    }

    @Nullable
    @URL
    public String getWebhookURL() {
        return this.webhookURL;
    }

    @Override
    public String toString() {
        return "NotificationMechanism{" +
            "mechanism='" + mechanism + '\'' +
            ", webhookURL='" + webhookURL + '\'' +
            ", emailAddress='" + emailAddress + '\'' +
            '}';
    }
}
