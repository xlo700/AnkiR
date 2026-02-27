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
    val DECK_ID = "did"
    val NOTE_ID = "nid"
    val CARD_ORD = "cord"
    val CARD_TABLE = "card"
    val REVIEW_TABLE = "review"
    val createCard = "CREATE TABLE $CARD_TABLE (" +
            "nid integer," +
            "cord integer," +
            "did integer," +
            "answer text," +
            "question text" +
            ");"
    val createReview = "CREATE TABLE $REVIEW_TABLE (" +
            "nid integer," +
            "cord integer," +
            "did integer," +
            "answer text," +
            "question text" +
            ");"

    override fun onCreate(db: SQLiteDatabase?) {
        if(db == null)return
        db.execSQL(createCard)
        db.execSQL(createReview)
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if(db == null)return
        db.execSQL(createReview)
    }
    fun addCard(card : Card, table : String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(NOTE_ID,card.mNoteId.toLong())
            put(CARD_ORD,card.mCardOrd.toLong())
            put(DECK_ID,card.mDeckID.toLong())
            put(ANSWER,card.mAnswer)
            put(QUESTION,card.mQuestion)
        }
        db.insert(table,null,values)
    }
    fun queryCards(table: String) : List<Card> {
        val db = this.writableDatabase
        val cards = mutableListOf<Card>()
        val columns = arrayOf(NOTE_ID, CARD_ORD, DECK_ID, ANSWER, QUESTION)
        val cursor = db.query(table,columns,null,null,null,null,null)
        cursor.use {
            val answer = cursor.getColumnIndex(ANSWER)
            val question = cursor.getColumnIndex(QUESTION)
            val noteId = cursor.getColumnIndex(NOTE_ID)
            val cardOrd = cursor.getColumnIndex(CARD_ORD)
            val deckId = cursor.getColumnIndex(DECK_ID)
            while (cursor.moveToNext()) {
                cards.add(Card(noteId.toString(),cardOrd.toString(),deckId.toString(),cursor.getString(answer),cursor.getString(question)))
            }
        }
        return cards
    }
    fun clear() {
        this.writableDatabase.delete(CARD_TABLE,null,null)
        this.writableDatabase.delete(REVIEW_TABLE,null,null)
        mContext.getSharedPreferences("config",MODE_PRIVATE).edit {
            putBoolean("is_save",false)
        }
        mContext.getSharedPreferences("config",MODE_PRIVATE).edit {
            putInt("card_index",0)
        }
    }
}