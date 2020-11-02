package edu.utexas.tacc.tapis.shared.notifications;

import com.rabbitmq.client.Delivery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

public interface ITapisNotificationsClient {

    Mono<Void> sendUserNotificationAsync(Notification note);
    Mono<Void> sendNotificationAsync(String routingKey, Notification note);
    void sendNotification(String routingKey, Notification note) throws IOException;
    Flux<Notification> streamNotifications(String bindingKey);
}
