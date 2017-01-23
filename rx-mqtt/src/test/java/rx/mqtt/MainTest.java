package rx.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;

import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MainTest {

    @Test
    public void testConnect() throws InterruptedException {
        MqttObservable.client("tcp://test.mosquitto.org:1883").flatMap(new Function<IMqttAsyncClient, ObservableSource<IMqttToken>>() {
            @Override
            public ObservableSource<IMqttToken> apply(final IMqttAsyncClient client) throws Exception {
                final MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                return MqttObservable.connect(client, options);
            }
        }).doOnNext(new Consumer<IMqttToken>() {
            @Override
            public void accept(IMqttToken token) throws Exception {
                System.out.println(token);
            }
        }).test().awaitDone(3, SECONDS).assertNoErrors();
    }

    // docker run -it yongjhih/mosquitto mosquitto_sub -h test.mosquitto.org -t "#" -v
    @Test
    public void testMsg() {
        MqttObservable.client("tcp://test.mosquitto.org:1883").flatMap(new Function<IMqttAsyncClient, ObservableSource<MqttMessage>>() {
            @Override
            public ObservableSource<MqttMessage> apply(final IMqttAsyncClient client) throws Exception {
                System.out.println("apply client");
                return MqttObservable.message(client, "#");
            }
        }).doOnNext(new Consumer<MqttMessage>() {
            @Override
            public void accept(MqttMessage msg) throws Exception {
                System.out.println("accept msg");
                System.out.println(new String(msg.getPayload()));
            }
       }).take(10).test().awaitDone(10, SECONDS).assertValueCount(10);
    }
}
