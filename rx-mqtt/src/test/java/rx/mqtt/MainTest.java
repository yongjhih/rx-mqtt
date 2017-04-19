package rx.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;

import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MainTest {

    @Test
    public void testConnect() throws InterruptedException, MqttException {
        RxMqtt.connect(RxMqtt.client("tcp://test.mosquitto.org:1883"))
                .doOnSuccess(new Consumer<IMqttToken>() {
            @Override
            public void accept(IMqttToken token) throws Exception {
                System.out.println(token);
            }
        }).subscribeOn(Schedulers.io()).test().awaitDone(5, SECONDS).assertNoErrors();
    }

    // docker run -it yongjhih/mosquitto mosquitto_sub -h test.mosquitto.org -t "#" -v
    @Test
    public void testMsg() throws MqttException {
        RxMqtt.message(RxMqtt.client("tcp://test.mosquitto.org:1883"), "#")
          .doOnNext(new Consumer<MqttMessage>() {
            @Override
            public void accept(MqttMessage msg) throws Exception {
                System.out.println("accept msg");
                System.out.println(new String(msg.getPayload()));
            }
       }).subscribeOn(Schedulers.io()).take(2).test().awaitDone(60, SECONDS).assertValueCount(2);
    }
}
