package site.kicey.springbootstartermqtt.autoconfigure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import site.kicey.springbootstartermqtt.MqttProperty;

/**
 * @author Kicey
 */
@EnableConfigurationProperties(MqttProperty.class)
@ComponentScan(basePackages = "site.kicey.springbootstartermqtt.core")
@Configuration
public class MqttAutoConfigure {
    
}
