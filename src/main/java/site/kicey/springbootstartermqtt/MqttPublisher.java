package site.kicey.springbootstartermqtt;

import org.springframework.integration.annotation.MessagingGateway;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Kicey
 * 
 * This annotation will be processed and create according MqttPahoMessageHandler and channel.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MessagingGateway(proxyDefaultMethods = false)
public @interface MqttPublisher {
    String clientId();
}
