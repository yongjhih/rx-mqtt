[![JitPack](https://img.shields.io/github/tag/yongjhih/rx-mqtt.svg?label=JitPack)](https://jitpack.io/#yongjhih/rx-mqtt)
[![javadoc](https://img.shields.io/github/tag/yongjhih/rx-mqtt.svg?label=javadoc)](https://jitpack.io/com/github/yongjhih/rx-mqtt/rx-mqtt/-SNAPSHOT/javadoc/)
[![Build Status](https://travis-ci.org/yongjhih/rx-mqtt.svg)](https://travis-ci.org/yongjhih/rx-mqtt)
[![codecov](https://codecov.io/gh/yongjhih/rx-mqtt/branch/master/graph/badge.svg)](https://codecov.io/gh/yongjhih/rx-mqtt)

# rx-mqtt

## Usage

Before:

```java
final MqttAsyncClient client = new MqttAsyncClient(url, id, persistence)

client.setCallback(new MqttCallback() { // message
    @Override
    public void connectionLost(@Nullable final Throwable e) {
        if (e != null) {
            e.printStackTrace();
        } else {
            (new RuntimeException("Connection Lost")).printStackTrace();
        }
    }

    @Override
    public void messageArrived(@NonNull final String topic,
                               @NonNull final MqttMessage message)
            throws Exception {
         Gson.parse(message.getPayload(), Telemetry.class)
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // nothing
    }
});

MqttConnectOptions options = new MqttConnectOptions();
options.setCleanSession(true);
client.connect(options, null, new IMqttActionListener() {
    @Override
    public void onSuccess(@NonNull final IMqttToken token) {
        client.subscribe(topic, 0, null, new IMqttActionListener() {
            @Override
            public void onSuccess(@NonNull IMqttToken token) {
                // nothing
            }

            @Override
            public void onFailure(@NonNull final IMqttToken token,
                                  @Nullable final Throwable e) {
                if (e != null) {
                    e.printStackTrace();
                } else {
                    (new RuntimeException("Connection Lost")).printStackTrace();
                }
            }
        }, new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message)
                    throws Exception {
                // nothing
            }
        });
    }

    @Override
    public void onFailure(@NonNull final IMqttToken token,
                          @Nullable final Throwable e) {
        if (e != null) {
            e.printStackTrace();
        } else {
            (new RuntimeException("Connection Lost")).printStackTrace();
        }
    }
});
```

After:

For pure Java:

```java
  RxMqtt.message(RxMqtt.client("tcp://test.mosquitto.org:1883"), "#"))
  .subscribe(System.out::println);
```

For Android:

```java
RxMqtt.message(RxMqtt.client(context, "tcp://test.mosquitto.org:1883"), "#")
  .subscribe(System.out::println);
```

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <service android:name="org.eclipse.paho.android.service.MqttService" />
```

## Installation

via jitpack.io

```gradle
repositories {
    jcenter()
    maven { url "https://jitpack.io" }
}

dependencies {
    //compile 'com.github.yongjhih.rx-mqtt:rx-mqtt:-SNAPSHOT' // for pure java
    compile 'com.github.yongjhih.rx-mqtt:rx-mqtt-android:-SNAPSHOT'
}
```

## LICENSE

```
Copyright 2016 Andrew Chen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
