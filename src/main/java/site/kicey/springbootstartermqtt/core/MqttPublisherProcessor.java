package site.kicey.springbootstartermqtt.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import site.kicey.springbootstartermqtt.MqttProperty;
import site.kicey.springbootstartermqtt.MqttPublisher;
import site.kicey.springbootstartermqtt.MqttTopic;
import site.kicey.springbootstartermqtt.util.AnnotationParser;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kicey
 */
@Component
public class MqttPublisherProcessor implements BeanFactoryPostProcessor, EnvironmentAware {

  private static final Log log = LogFactory.getLog(MqttSubscriberProcessor.class);

  private Environment env;

  @Override
  public void setEnvironment(final Environment env) {
    this.env = env;
  }

  /**
   * @param beanFactory the bean factory used by the application context
   * @throws BeansException in case of errors
   */
  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    log.debug("Start to process MqttPublisher");
    String[] names = beanFactory.getBeanNamesForAnnotation(MqttPublisher.class);
    if (names.length == 0) {
      log.debug("No MqttPublisher found.");
      return;
    }
    MqttPahoClientFactory mqttPahoClientFactory;
    try {
      mqttPahoClientFactory = beanFactory.getBean(MqttPahoClientFactory.class);
    } catch (BeansException e) {
      log.debug(
          "Can't find bean MqttPahoClientFactory, try to use site.kicey.springbootstartermqtt.core.IMqttPahoClientFactory");
      mqttPahoClientFactory =
          IMqttPahoClientFactory.from(
              env.getProperty("mqtt.uri"),
              env.getProperty("mqtt.username"),
              env.getProperty("mqtt.password"));
      beanFactory.registerSingleton(MqttProperty.MqttClientFactoryName, mqttPahoClientFactory);
    }

    for (String name : names) {
      MqttPublisher mqttPublisher = beanFactory.findAnnotationOnBean(name, MqttPublisher.class);
      assert mqttPublisher != null;
      final String clientId;

      Class<?> clz;
      try {
        clz = beanFactory.getType(name);
      } catch (NoSuchBeanDefinitionException e) {
        throw new RuntimeException("can't find class for bean " + name);
      }
      assert clz != null;

      if (mqttPublisher.clientId() == null || mqttPublisher.clientId().equals("")) {
        clientId = StringUtils.uncapitalize(clz.getSimpleName());
        log.debug("Use the bean name: " + clientId + " as the client id.");
      } else {
        clientId = mqttPublisher.clientId();
      }

      final String channelName = clientId + ".output.channel";
      DirectChannel mqttOutputChannel = new DirectChannel();

      beanFactory.registerSingleton(channelName, mqttOutputChannel);

      final String handlerName = clientId + ".output.handler";
      MqttPahoMessageHandler messageSender =
          new MqttPahoMessageHandler(clientId, mqttPahoClientFactory);
      messageSender.setAsync(true);
      messageSender.setDefaultQos(1);
      messageSender.setConverter(new DefaultPahoMessageConverter());

      beanFactory.registerSingleton(handlerName, messageSender);

      mqttOutputChannel.subscribe(messageSender);

      Method[] mqttTopicMethods = AnnotationParser.getMqttTopicMethods(clz);
      for (Method method : mqttTopicMethods) {
        MqttTopic mqttTopic = method.getAnnotation(MqttTopic.class);
        if (mqttTopic.topics() == null || mqttTopic.topics().length != 1) {
          throw new RuntimeException(
              "@MqttTopic's topics must equal to 1 when used with @MqttPublisher");
        }
        final String methodDirectChannelName = mqttTopic.requestChannel();
        DirectChannel methodDirectChannel = new DirectChannel();
        beanFactory.registerSingleton(methodDirectChannelName, methodDirectChannel);

        final String headerEnricherName = clientId + "." + method.getName() + ".header.enricher";
        MessageTransformingHandler headerEnricher =
            new MessageTransformingHandler(
                message -> {
                  Map<String, Object> headersMap = new HashMap<>(message.getHeaders());
                  headersMap.put(MqttHeaders.TOPIC, mqttTopic.topics()[0]);
                  GenericMessage<?> messageWithHeader =
                      new GenericMessage<>(message.getPayload(), headersMap);
                  log.debug(
                      "HeaderEnricher for " + mqttTopic.requestChannel() + " receive message.");
                  log.debug(messageWithHeader.getPayload().toString());
                  return messageWithHeader;
                });
        methodDirectChannel.subscribe(headerEnricher);
        headerEnricher.setOutputChannel(mqttOutputChannel);
        beanFactory.registerSingleton(headerEnricherName, headerEnricher);
      }
    }
  }
}
