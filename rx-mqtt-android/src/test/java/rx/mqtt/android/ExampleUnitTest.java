package rx.mqtt.android;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttService;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ServiceController;

import io.reactivex.functions.Consumer;
import io.reactivex.observers.TestObserver;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
//@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class ExampleUnitTest {
    public static final String url = "tcp://test.mosquitto.org:1883";
    public static final String id = "rxmqtt";
    public static final String topic = "#";

    //@Test
    // MqttAndroidClient cannot retrieve MqttService by intent
    public void testMsg() {
        final MqttAndroidClient client = new MqttAndroidClient(RuntimeEnvironment.application, url, id);
        TestObserver observer = MqttObservable.message(client, topic).doOnNext(new Consumer<MqttMessage>() {
            @Override
            public void accept(MqttMessage msg) throws Exception {
                System.out.println(msg);
            }
        }).test();
        try {
            observer.await(10, SECONDS);
        } catch (InterruptedException e) {
            fail(e.toString());
            e.printStackTrace();
        }
        observer.assertNoErrors();
        //assertTrue(true);
    }

    @Test
    public void testConnect() {
        final MqttAndroidClient client = new MqttAndroidClient(RuntimeEnvironment.application, url, id);
        TestObserver observer = MqttObservable.connect(client).doOnNext(new Consumer<IMqttToken>() {
            @Override
            public void accept(IMqttToken token) throws Exception {
                System.out.println(token);
            }
        }).test();
        observer.awaitTerminalEvent();
        observer.assertTerminated();
    }

    private MqttService mqttService;
    private ServiceController<MqttService> controller;

    @Before
    public void setUp() {
        controller = Robolectric.buildService(MqttService.class);
        mqttService = controller.attach().create().get();
    }

    @After
    public void tearDown() {
        controller.destroy();
    }
}
