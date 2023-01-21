package site.kicey.springbootstartermqtt.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import site.kicey.springbootstartermqtt.MqttPublisher;
import site.kicey.springbootstartermqtt.MqttSubscriber;
import site.kicey.springbootstartermqtt.MqttTopic;

/**
 * Util which get methods and corresponding topic annotated with {@link MqttTopic} from class
 * annotated with {@link MqttTopic} or {@link MqttPublisher}.
 *
 * @author Kicey
 */
public class AnnotationParser {

  /**
   * Get methods annotated with {@link MqttTopic} from class annotated with {@link MqttTopic} or
   * {@link MqttPublisher}.
   *
   * @param clazz Class to be parsed
   * @return Corresponding methods
   */
  public static Method[] getMqttTopicMethods(Class<?> clazz) {
    if (!clazz.isAnnotationPresent(MqttSubscriber.class)
        && !clazz.isAnnotationPresent(MqttPublisher.class)) {
      return new Method[0];
    }
    return Arrays.stream(clazz.getMethods())
        .filter(method -> method.isAnnotationPresent(MqttTopic.class))
        .toArray(Method[]::new);
  }

  /**
   * Get topic from method annotated with {@link MqttTopic}.
   *
   * @param clazz Class to be parsed
   * @return Corresponding topic
   */
  public static String[] getTopics(Class<?> clazz) {
    Method[] methods = getMqttTopicMethods(clazz);
    String[] topics =
        Arrays.stream(methods)
            .flatMap(
                method -> {
                  MqttTopic mqttTopic = method.getAnnotation(MqttTopic.class);
                  return Arrays.stream(mqttTopic.topics());
                })
            .toArray(String[]::new);
    return topics;
  }
}
