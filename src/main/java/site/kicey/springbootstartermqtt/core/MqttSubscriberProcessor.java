package site.kicey.springbootstartermqtt.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import site.kicey.springbootstartermqtt.MqttProperty;
import site.kicey.springbootstartermqtt.MqttSubscriber;
import site.kicey.springbootstartermqtt.MqttTopic;
import site.kicey.springbootstartermqtt.util.AnnotationParser;

import java.lang.reflect.Method;

/**
 * @author Kicey
 */
@Component
public class MqttSubscriberProcessor implements BeanFactoryPostProcessor, EnvironmentAware {

  private static final Log log = LogFactory.getLog(MqttSubscriberProcessor.class);

  private Environment env;

  @Override
  public void setEnvironment(final Environment env) {
    this.env = env;
  }

  /**
   * Modify the application context's internal bean factory after its standard initialization. All
   * bean definitions will have been loaded, but no beans will have been instantiated yet. This
   * allows for overriding or adding properties even to eager-initializing beans.
   *
   * @param beanFactory the bean factory used by the application context
   * @throws BeansException in case of errors
   */
  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    log.debug("Start to process annotation @MqttSubscriber");

    String[] names = beanFactory.getBeanNamesForAnnotation(MqttSubscriber.class);
    if (names.length == 0) {
      log.debug("No bean with annotation @MqttSubscriber found, processing return.");
      return;
    }

    // try to obtain mqtt client factory
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

    // process every bean with annotation @MqttSubscriber
    for (String name : names) {
      BeanDefinition definition = beanFactory.getBeanDefinition(name);
      String className = definition.getBeanClassName();
      Class<?> clz;
      try {
        clz = Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(
            "can't get Class: " + className + "annotated by @MqttSubscriber");
      }
      MqttSubscriber mqttSubscriber = clz.getAnnotation(MqttSubscriber.class);
      assert mqttSubscriber != null;
      final String clientId;
      if (mqttSubscriber.clientId() == null || mqttSubscriber.clientId().equals("")) {
        clientId = StringUtils.uncapitalize(clz.getSimpleName());
      } else {
        clientId = mqttSubscriber.clientId();
      }

      // create an adapter for every MqttSubscriber
      PublishSubscribeChannel mqttToMessageChannel = new PublishSubscribeChannel();

      // name of the raw mqtt message input channel
      final String clientInputChannelName = clientId + ".inputChannel";
      beanFactory.registerSingleton(clientInputChannelName, mqttToMessageChannel);

      // get all topics related to the client to create an adapter
      String[] clientTopics = AnnotationParser.getTopics(clz);
      MqttPahoMessageDrivenChannelAdapter adapter =
          new MqttPahoMessageDrivenChannelAdapter(
              clientId, mqttPahoClientFactory, clientTopics);

      adapter.setBeanFactory(beanFactory);
      adapter.setCompletionTimeout(5000);
      adapter.setOutputChannel(mqttToMessageChannel);
      adapter.setConverter(new DefaultPahoMessageConverter());
      adapter.setQos(1);

      if (log.isDebugEnabled()) {
        adapter.setShouldTrack(true);
      }
      beanFactory.registerSingleton(clientId + ".inputAdapter", adapter);

      // create a router for every method annotated by MqttTopic
      Method[] mqttTopicMethods = AnnotationParser.getMqttTopicMethods(clz);
      HeaderValueRouter router = null;
      if (mqttTopicMethods.length > 0) {
        router = new HeaderValueRouter(MqttHeaders.RECEIVED_TOPIC);
        router.setBeanFactory(beanFactory);
        mqttToMessageChannel.subscribe(router);
        String routerBeanName = clientId + ".router";
        beanFactory.registerSingleton(routerBeanName, router);
      }
      for (Method method : mqttTopicMethods) {
        MqttTopic mqttTopic = method.getAnnotation(MqttTopic.class);

        String methodInputChannelName = mqttTopic.inputChannel();
        MessageChannel methodInputChannel = new DirectChannel();
        beanFactory.registerSingleton(methodInputChannelName, methodInputChannel);

        // route all related message to the method related channel
        for (String topic : mqttTopic.topics()) {
          router.setChannelMapping(topic, methodInputChannelName);
        }
      }
    }
  }
}
