package jp.techacademy.yae.wakahara.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import java.io.Serializable
import java.util.*

open class Task: RealmObject(), Serializable {
    var title: String = ""      // タイトル
    var contents: String = ""   // 内容
    var date: Date = Date()      // 日時

    // id をプライマリキーとして設定
    @PrimaryKey
    var id: Int = 0

    // 所属するカテゴリへの逆参照
    @LinkingObjects("taskList")
    private val categoryResults: RealmResults<Category>? = null

    /**
     * 所属するカテゴリを取得する
     */
    public fun getCategory(): Category {
        return categoryResults?.first()!!
    }

    /**
     * タスクを削除するとともに、アラームも削除する
     */
    public fun deleteFromRealm(context: Context) {
        cancelAlarm(context)
        deleteFromRealm()
    }

    /**
     * アラームを削除する
     */
    private fun cancelAlarm(context: Context) {
        val resultIntent = Intent(context, TaskAlarmReceiver::class.java)
        val resultPendingIntent = PendingIntent.getBroadcast(
            context, id, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(resultPendingIntent)
    }
}