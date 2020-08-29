package com.lorenzolerate.weather.util

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import android.util.Log


class MySuggestionProvider : ContentProvider() {
    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        TODO("Not yet implemented")
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? {
        Log.d(this.javaClass.name,"query(...) called")
        val cursor = MatrixCursor(arrayOf(BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1,SearchManager.EXTRA_DATA_KEY,SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA))
        //TODO research about text below
        cursor.addRow(arrayOf(1, "Current location","I don't know what to put here. Change this!",""))
        return cursor
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
//        TODO("Not yet implemented")
        return 0
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
//        TODO("Not yet implemented")
        return 0
    }

    override fun getType(p0: Uri): String? {
//        TODO("Not yet implemented")
        return null
    }
}