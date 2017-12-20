package wangqc.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.svm_screen.*
import wangqc.myapplication.svm.svm_scale
import wangqc.myapplication.svm.svm_predict
import wangqc.myapplication.svm.svm_train
import org.jetbrains.anko.doAsync
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.PrintStream

class MainActivity : AppCompatActivity() {

    lateinit var mSensorManger: SensorManager
    lateinit var mAccSensor: Sensor
    var hz = (1000 * 1000 / 32).toInt()

    var isStartCollection: Boolean = false

    var mCollectionNum: Int = 0
    var mCurrentCollectionNum: Int = 0
    var mLabel: Int = 0

    var mSensorEventLister = object : SensorEventListener {
        var accData = DoubleArray(128)
        var num = 128
        var index = 0

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onSensorChanged(p0: SensorEvent) {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.


            val x = p0.values[0]
            val y = p0.values[1]
            val z = p0.values[2]

            val acc = Math.sqrt((x * x + y * y + z * z).toDouble())
            tv_acc.text = acc.toString()

            if (index < num) {
                accData[index++] = acc
            } else {
                val features = Util.dataToFeatures(accData, hz)
                Util.writeToFile("${filesDir}/train", mLabel, features)

                index = 0


                mCurrentCollectionNum ++
                tv_collected_num.text = "Collected${mCurrentCollectionNum}"

                if (mCurrentCollectionNum >= mCollectionNum) {
                    collection()
                }
            }

        }
    }

    var mTestSensorEventListener = object : SensorEventListener {

        var accData = DoubleArray(128)
        var num = 128
        var index = 0

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onSensorChanged(p0: SensorEvent) {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            val x = p0.values[0]
            val y = p0.values[1]
            val z = p0.values[2]

            val acc = Math.sqrt((x * x + y * y + z * z).toDouble())
            tv_acc.text = acc.toString()

            if (index < num) {
                accData[index++] = acc
            } else {
                index = 0
                val features = Util.dataToFeatures(accData, hz)
                val code = Util.predictUnScaleData(features)
                testResult(code)
            }

        }
    }

    private fun testResult(code: Double) {
        iv_still.setImageResource(R.mipmap.gait_still_off)
        iv_walk.setImageResource(R.mipmap.gait_walk_off)
        iv_run.setImageResource(R.mipmap.gait_run_off)
        when (code.toInt()) {
            0 -> {
                iv_still.setImageResource(R.mipmap.gait_still)
            }
            1 -> {
                iv_walk.setImageResource(R.mipmap.gait_walk)
            }
            2 -> {
                iv_run.setImageResource(R.mipmap.gait_run)
            }
        }
    }


    var mOnTrainNumCheckChangeListener = object : RadioGroup.OnCheckedChangeListener {
        override fun onCheckedChanged(p0: RadioGroup?, p1: Int) {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            when (p1) {
                R.id.rb_num1 -> {
                    mCollectionNum = 30
                    tv_train_num.text = "Sample Size: 30"
                    Toast.makeText(this@MainActivity, "Pick 30", Toast.LENGTH_SHORT).show()
                }
                R.id.rb_num2 -> {
                    mCollectionNum = 50
                    tv_train_num.text = "Sample Size: 50"
                    Toast.makeText(this@MainActivity, "Pick 50", Toast.LENGTH_SHORT).show()
                }
                R.id.rb_num3 -> {
                    mCollectionNum = 100
                    tv_train_num.text = "Sample Size: 100"
                    Toast.makeText(this@MainActivity, "Pick 100", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }
    var mOnLableCheckChangeListener = object : RadioGroup.OnCheckedChangeListener {
        override fun onCheckedChanged(p0: RadioGroup?, p1: Int) {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            when (p1) {
                R.id.rb_label1 -> {
                    mLabel = 0
                    tv_label.text = "Label0"
                }
                R.id.rb_label2 -> {
                    mLabel = 1
                    tv_label.text = "Label1"
                }
                R.id.rb_label3 -> {
                    mLabel = 2
                    tv_label.text = "Label2"
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.svm_screen)

        mSensorManger = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccSensor = mSensorManger.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


        // 收集数据
        iv_collection.setOnClickListener {
            Toast.makeText(this@MainActivity, "Collect", Toast.LENGTH_SHORT).show()
            collection()
        }
        // 训练模型
        iv_train.setOnClickListener {
            Toast.makeText(this@MainActivity, "Train", Toast.LENGTH_SHORT).show()
            doAsync {
                train()
                runOnUiThread {
                    val reader = BufferedReader(InputStreamReader(FileInputStream("${filesDir}/accuracy")))
                    val readLine = reader.readLine()
                    tv_accuracy.text = readLine
                    iv_train.setImageResource(R.mipmap.train)
                }
            }
        }
        // 测试
        iv_test.setOnClickListener {
            //Toast.makeText(this@MainActivity, "点击了测试", Toast.LENGTH_SHORT).show()
            test()
        }

        rg_num.setOnCheckedChangeListener(mOnTrainNumCheckChangeListener)
        rg_num.check(R.id.rb_num1)

        rg_label.setOnCheckedChangeListener(mOnLableCheckChangeListener)
        rg_label.check(R.id.rb_label1)
    }

    var isTest: Boolean = false
    private fun test() {
        Util.loadFile("${filesDir}/range", "${filesDir}/model")
        if (!isTest) {
            mSensorManger.registerListener(mTestSensorEventListener, mAccSensor, hz)
            iv_test.setImageResource(R.mipmap.test)
        } else {
            mSensorManger.unregisterListener(mSensorEventLister)
            iv_test.setImageResource(R.mipmap.test_off)
        }
        isTest = !isTest
    }

    // 训练模型
    private fun train() {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        createScaleFile(arrayOf("-l", "0", "-u", "1", "-s", "${filesDir}/range", "${filesDir}/train"))
        createModelFile(arrayOf("-s", "0", "-c", "128.0", "-t", "2", "-g", "8.0", "-e", "0.1", "${filesDir}/scale", "${filesDir}/model"))
        createPredictFile(arrayOf("${filesDir}/scale", "${filesDir}/model", "${filesDir}/predict"))
    }

    // 预测
    private fun createPredictFile(param: Array<String>) {
        val out = System.out
        System.setOut(PrintStream("${filesDir}/accuracy"))
        svm_predict.main(param)
        System.setOut(out)

    }

    // 创建模型文件
    private fun createModelFile(param: Array<String>) {
        svm_train.main(param)
    }

    // 创建归一化文件
    private fun createScaleFile(param: Array<String>) {
        val out = System.out
        System.setOut(PrintStream("${filesDir}/scale"))
        svm_scale.main(param)
        System.setOut(out)
    }

    // 收集数据
    private fun collection() {
        // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        if (!isStartCollection) {
            mSensorManger.registerListener(mSensorEventLister, mAccSensor, hz)
            iv_collection.setImageResource(R.mipmap.sample)
        } else {
            mSensorManger.unregisterListener(mSensorEventLister)
            iv_collection.setImageResource(R.mipmap.sample_off)
        }
        isStartCollection = !isStartCollection
    }
}