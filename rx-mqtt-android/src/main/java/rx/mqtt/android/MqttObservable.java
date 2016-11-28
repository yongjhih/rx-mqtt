package rx.mqtt.android;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by andrew on 11/28/16.
 */

public class MqttObservable {
    public static Observable<MqttMessage> message(final MqttAndroidClient client, final String topic) {
        return Observable.create(new ObservableOnSubscribe<MqttMessage>() {
            @Override
            public void subscribe(final ObservableEmitter<MqttMessage> emitter) throws Exception {
                client.subscribe(topic, 0, new IMqttMessageListener() {
                    @Override
                    public void messageArrived(String topic2, MqttMessage message) throws Exception {
                        emitter.onNext(message);
                    }
                });
            }
        });
    }

    public static Observable<IMqttToken> connect(final MqttAndroidClient client) {
        final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        return Observable.create(new ObservableOnSubscribe<IMqttToken>() {
            @Override
            public void subscribe(final ObservableEmitter<IMqttToken> emitter) throws Exception {
                client.connect(mqttConnectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken token) {
                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        client.setBufferOpts(disconnectedBufferOptions);
                        emitter.onNext(token);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                        emitter.onError(e);
                    }
                });
            }
        });
    }
}
