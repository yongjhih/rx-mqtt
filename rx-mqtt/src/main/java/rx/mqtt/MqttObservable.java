package rx.mqtt;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
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
    public static Observable<MqttClient> client(final String url) {
        return client(url, MqttClient.generateClientId());
    }

    public static Observable<MqttClient> client(final String url, final String id) {
        return client(url, id, new MemoryPersistence());
    }

    public static Observable<MqttClient> client(final String url, final String id, final MqttClientPersistence persistence) {
        try {
            return Observable.just(new MqttClient(url, id, persistence));
        } catch (MqttException e) {
            e.printStackTrace();
            return Observable.error(e);
        }
    }

    public static Observable<MqttClient> connect(final MqttClient client) {
        return connect(client, null);
    }

    public static Observable<MqttClient> connect(final MqttClient client, final MqttConnectOptions options) {
        return Observable.create(new ObservableOnSubscribe<MqttClient>() {
            @Override
            public void subscribe(ObservableEmitter<MqttClient> emitter) throws Exception {
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
                    }
                });

                if (options == null) {
                    client.connect();
                } else {
                    client.connect(options);
                }
                emitter.onNext(client);
                emitter.onComplete();
            }
        });
    }

    public static Observable<MqttMessage> message(final MqttClient client, final String topic) {
        final Observable<MqttMessage> msgObs = Observable.create(new ObservableOnSubscribe<MqttMessage>() {
            @Override
            public void subscribe(ObservableEmitter<MqttMessage> emitter) throws Exception {
                client.setCallback(new MqttCallback() {

                    @Override
                    public void connectionLost(Throwable e) {
                        emitter.onError(e);
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
                client.subscribe(topic);
            }
        });
        if (client.isConnected()) {
            return msgObs;
        } else {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            return connect(client, options).flatMap(new Function<MqttClient, ObservableSource<MqttMessage>>() {
            //return connect(client).flatMap(new Function<MqttClient, ObservableSource<MqttMessage>>() {
                @Override
                public ObservableSource<MqttMessage> apply(MqttClient clkient) throws Exception {
                    return msgObs;
                }
            });
        }
    }
}
