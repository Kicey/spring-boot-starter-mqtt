package site.kicey.springbootstartermqtt;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Kicey
 *     <p>This annotation is used for class. After processed, according
 *     MqttPahoMessageDrivenChannelAdapter and channel will be created to subscribe the mqtt topic.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface MqttSubscriber {
  @AliasFor(annotation = Component.class, attribute = "value")
  String value() default "";

  String clientId() default "";
}
