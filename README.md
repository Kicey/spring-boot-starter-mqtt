This is a spring boot starter for Mqtt back end development. It depends on spring-integration-mqtt which is based on
Paho MQTT.

It allows you to create MessageEndpoint produce or consume Mqtt message. It's not completed, I'm going to complete it in
my idle time. Pull Request is welcome.

The mqtt version is supported now is 3.1.1 .

There is a sample about how to use the starter: [starter-mqtt-sample](https://github.com/Kicey/starter-mqtt-sample).

Features to be supported:

- [ ] implicit client id, channel name
- [ ] combine two client that used by publisher and subscriber
- [ ] message payload convert
- [ ] qos support
