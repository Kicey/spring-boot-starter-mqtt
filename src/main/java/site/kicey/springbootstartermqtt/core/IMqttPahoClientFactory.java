package site.kicey.springbootstartermqtt.core;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.integration.mqtt.core.ConsumerStopAction;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kicey
 */
public class IMqttPahoClientFactory implements MqttPahoClientFactory {

  private MqttConnectOptions options = new MqttConnectOptions();
  private final Map<String, IMqttClient> mqttClients = new ConcurrentHashMap<>();
  private final Map<String, IMqttAsyncClient> asyncMqttClients = new ConcurrentHashMap<>();
  private MqttClientPersistence persistence;
  private final ConsumerStopAction consumerStopAction = ConsumerStopAction.UNSUBSCRIBE_CLEAN;

  static IMqttPahoClientFactory from(String uri, String username, String password) {
    IMqttPahoClientFactory clientFactory = new IMqttPahoClientFactory();

    MqttConnectOptions connectOptions = new MqttConnectOptions();
    connectOptions.setServerURIs(new String[] {uri});
    connectOptions.setUserName(username);
    connectOptions.setPassword(Objects.requireNonNull(password).toCharArray());

    clientFactory.setConnectionOptions(connectOptions);
    return clientFactory;
  }

  /**
   * Retrieve a client instance.
   *
   * @param url The URL.
   * @param clientId The client id.
   * @return The client instance.
   * @throws MqttException Any.
   */
  @Override
  public IMqttClient getClientInstance(String url, String clientId) throws MqttException {
    Assert.doesNotContain(url, "\n", "The URL must not contain a new line character");
    Assert.doesNotContain(clientId, "\n", "The client id must not contain a new line character");
    clientId = clientId + ":sync";
    String clientKey = url + "\n" + clientId;
    if (!mqttClients.containsKey(clientKey)) {
      synchronized (this) {
        if (!mqttClients.containsKey(clientKey)) {
          MqttClient created =
              new MqttClient(
                  url == null ? "tcp://NO_URL_PROVIDED" : url, clientId, this.persistence);
          mqttClients.put(clientKey, created);
        }
      }
    }
    return mqttClients.get(clientKey);
  }

  /**
   * Retrieve an async client instance.
   *
   * @param url The URL.
   * @param clientId The client id.
   * @return The client instance.
   * @throws MqttException Any.
   * @since 4.1
   */
  @Override
  public IMqttAsyncClient getAsyncClientInstance(String url, String clientId) throws MqttException {
    Assert.doesNotContain(url, "\n", "The URL must not contain a new line character");
    Assert.doesNotContain(clientId, "\n", "The client id must not contain a new line character");
    clientId = clientId + ":async";
    String clientKey = url + "\n" + clientId;
    if (!asyncMqttClients.containsKey(clientKey)) {
      synchronized (this) {
        if (!asyncMqttClients.containsKey(clientKey)) {
          MqttAsyncClient created =
              new MqttAsyncClient(
                  url == null ? "tcp://NO_URL_PROVIDED" : url, clientId, this.persistence);
          asyncMqttClients.put(clientKey, created);
        }
      }
    }
    return asyncMqttClients.get(clientKey);
  }

  public void setConnectionOptions(MqttConnectOptions options) {
    Assert.notNull(options, "MqttConnectOptions cannot be null");
    this.options = options;
  }

  /**
   * Retrieve the connection options.
   *
   * @return The options.
   */
  @Override
  public MqttConnectOptions getConnectionOptions() {
    return options;
  }

  /**
   * Get the consumer stop action.
   *
   * @return the consumer stop action.
   * @since 4.3
   */
  @Override
  public ConsumerStopAction getConsumerStopAction() {
    return consumerStopAction;
  }
}
