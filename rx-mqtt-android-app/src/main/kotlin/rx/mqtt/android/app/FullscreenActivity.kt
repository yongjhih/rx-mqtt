package rx.mqtt.android.app

import android.os.Bundle
import rx.mqtt.android.RxMqtt
import android.view.View
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers.io
import kotterknife.bindView
import org.eclipse.paho.android.service.MqttAndroidClient

class FullscreenActivity : RxAppCompatActivity() {
    val mContentView: View by bindView(R.id.fullscreen)

    val url = "tcp://test.mosquitto.org:1883"
    val topic = "#"

    val mqttAndroidClient: MqttAndroidClient by lazy {
        RxMqtt.client(applicationContext, url)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)
    }

    override fun onResume() {
        super.onResume()

        mContentView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        RxMqtt.connect(mqttAndroidClient)
                .flatMap { RxMqtt.message(mqttAndroidClient, topic) }
                .map { String(it.payload) }
                .observeOn(mainThread())
                .subscribeOn(io())
                //.compose(bindToLifecycle())
                .subscribe (::println, ::println)
    }
}
