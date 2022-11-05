package site.kicey.springbootstartermqtt;

import org.springframework.core.annotation.AliasFor;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.ServiceActivator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Kicey
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ServiceActivator
@Gateway
public @interface MqttTopic {

  @AliasFor("value")
  String[] topics() default {};

  @AliasFor("topics")
  String[] value() default {};

  @AliasFor(annotation = ServiceActivator.class, attribute = "inputChannel")
  String inputChannel() default "nullChannel";

  @AliasFor(annotation = Gateway.class, attribute = "requestChannel")
  String requestChannel() default "";
}
