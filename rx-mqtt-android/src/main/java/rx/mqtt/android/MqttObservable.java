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
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * Created by andrew on 11/28/16.
 */

public class MqttObservable {
    public static Observable<MqttMessage> message(final MqttAndroidClient client, final String topic) {
        final Observable<MqttMessage> msgObs = Observable.create(new ObservableOnSubscribe<MqttMessage>() {
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

        if (client.isConnected()) {
            return msgObs;
        } else {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            return connect(client, options).flatMap(new Function<IMqttToken, ObservableSource<MqttMessage>>() {
                //return connect(client).flatMap(new Function<IMqttToken, ObservableSource<MqttMessage>>() {
                @Override
                public ObservableSource<MqttMessage> apply(IMqttToken token) throws Exception {
                    return msgObs;
                }
            });
        }
    }

    public static Observable<IMqttToken> connect(final MqttAndroidClient client) {
        return connect(client, null, null);
    }

    public static Observable<IMqttToken> connect(final MqttAndroidClient client, final MqttConnectOptions options) {
        final DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(100);
        bufferOptions.setPersistBuffer(false);
        bufferOptions.setDeleteOldestMessages(false);
        return connect(client, options, bufferOptions);
    }

    public static Observable<IMqttToken> connect(final MqttAndroidClient client, final MqttConnectOptions options, final DisconnectedBufferOptions bufferOption) {
        return Observable.create(new ObservableOnSubscribe<IMqttToken>() {
            @Override
            public void subscribe(final ObservableEmitter<IMqttToken> emitter) throws Exception {
                client.connect(options, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken token) {
                        if (bufferOption != null) client.setBufferOpts(bufferOption);
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
