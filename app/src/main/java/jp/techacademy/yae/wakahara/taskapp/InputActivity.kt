package jp.techacademy.yae.wakahara.taskapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_input.*
import kotlinx.android.synthetic.main.content_input.*
import java.util.*

class InputActivity : AppCompatActivity() {

    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0
    private var mMinute = 0
    private var mTask: Task? = null

    private val mOnDateClickListener = View.OnClickListener {
        val datePickerDialog = DatePickerDialog(this,
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                mYear = year
                mMonth = month
                mDay = dayOfMonth
                val dateString = "$mYear/%02d/%02d".format(mMonth+1, mDay)
                dateButton.text = dateString
            }, mYear, mMonth, mDay)
        datePickerDialog.show()
    }

    private val mOnTimeClickListener = View.OnClickListener {
        val timePickerDialog = TimePickerDialog(this,
            TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                mHour = hourOfDay
                mMinute = minute
                val timeString ="%02d:%02d".format(mHour, mMinute)
                timesButton.text = timeString
            }, mHour, mMinute, false)
        timePickerDialog.show()
    }

    private val mDoneClickListener = View.OnClickListener {
        addTask()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        // ActionBar を設定する
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true) // 戻るボタンを有効にする
        }

        // UI 部品の設定
        dateButton.setOnClickListener(mOnDateClickListener)
        timesButton.setOnClickListener(mOnTimeClickListener)
        doneButton.setOnClickListener(mDoneClickListener)

        // EXTRA_TASK から Task の id を取得して、 id から Task のインスタンスを取得する
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)
        val realm = Realm.getDefaultInstance()
        mTask = realm.where(Task::class.java).equalTo("id", taskId).findFirst()
        realm.close()

        val calendar = Calendar.getInstance()
        if (mTask == null) {
            // 新規作成の場合
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)
        }
        else {
            // 更新の場合
            titleEditText.setText(mTask!!.title)
            contentEditText.setText(mTask!!.contents)
            calendar.time = mTask!!.date
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)

            val dateString = "$mYear/%02d/%02d".format(mMonth+1, mDay)
            val timeString ="%02d:%02d".format(mHour, mMinute)
            dateButton.text = dateString
            timesButton.text = timeString
        }
    }

    private fun addTask() {
        val realm = Realm.getDefaultInstance()

        realm.beginTransaction()

        if (mTask == null) {
            // 新規作成の場合、Realm に存在する ID の最大値 + 1 で ID を発行する
            mTask = Task()

            val realmResults = realm.where(Task::class.java).findAll()
            val identifier: Int =
                if (realmResults.max("id") != null) {
                    realmResults.max("id")!!.toInt() + 1
                } else {
                    0
                }
            mTask!!.id = identifier
        }

        // タスクの内容を設定
        mTask!!.title = titleEditText.text.toString()
        mTask!!.contents = contentEditText.text.toString()

        val calendar = GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute)
        mTask!!.date = calendar.time

        // Realm に登録
        realm.copyToRealmOrUpdate(mTask!!)
        realm.commitTransaction()

        realm.close()

        // アラーム
        val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
        resultIntent.putExtra(EXTRA_TASK, mTask!!.id)
        val resultPendingIntent = PendingIntent.getBroadcast(
            this,
            mTask!!.id,     // アラームを一意に指定するためのIDを、タスクIDで代用
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT   // 既存のPendingIntentがあればそのままデータだけ置き換える
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,    // UTC時間を指定、画面スリープ中でもアラームを発行
            calendar.timeInMillis,      // タスクのUTC時間
            resultPendingIntent
        )
    }
}
