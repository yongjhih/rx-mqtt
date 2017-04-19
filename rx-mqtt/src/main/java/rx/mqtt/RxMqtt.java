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

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.CheckReturnValue;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by andrew on 11/17/16.
 */

public class RxMqtt {
    @NonNull
    public static IMqttAsyncClient client(@NonNull final String url) throws MqttException {
        return client(url, MqttClient.generateClientId());
    }

    @NonNull
    public static IMqttAsyncClient client(
            @NonNull final String url,
            @NonNull final String id) throws MqttException {
        return client(url, id, new MemoryPersistence());
    }

    @NonNull
    public static IMqttAsyncClient client(
            @NonNull final String url,
            @NonNull final String id,
            @NonNull final MqttClientPersistence persistence) throws MqttException {
        return new MqttAsyncClient(url, id, persistence);
    }

    @NonNull
    @CheckReturnValue
    public static Maybe<IMqttToken> connect(@NonNull final IMqttAsyncClient client) {
        return  connect(client, new MqttConnectOptions());
    }
    public static Maybe<IMqttToken> connect(@NonNull final IMqttAsyncClient client,
                                            @NonNull final MqttConnectOptions options) {
        return Maybe.create(new MaybeOnSubscribe<IMqttToken>() {
            @Override
            public void subscribe(@NonNull final MaybeEmitter<IMqttToken> emitter)
                    throws Exception {
                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(@Nullable final Throwable e) {
                        if (!emitter.isDisposed()) {
                            if (e != null) {
                                emitter.onError(e);
                            } else {
                                emitter.onError(new RuntimeException("Connection Lost"));
                            }
                        }
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

                // ?if (!client.isConnected()) {
                client.connect(options, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(@NonNull final IMqttToken token) {
                        if (!emitter.isDisposed()) {
                            emitter.onSuccess(token);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull final IMqttToken token,
                                          @Nullable final Throwable e) {
                        if (!emitter.isDisposed()) {
                            if (e != null) {
                                emitter.onError(e);
                            } else {
                                emitter.onError(new RuntimeException("Connection Lost"));
                            }
                        }
                    }
                });
            }
        });
    }

    public static void disconnectAndClose(@NonNull final IMqttAsyncClient client) {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        try {
            client.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void disconnectAndCloseAsync(@NonNull final IMqttAsyncClient client) {
        try {
            client.disconnect(null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        client.close();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    try {
                        client.close();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Auto close the client if disposed
     *
     * @param client
     * @param topic
     * @return
     */
    public static Observable<MqttMessage> message(@NonNull final IMqttAsyncClient client,
                                                  @NonNull final String topic) {
        final Observable<MqttMessage> msgObs = Observable.create(new ObservableOnSubscribe<MqttMessage>() {
            @Override
            public void subscribe(@NonNull final ObservableEmitter<MqttMessage> emitter)
                    throws Exception {
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        Schedulers.io().createWorker().schedule(new Runnable() {
                            @Override
                            public void run() {
                                disconnectAndCloseAsync(client);
                            }
                        });
                    }
                });

                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(@Nullable final Throwable e) {
                        if (!emitter.isDisposed()) {
                            if (e != null) {
                                emitter.onError(e);
                            } else {
                                emitter.onError(new RuntimeException("Connection Lost"));
                            }
                        }
                    }

                    @Override
                    public void messageArrived(@NonNull final String topic,
                                               @NonNull final MqttMessage message)
                            throws Exception {
                        if (!emitter.isDisposed()) {
                            emitter.onNext(message);
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        // nothing
                    }
                });

                client.subscribe(topic, 0, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(@NonNull IMqttToken token) {
                        // nothing
                    }

                    @Override
                    public void onFailure(@NonNull final IMqttToken token,
                                          @Nullable final Throwable e) {
                        if (!emitter.isDisposed()) {
                            if (e != null) {
                                emitter.onError(e);
                            } else {
                                emitter.onError(new RuntimeException("Connection Lost"));
                            }
                        }
                    }
                }, new IMqttMessageListener() {
                    @Override
                    public void messageArrived(String topic, MqttMessage message)
                            throws Exception {
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
            return connect(client, options).flatMapObservable(new Function<IMqttToken,
                    ObservableSource<MqttMessage>>() {
                @Override
                public ObservableSource<MqttMessage> apply(IMqttToken token) throws Exception {
                    return msgObs;
                }
            });
        }
    }
}
