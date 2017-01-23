[![JitPack](https://img.shields.io/github/tag/yongjhih/rx-mqtt.svg?label=JitPack)](https://jitpack.io/#yongjhih/rx-mqtt)
[![javadoc](https://img.shields.io/github/tag/yongjhih/rx-mqtt.svg?label=javadoc)](https://jitpack.io/com/github/yongjhih/rx-mqtt/rx-mqtt/-SNAPSHOT/javadoc/)
[![Build Status](https://travis-ci.org/yongjhih/rx-mqtt.svg)](https://travis-ci.org/yongjhih/rx-mqtt)
[![Coverage Status](https://coveralls.io/repos/github/yongjhih/rx-mqtt/badge.svg?branch=master)](https://coveralls.io/github/yongjhih/rx-mqtt?branch=master)

# rx-mqtt

## Usage

For pure Java:

```java
MqttObservable.client("tcp://test.mosquitto.org:1883")
  .flatMap(client -> MqttObservable.message(client, "#"))
  .subscribe(System.out::println);
```

For Android:

```java
MqttObservable.message(new MqttAndroidClient(context, "tcp://test.mosquitto.org:1883", "rxmqtt"), "#")
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
    //compile 'com.github.yongjhih.rx-mqtt:rx-mqtt:-SNAPSHOT'
    compile 'com.github.yongjhih.rx-mqtt:rx-mqtt-android:-SNAPSHOT'
}
```

## LICENSE

```
Copyright 2016 8tory, Inc.

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
