package jp.techacademy.yae.wakahara.taskapp

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*

const val EXTRA_TASK = "jp.techacademy.yae.wakahara.taskapp.TASK"
const val EXTRA_CATEGORY = "jp.techacademy.yae.wakahara.taskapp.CATEGORY"

class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(t: Realm) {
            reloadView()
        }
   }

    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 追加ボタン押下
        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, TaskActivity::class.java)
            startActivity(intent)
        }

        // Realm の設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // Spinner のアダプタ設定
        val results = mRealm.where(Category::class.java).sort("id").findAll()
        val categoryList = arrayListOf(Category(-1, "全てのカテゴリ"))
        categoryList.addAll(results.toList())
        val categoryAdapter = CategoryAdapter(this, R.layout.spinner_item, categoryList)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_list_item_checked)
        categorySpinner.adapter = categoryAdapter

        // Spinner を選択したときの処理
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // カテゴリが選択されたとき
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                reloadView()
            }
            // 何も選択されなかったとき
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // ListView のアダプタ生成
        mTaskAdapter = TaskAdapter(this)
        listView1.adapter = mTaskAdapter

        // ListView をタップしたときの処理
        listView1.setOnItemClickListener { parent, view, position, id ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, TaskActivity::class.java)
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
                val task = mRealm.where(Task::class.java).equalTo("id", task.id).findFirst()
                task!!.deleteFromRealm(applicationContext)
                mRealm.commitTransaction()
            }
            builder.setNegativeButton("キャンセル", null)

            val dialog = builder.create()
            dialog.show()

            // 通常のクリックイベントを起こさない
            true
        }

        if (categorySpinner.count > 1) reloadView()
    }

    private fun reloadView() {
        // カテゴリの表示を更新
        val categoryAdapter = categorySpinner.adapter as CategoryAdapter
        categoryAdapter.clear()
        categoryAdapter.add(Category(-1, "全てのカテゴリ"))
        categoryAdapter.addAll(mRealm.where(Category::class.java).sort("id").findAll())

        // 全てのカテゴリを表示するとき
        if (categorySpinner.selectedItemId == -1L) {
            // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
            val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

            // 結果を TaskList として設定
            mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)
        }
        // 特定のカテゴリのみ表示するとき
        else {
            val category = mRealm.where(Category::class.java).equalTo("id", categorySpinner.selectedItemId.toInt()).findFirst()
            if (category != null) {
                mTaskAdapter.taskList = category.taskList.toMutableList()
            }
        }

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }

}
