package jp.techacademy.yae.wakahara.taskapp

import io.realm.RealmObject
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
}