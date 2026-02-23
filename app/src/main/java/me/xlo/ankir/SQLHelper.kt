package me.xlo.ankir

import Card
import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.content.edit

class SQLHelper(context : Context, name : String, version : Int) : SQLiteOpenHelper(context, name, null, version) {
    val mContext = context
    val ANSWER = "answer"
    val QUESTION = "question"

    val createCard = "CREATE TABLE card (" +
            "nid integer," +
            "cord integer," +
            "did integer," +
            "answer text," +
            "question text" +
            ");"

    override fun onCreate(db: SQLiteDatabase?) {
        if(db == null)return
        db.execSQL(createCard)
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }
    fun addCard(nid : Long, cord : Long, did : Long, answer : String, question : String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("nid",nid)
            put("cord",cord)
            put("did",did)
            put("answer",answer)
            put("question",question)
        }
        db.insert("card",null,values)
    }
    fun queryCards() : List<Card> {
        val db = this.writableDatabase
        val cards = mutableListOf<Card>()
        val columns = arrayOf(ANSWER,QUESTION)
        val cursor = db.query("card",columns,null,null,null,null,null)
        cursor.use {
            val answer = cursor.getColumnIndex(ANSWER)
            val question = cursor.getColumnIndex(QUESTION)
            while (cursor.moveToNext()) {
                cards.add(Card("","","",cursor.getString(answer),cursor.getString(question)))
            }
        }
        return cards
    }
    fun clear() {
        this.writableDatabase.delete("card",null,null)
        mContext.getSharedPreferences("config",MODE_PRIVATE).edit {
            putBoolean("is_save",false)
        }
    }
}