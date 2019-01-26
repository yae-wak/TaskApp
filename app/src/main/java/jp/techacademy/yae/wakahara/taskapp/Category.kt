package jp.techacademy.yae.wakahara.taskapp

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.Sort
import io.realm.annotations.PrimaryKey
import java.io.Serializable

open class Category: RealmObject, Serializable {
    @PrimaryKey
    var id: Int
    var name: String // カテゴリ名
    var taskList: RealmList<Task> // Task と1対多の Relationship

    constructor() : super() {
        this.id = 0
        this.name = ""
        this.taskList = RealmList<Task>()
    }

    constructor(id: Int, name: String) : super() {
        this.id = id
        this.name = name
        this.taskList = RealmList<Task>()
    }

    public fun addTask(task: Task?) {
        this.taskList.add(task)
        this.taskList.sort("date", Sort.DESCENDING)
    }

    public fun removeTask(task: Task?) {
        this.taskList.remove(task)
    }
}