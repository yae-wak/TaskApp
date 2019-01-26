package jp.techacademy.yae.wakahara.taskapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast
import io.realm.Realm
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_category.*

class CategoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // 編集用にカテゴリを取得
        var category: Category? = null
        val categoryId = intent.getIntExtra(EXTRA_CATEGORY, -1)
        if (categoryId >= 0) {
            val realm = Realm.getDefaultInstance()
            category = realm.where(Category::class.java).equalTo("id", categoryId).findFirst()
            nameEditText.setText(category!!.name)
            realm.close()
        }

        // 新規作成の場合は削除ボタンを表示しない
        if (category == null) {
            deleteButton.visibility = View.GONE
        }

        // 決定ボタン押下
        doneButton.setOnClickListener {
            // 名前の空チェック
            if (nameEditText.text.isNullOrBlank()) {
                Toast.makeText(this@CategoryActivity, "名前を入力してください", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // 名前の重複チェック
            val realm = Realm.getDefaultInstance()
            val sameNameResults = realm.where(Category::class.java).equalTo("name", nameEditText.text.toString())
            if (sameNameResults.count() > 0) {
                Toast.makeText(this@CategoryActivity, "名前の重複しているカテゴリが存在します", Toast.LENGTH_LONG).show()
                realm.close()
                return@setOnClickListener
            }

            // 新規作成の場合、カテゴリを生成してIDを発行
            if (category == null) {
                category = Category()
                val maxIdCategory = realm.where(Category::class.java).sort("id", Sort.DESCENDING).findFirst()
                category!!.id = (maxIdCategory?.id ?: -1) + 1
            }

            // カテゴリを更新または追加
            realm.executeTransaction {
                category!!.name = nameEditText.text.toString()
                it.copyToRealmOrUpdate(category)
            }

            realm.close()

            // 画面を閉じる
            finish()
        }

        // 削除ボタン押下（更新時のみ）
        deleteButton.setOnClickListener {
            // 削除するかダイアログで確認
            val builder = AlertDialog.Builder(this@CategoryActivity)
            builder.setTitle("削除")
            builder.setMessage(category!!.name + "に所属するタスクも全て削除されます。本当に削除しますか？")

            builder.setPositiveButton("OK") { _,_ ->
                val realm = Realm.getDefaultInstance()
                realm.executeTransaction {
                    // タスクとタイマーを削除
                    val tasks = category!!.taskList.toList()
                    tasks.forEach {
                        it.deleteFromRealm(applicationContext)
                    }
                    // カテゴリを削除
                    category!!.deleteFromRealm()
                }
                realm.close()

                // 画面を閉じて、タスク一覧画面に戻る
                startActivity(Intent(this@CategoryActivity, MainActivity::class.java))
            }
            builder.setNegativeButton("キャンセル", null)

            builder.create().show()
        }
    }
}
