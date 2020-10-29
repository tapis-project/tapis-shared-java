package edu.utexas.tacc.tapis.shared.notifications;

import reactor.core.publisher.Flux;

import java.io.IOException;

public interface ITapisNotificationsClient {

    void sendNotification(String routingKey, Notification note) throws IOException;
    Flux<Notification> streamNotifications(String bindingKey);
}
