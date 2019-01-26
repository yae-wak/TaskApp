package jp.techacademy.yae.wakahara.taskapp

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import java.lang.IllegalArgumentException

/**
 * カテゴリを Spinner で表示するためのアダプタ
 */
class CategoryAdapter: ArrayAdapter<Category> {
    private val mLayoutInflater: LayoutInflater
    private val mResource: Int
    private var mDropDownResource: Int

    constructor(context: Context, resource: Int, categryList: List<Category>)
            : super(context, resource, categryList) {
        mResource = resource
        mDropDownResource = resource
        mLayoutInflater = LayoutInflater.from(context)
    }

    override fun setDropDownViewResource(resource: Int) {
        mDropDownResource = resource
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, mResource)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, mDropDownResource)
    }

    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup, resource: Int): View {
        val view = convertView ?: mLayoutInflater.inflate(resource, parent, false)
        if (view is TextView) {
            val category: Category = getItem(position)
            view.text = category.name
            return view
        }
        else {
            Log.e("CategoryAdapter", "Resource must be only one TextView.")
            throw IllegalArgumentException("Resource must be only one TextView.")
        }
    }
}