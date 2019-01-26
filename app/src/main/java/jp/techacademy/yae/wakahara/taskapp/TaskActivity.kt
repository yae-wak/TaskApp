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
import android.widget.Toast
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_task.*
import kotlinx.android.synthetic.main.content_task.*
import java.util.*

class TaskActivity : AppCompatActivity() {

    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0
    private var mMinute = 0
    private var mTask: Task? = null

    // 日付ボタン押下
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

    // 時間ボタン押下
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

    // 決定ボタン押下
    private val mOnDoneClickListener = View.OnClickListener {
        // タスクを更新または追加
        addTask()
        // アクティビティ終了
        finish()
    }

    // カテゴリ追加ボタン押下
    private val mOnAddCategoryClickListener = View.OnClickListener {
        // カテゴリ画面へ遷移
        val intent = Intent(this, CategoryActivity::class.java)
        startActivity(intent)
    }

    // カテゴリ編集ボタン押下
    private val mOnEditCategoryClickListener = View.OnClickListener {
        // カテゴリがないときはボタンが無効なので、必ず存在する
        // カテゴリIDを指定して編集画面へ遷移
        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra(EXTRA_CATEGORY, categorySpinner.selectedItemId.toInt())
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        // ActionBar を設定する
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true) // 戻るボタンを有効にする
        }

        // ボタンの設定
        dateButton.setOnClickListener(mOnDateClickListener)
        timesButton.setOnClickListener(mOnTimeClickListener)
        doneButton.setOnClickListener(mOnDoneClickListener)
        addCategoryButton.setOnClickListener(mOnAddCategoryClickListener)
        editCategoryButton.setOnClickListener(mOnEditCategoryClickListener)

        // Spinner のアダプタ生成
        val categoryAdapter = CategoryAdapter(this, R.layout.spinner_item, ArrayList<Category>())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_list_item_checked)
        categorySpinner.adapter = categoryAdapter

        // EXTRA_TASK から Task の id を取得して、 id から Task のインスタンスを取得する
        val realm = Realm.getDefaultInstance()
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)
        mTask = realm.where(Task::class.java).equalTo("id", taskId).findFirst()
        realm.close()

        // 日付と時間を取得
        val calendar = Calendar.getInstance() // 新規作成では現在時刻
        if (mTask != null) {
            // 更新の場合、時間が存在する
            calendar.time = mTask!!.date
        }
        mYear = calendar.get(Calendar.YEAR)
        mMonth = calendar.get(Calendar.MONTH)
        mDay = calendar.get(Calendar.DAY_OF_MONTH)
        mHour = calendar.get(Calendar.HOUR_OF_DAY)
        mMinute = calendar.get(Calendar.MINUTE)
    }

    /**
     * カテゴリ画面から戻っても情報が正しく表示されるように、表示更新は onStart で行う
     */
    override fun onStart() {
        super.onStart()

        // カテゴリ情報：Spinner の情報更新
        val realm = Realm.getDefaultInstance()
        val categoryAdapter = categorySpinner.adapter as CategoryAdapter
        categoryAdapter.clear()
        categoryAdapter.addAll(realm.where(Category::class.java).sort("id").findAll())
        realm.close()

        // カテゴリが存在しない場合はカテゴリ編集・決定ボタンを押せないようにする
        if (categorySpinner.count == 0) {
            doneButton.isEnabled = false
            editCategoryButton.visibility = View.GONE
            categoryAdapter.add(Category(-1, "カテゴリなし"))
        }
        else {
            doneButton.isEnabled = true
            editCategoryButton.visibility = View.VISIBLE
        }

        // タスク情報
        if (mTask != null) {
            categorySpinner.setSelection(categoryAdapter.getPosition(mTask!!.getCategory()))
            titleEditText.setText(mTask!!.title)
            contentEditText.setText(mTask!!.contents)
            val dateString = "$mYear/%02d/%02d".format(mMonth+1, mDay)
            val timeString ="%02d:%02d".format(mHour, mMinute)
            dateButton.text = dateString
            timesButton.text = timeString
        }
    }

    private fun addTask() {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()

        var isNew = mTask == null
        if (isNew) {
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

            // カテゴリにタスクを関連付ける
            val category = categorySpinner.selectedItem as Category
            category.addTask(mTask)
            realm.copyToRealmOrUpdate(category)
        }
        else {
            // カテゴリが変更されていたら修正
            if (mTask!!.getCategory().id != categorySpinner.selectedItemId.toInt()) {
                val orgCategory = mTask!!.getCategory()
                val newCategory = categorySpinner.selectedItem as Category
                orgCategory.removeTask(mTask)
                newCategory.addTask(mTask)
            }
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

        // 新規登録の場合にアラーム
        if (isNew) {
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
}
