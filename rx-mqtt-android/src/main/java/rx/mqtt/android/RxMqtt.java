package rx.mqtt.android;

import android.content.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.CheckReturnValue;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Function;

/**
 * Created by andrew on 11/28/16.
 */

public class RxMqtt {
    @NonNull
    public static MqttAndroidClient client(
            @NonNull final Context context,
            @NonNull final String url) throws MqttException {
        return new MqttAndroidClient(context, url, MqttAsyncClient.generateClientId());
    }

    /**
     * Auto close client
     * mqttConnectOptions.userName = it }$
     * mqttConnectOptions.password = it.toCharArray() }
     * @param client
     * @param topic
     * @return
     */
    @NonNull
    @CheckReturnValue
    public static Observable<MqttMessage> remessage(@NonNull final MqttAndroidClient client,
                                                    @NonNull final String topic) {
        final Observable<MqttMessage> msgObs =
                Observable.create(new ObservableOnSubscribe<MqttMessage>() {
            public void subscribe(
                    @NonNull final ObservableEmitter<MqttMessage> emitter) throws Exception {
                client.subscribe(topic, 0, new IMqttMessageListener() {
                    @Override
                    public void messageArrived(
                            String topic2, @NonNull final MqttMessage message) throws Exception {
                        if (!emitter.isDisposed()) {
                            emitter.onNext(message);
                        }
                    }
                });
            }
        });

        if (client.isConnected()) {
            return msgObs;
        } else {
            return reconnect(client).flatMapObservable(
                    new Function<IMqttToken, ObservableSource<MqttMessage>>() {
                @Override
                public ObservableSource<MqttMessage> apply(IMqttToken token) throws Exception {
                    return msgObs;
                }
            });
        }
    }

    /**
     * Auto close client
     * @param client
     * @param topic
     * @return
     */
    @NonNull
    @CheckReturnValue
    public static Observable<MqttMessage> message(@NonNull final MqttAndroidClient client,
                                                  @NonNull final String topic) {
        final Observable<MqttMessage> msgObs =
                Observable.create(new ObservableOnSubscribe<MqttMessage>() {
            @Override
            public void subscribe(
                    @NonNull final ObservableEmitter<MqttMessage> emitter) throws Exception {
                client.subscribe(topic, 0, new IMqttMessageListener() {
                    @Override
                    public void messageArrived(
                            String topic2,
                            @NonNull final MqttMessage message) throws Exception {
                        if (!emitter.isDisposed()) {
                            emitter.onNext(message);
                        }
                    }
                });
            }
        });

        if (client.isConnected()) {
            return msgObs;
        } else {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            return connect(client, options).flatMapObservable(
                    new Function<IMqttToken, ObservableSource<MqttMessage>>() {
                @Override
                public ObservableSource<MqttMessage> apply(IMqttToken token) throws Exception {
                    return msgObs;
                }
            });
        }
    }

    @NonNull
    @CheckReturnValue
    public static Maybe<IMqttToken> connect(@NonNull final MqttAndroidClient client) {
        return connect(client, new MqttConnectOptions(), null);
    }

    @NonNull
    @CheckReturnValue
    public static Maybe<IMqttToken> connect(@NonNull final MqttAndroidClient client,
                                            @NonNull final MqttConnectOptions options) {
        return connect(client, options, null);
    }

    @NonNull
    @CheckReturnValue
    public static Maybe<IMqttToken> connect(
            @NonNull final MqttAndroidClient client,
            @NonNull final DisconnectedBufferOptions disconnectedBufferOptions) {
        return connect(client, new MqttConnectOptions(), disconnectedBufferOptions);
    }

    @NonNull
    @CheckReturnValue
    public static DisconnectedBufferOptions defaultDisconnectedBufferOptions() {
        final DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(100);
        bufferOptions.setPersistBuffer(false);
        bufferOptions.setDeleteOldestMessages(false);
        return bufferOptions;
    }

    /**
     * Reconnectable, Emit token every time if re/connected
     *
     * @param client
     * @param options
     * @param bufferOptions
     * @return
     */
    public static Maybe<IMqttToken> connect(
            @NonNull final MqttAndroidClient client,
            @NonNull final MqttConnectOptions options,
            @Nullable final DisconnectedBufferOptions bufferOptions) {
        return Maybe.create(new MaybeOnSubscribe<IMqttToken>() {
            @NonNull IMqttToken mToken = new SimpleMqttToken();
            @Override
            public void subscribe(
                    @NonNull final MaybeEmitter<IMqttToken> emitter) throws Exception {
                client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        if (!reconnect && !emitter.isDisposed()) {
                            emitter.onSuccess(mToken);
                        }
                    }

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
                    public void messageArrived
                            (String topic, MqttMessage message) throws Exception {
                        // nothing
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        // nothing
                    }
                });

                mToken = client.connect(options, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(@NonNull final IMqttToken token) {
                        if (!emitter.isDisposed()) {
                            if (bufferOptions != null) {
                                client.setBufferOpts(bufferOptions);
                            }
                            mToken = token;
                            emitter.onSuccess(token);
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken,
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

    public static SSLSocketFactory sslSocketFactory() {
        return sslSocketFactory("TLSv1.2");
    }

    public static SSLSocketFactory sslSocketFactory(@NonNull String protocol) {
        SSLContext sslContext = null;

        try {
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            sslContext = SSLContext.getInstance(protocol);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        } catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (sslContext != null) {
            return sslContext.getSocketFactory();
        }
        return null;
    }

    public static Maybe<IMqttToken> reconnect(@NonNull final MqttAndroidClient client) {
        return reconnect(client, reconnectOptions());
    }

    public static Maybe<IMqttToken> reconnect(
            @NonNull final MqttAndroidClient client,
            @NonNull final MqttConnectOptions options) {
        return reconnect(client, reconnectOptions(options), null);
    }

    public static Maybe<IMqttToken> reconnect(
            @NonNull final MqttAndroidClient client,
            @NonNull final MqttConnectOptions options,
            @Nullable final DisconnectedBufferOptions bufferOptions) {
        return connect(client, options, bufferOptions);
    }

    @NonNull
    public static MqttConnectOptions reconnectOptions() {
        return reconnectOptions(new MqttConnectOptions());
    }

    @NonNull
    public static MqttConnectOptions reconnectOptions(@NonNull final MqttConnectOptions options) {
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        return options;
    }

    public static class SimpleMqttToken implements IMqttToken {
        @Override
        public void waitForCompletion() throws MqttException {
            // nothing
        }

        @Override
        public void waitForCompletion(long timeout) throws MqttException {
            // nothing
        }

        @Override
        public boolean isComplete() {
            // nothing
            return false;
        }

        @Nullable
        @Override
        public MqttException getException() {
            // nothing
            return null;
        }

        @Override
        public void setActionCallback(IMqttActionListener listener) {
            // nothing
        }

        @Override
        @Nullable
        public IMqttActionListener getActionCallback() {
            // nothing
            return null;
        }

        @Override
        @Nullable
        public IMqttAsyncClient getClient() {
            // nothing
            return null;
        }

        @Override
        @Nullable
        public String[] getTopics() {
            // nothing
            return new String[0];
        }

        @Override
        public void setUserContext(Object userContext) {
            // nothing

        }

        @Override
        @Nullable
        public Object getUserContext() {
            // nothing
            return null;
        }

        @Override
        public int getMessageId() {
            // nothing
            return 0;
        }

        @Override
        @Nullable
        public int[] getGrantedQos() {
            // nothing
            return new int[0];
        }

        @Override
        public boolean getSessionPresent() {
            // nothing
            return false;
        }

        @Override
        @Nullable
        public MqttWireMessage getResponse() {
            // nothing
            return null;
        }
    }

    public static void disconnectAndCloseAsync(@NonNull final MqttAndroidClient client) {
        try {
            client.disconnect(null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    client.close();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    client.close();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static class MqttPublishException extends RuntimeException {
        private final IMqttToken token;
        public MqttPublishException(@NonNull IMqttToken token, @NonNull Throwable cause) {
            super(cause);
            this.token = token;
        }

        public MqttPublishException(@NonNull IMqttToken token) {
            this.token = token;
        }

        @NonNull
        public IMqttToken getToken() {
            return token;
        }
    }

    @NonNull
    @CheckReturnValue
    public static Maybe<IMqttToken> publish(@NonNull final MqttAndroidClient client,
                                            @NonNull final String topic,
                                            @NonNull final MqttMessage message) {
        final Maybe<IMqttToken> maybe =
                Maybe.create(new MaybeOnSubscribe<IMqttToken>() {
                    @Override
                    public void subscribe(
                            @NonNull final MaybeEmitter<IMqttToken> emitter) throws Exception {
                        client.publish(topic, message, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(@NonNull IMqttToken asyncActionToken) {
                                if (!emitter.isDisposed()) {
                                    emitter.onSuccess(asyncActionToken);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull IMqttToken asyncActionToken,
                                                  @Nullable Throwable e) {
                                if (!emitter.isDisposed()) {
                                    if (e != null) {
                                        emitter.onError(new MqttPublishException(asyncActionToken, e));
                                    } else {
                                        emitter.onError(new MqttPublishException(asyncActionToken));
                                    }
                                }
                            }
                        });
                    }
                });

        if (client.isConnected()) {
            return maybe;
        } else {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            return connect(client, options).flatMap(
                    new Function<IMqttToken, MaybeSource<IMqttToken>>() {
                        @Override
                        public MaybeSource<IMqttToken> apply(IMqttToken token) throws Exception {
                            return maybe;
                        }
                    });
        }
    }
}
