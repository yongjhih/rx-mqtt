package rx.mqtt;


import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * Created by andrew on 11/17/16.
 */

public class MqttObservable {
    public static Observable<IMqttAsyncClient> client(final String url) {
        return client(url, MqttClient.generateClientId());
    }

    public static Observable<IMqttAsyncClient> client(final String url, final String id) {
        return client(url, id, new MemoryPersistence());
    }

    public static Observable<IMqttAsyncClient> client(final String url, final String id, final MqttClientPersistence persistence) {
        try {
            return Observable.just(new MqttAsyncClient(url, id, persistence));
        } catch (MqttException e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }

    public static Observable<IMqttToken> connect(final IMqttAsyncClient client) {
        return connect(client, null);
    }

    public static Observable<IMqttToken> connect(final IMqttAsyncClient client, final MqttConnectOptions options) {
        return Observable.create(new ObservableOnSubscribe<IMqttToken>() {
            @Override
            public void subscribe(ObservableEmitter<IMqttToken> emitter) throws Exception {
                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable e) {
                        emitter.onError(e);
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        // nothing
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        // nothing
                    }
                });

                client.connect(options, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken token) {
                        emitter.onNext(token);
                    }

                    @Override
                    public void onFailure(IMqttToken token, Throwable e) {
                        emitter.onError(e);
                    }
                });
            }
        });
    }

    public static Observable<MqttMessage> message(final IMqttAsyncClient client, final String topic) {
        final Observable<MqttMessage> msgObs = Observable.create(new ObservableOnSubscribe<MqttMessage>() {
            @Override
            public void subscribe(ObservableEmitter<MqttMessage> emitter) throws Exception {
                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable e) {
                        emitter.onError(e); // careful duplicated onError(e), but we think emitter can figure out
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        emitter.onNext(message);
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        // nothing
                    }
                });
                client.subscribe(topic, 0, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken token) {
                        // noting
                    }

                    @Override
                    public void onFailure(IMqttToken token, Throwable e) {
                        emitter.onError(e); // careful duplicated onError(e), but we think emitter can figure out
                    }
                }, new IMqttMessageListener() {
                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        // 69.7 46.7 18660 ?
                        //emitter.onNext(message);
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
}
