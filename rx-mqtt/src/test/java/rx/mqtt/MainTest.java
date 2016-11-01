package rx.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;

import io.reactivex.functions.Consumer;
import io.reactivex.observers.TestObserver;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MainTest {


    // docker run -it yongjhih/mosquitto mosquitto_sub -h test.mosquitto.org -t "#" -v
    @Test
    public void testMsg() {
        TestObserver observer = MqttObservable.msg("tcp://test.mosquitto.org:1883", "#", "rxmqtt").doOnNext(new Consumer<MqttMessage>() {
            @Override
            public void accept(MqttMessage msg) throws Exception {
                System.out.println(msg);
            }
        }).test();
        try {
            observer.await(3, SECONDS);
        } catch (InterruptedException e) {
            fail(e.toString());
            e.printStackTrace();
        }
        observer.assertNoErrors();
        //assertTrue(true);
    }

    @Test
    public void testConnect() {
        TestObserver observer = MqttObservable.connect("tcp://test.mosquitto.org:1883", "rxmqtt").doOnNext(new Consumer<IMqttDeliveryToken>() {
            @Override
            public void accept(IMqttDeliveryToken token) throws Exception {
                System.out.println(token);
            }
        }).test();
        observer.awaitTerminalEvent();
        observer.assertTerminated();
    }
}
