package jp.techacademy.yae.wakahara.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*

const val EXTRA_TASK = "jp.techacademy.yae.wakahara.taskapp.TASK"

class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(t: Realm) {
            reloadListView()
        }
   }

    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        // Realm の設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListView のアダプタ生成
        mTaskAdapter = TaskAdapter(this)

        // ListView をタップした時の処理
        listView1.setOnItemClickListener { parent, view, position, id ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListView を長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, view, position, id ->
            // クリックされたタスクを取得
            val task = parent.adapter.getItem(position) as Task

            // 削除するかどうかをダイアログで確認
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか？")

            builder.setPositiveButton("OK") { _,_ ->
                // タスクを削除
                mRealm.beginTransaction()
                val realmResults = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()
                realmResults.deleteAllFromRealm()
                mRealm.commitTransaction()

                // アラームを削除
                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity, task.id, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)
            }
            builder.setNegativeButton("キャンセル", null)

            val dialog = builder.create()
            dialog.show()

            // 通常のクリックイベントを起こさない
            true
        }

        reloadListView()
    }

    private fun reloadListView() {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

        // 結果を TaskList として設定
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }

}
