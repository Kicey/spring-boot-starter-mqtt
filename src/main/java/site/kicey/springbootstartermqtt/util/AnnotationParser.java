package site.kicey.springbootstartermqtt.util;

import site.kicey.springbootstartermqtt.MqttPublisher;
import site.kicey.springbootstartermqtt.MqttSubscriber;
import site.kicey.springbootstartermqtt.MqttTopic;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author Kicey
 */
public class AnnotationParser {
    public static Method[] getMqttTopicMethods(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(MqttSubscriber.class) && !clazz.isAnnotationPresent(MqttPublisher.class)) {
            return new Method[0];
        }
        return Arrays.stream(clazz.getMethods())
                .filter(method -> method.isAnnotationPresent(MqttTopic.class))
                .toArray(Method[]::new);
    }

    public static String[] getTopics(Class<?> clazz) {
        Method[] methods = getMqttTopicMethods(clazz);
        String[] topics = Arrays.stream(methods).flatMap(method -> {
            MqttTopic mqttTopic = method.getAnnotation(MqttTopic.class);
            return Arrays.stream(mqttTopic.topics());
        }).toArray(String[]::new);
        return topics;
    }
}
