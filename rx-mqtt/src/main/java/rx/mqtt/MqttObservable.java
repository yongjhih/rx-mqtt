package rx.mqtt;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by andrew on 11/17/16.
 */

public class MqttObservable {
    public static Observable<IMqttDeliveryToken> connect(final String url, final String id) {
        MemoryPersistence persistence = new MemoryPersistence();
        MqttClient client = null;
        try {
            client = new MqttClient(url, id, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            client.connect(connOpts);
        } catch (MqttException e) {
            System.out.println("reason " + e.getReasonCode());
            System.out.println("msg " + e.getMessage());
            System.out.println("loc " + e.getLocalizedMessage());
            System.out.println("cause " + e.getCause());
            System.out.println("excep " + e);
            e.printStackTrace();
        }

        if (client == null) return Observable.empty();

        MqttClient finalClient = client;
        return Observable.create(new ObservableOnSubscribe<IMqttDeliveryToken>() {
            @Override
            public void subscribe(ObservableEmitter<IMqttDeliveryToken> emitter) throws Exception {
                finalClient.setCallback(new MqttCallback() {

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
                        emitter.onNext(token);
                        emitter.onComplete();
                    }
                });
                finalClient.connect();
            }
        });
    }
    public static Observable<MqttMessage> message(final String url, final String topic, final String id) {
        MemoryPersistence persistence = new MemoryPersistence();
        MqttClient client = null;
        try {
            client = new MqttClient(url, id, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            client.connect(connOpts);
        } catch (MqttException e) {
            System.out.println("reason " + e.getReasonCode());
            System.out.println("msg " + e.getMessage());
            System.out.println("loc " + e.getLocalizedMessage());
            System.out.println("cause " + e.getCause());
            System.out.println("excep " + e);
            e.printStackTrace();
        }

        if (client == null) return Observable.empty();

        MqttClient finalClient = client;
        return Observable.create(new ObservableOnSubscribe<MqttMessage>() {
            @Override
            public void subscribe(ObservableEmitter<MqttMessage> emitter) throws Exception {
                finalClient.setCallback(new MqttCallback() {

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
                finalClient.subscribe(topic);
            }
        });
    }
}
