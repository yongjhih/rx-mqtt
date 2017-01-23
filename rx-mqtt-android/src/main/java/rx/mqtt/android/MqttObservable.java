package rx.mqtt.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * Created by andrew on 11/28/16.
 */

public class MqttObservable {
    public static Observable<MqttMessage> remessage(@NonNull final MqttAndroidClient client, @NonNull final String topic) {
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
            return reconnect(client).flatMap(new Function<IMqttToken, ObservableSource<MqttMessage>>() {
                //return reconnect(client).flatMap(new Function<IMqttToken, ObservableSource<MqttMessage>>() {
                @Override
                public ObservableSource<MqttMessage> apply(IMqttToken token) throws Exception {
                    return msgObs;
                }
            });
        }
    }

    public static Observable<MqttMessage> message(@NonNull final MqttAndroidClient client, @NonNull final String topic) {
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

    public static Observable<IMqttToken> connect(@NonNull final MqttAndroidClient client) {
        return connect(client, null, null);
    }

    public static Observable<IMqttToken> connect(@NonNull final MqttAndroidClient client, @Nullable final MqttConnectOptions options) {
        final DisconnectedBufferOptions bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(100);
        bufferOptions.setPersistBuffer(false);
        bufferOptions.setDeleteOldestMessages(false);
        return connect(client, options, bufferOptions);
    }

    public static Observable<IMqttToken> connect(@NonNull final MqttAndroidClient client, @Nullable final MqttConnectOptions options, @Nullable final DisconnectedBufferOptions bufferOptions) {
        MqttConnectOptions optionsInternal = (options == null) ? new MqttConnectOptions() : options;
        final URI uri;
        try {
            uri = new URI(client.getServerURI());
            if (uri.getScheme().equals("ssl")) optionsInternal.setSocketFactory(sslSocketFactory());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return Observable.create(new ObservableOnSubscribe<IMqttToken>() {
            @Override
            public void subscribe(final ObservableEmitter<IMqttToken> emitter) throws Exception {
                client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        // nothing
                    }

                    @Override
                    public void connectionLost(@Nullable Throwable e) {
                        if (e != null) emitter.onError(e);
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
                        if (bufferOptions != null) client.setBufferOpts(bufferOptions);
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

    public static SSLSocketFactory sslSocketFactory() {
        return sslSocketFactory("TLSv1.2");
    }

    public static SSLSocketFactory sslSocketFactory(@NonNull String protocol) {
        SSLContext sslContext = null;

        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            sslContext = SSLContext.getInstance(protocol);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        if (sslContext != null) {
            return sslContext.getSocketFactory();
        }
        return null;
    }

    public static Observable<IMqttToken> reconnect(@NonNull final MqttAndroidClient client) {
        return reconnect(client, new MqttConnectOptions());
    }

    public static Observable<IMqttToken> reconnect(@NonNull final MqttAndroidClient client, @Nullable final MqttConnectOptions options) {
        return reconnect(client, options, null);
    }

    public static Observable<IMqttToken> reconnect(@NonNull final MqttAndroidClient client, @Nullable final MqttConnectOptions options, @Nullable final DisconnectedBufferOptions bufferOptions) {
        MqttConnectOptions optionsInternal = (options == null) ? new MqttConnectOptions() : options;
        optionsInternal.setAutomaticReconnect(true);
        optionsInternal.setCleanSession(false);
        return connect(client, options, bufferOptions);
    }
}
