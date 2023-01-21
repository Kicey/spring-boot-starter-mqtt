package site.kicey.springbootstartermqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Kicey
 */
@ConfigurationProperties(prefix = "mqtt")
@Component
public class MqttProperty {

  public static final String MqttClientFactoryName = "mqttPahoClientFactory";
  public String uri;
  public String username;
  public String password;

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
