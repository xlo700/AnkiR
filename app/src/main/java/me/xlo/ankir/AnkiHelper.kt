package me.xlo.ankir

import android.content.Context
import android.net.Uri
import com.ichi2.anki.FlashCardsContract
import com.ichi2.anki.api.AddContentApi

class AnkiHelper(
    val context : Context
) {
    val mContentResolver = context.contentResolver
    val mApi = AddContentApi(context)

    fun getReviewInfo() : MutableList<Pair<String, String>> {
        var arr = mutableListOf<Pair<String, String>>()
        var NoteID : String //key
        var CardOrd : String //value

        //selection & selectionArgs are necessary, default values can only get 1 note
        val cursor = mContentResolver.query(FlashCardsContract.ReviewInfo.CONTENT_URI,arrayOf(FlashCardsContract.ReviewInfo.NOTE_ID, FlashCardsContract.ReviewInfo.CARD_ORD),"limit=?",arrayOf("100"),null)

        if(cursor != null) {
            val nid = cursor.getColumnIndex(FlashCardsContract.ReviewInfo.NOTE_ID)
            val cid = cursor.getColumnIndex(FlashCardsContract.ReviewInfo.CARD_ORD)
            while (cursor.moveToNext()) {


                NoteID = cursor.getString(nid)
                CardOrd = cursor.getString(cid)
                arr.add(Pair(NoteID,CardOrd))
            }
            cursor.close()
        }
        return arr
    }
    fun getCard(nid : String,cid : String) : ACard? {
        var card = ACard("","","","")
        val proj = arrayOf(
            FlashCardsContract.Card.CARD_NAME,
            FlashCardsContract.Card.DECK_ID,
            FlashCardsContract.Card.ANSWER_PURE,
            FlashCardsContract.Card.QUESTION_SIMPLE
        )
        val noteuri = Uri.withAppendedPath(FlashCardsContract.Note.CONTENT_URI,nid)
        val cardsuri = Uri.withAppendedPath(noteuri,"cards")
        val carduri = Uri.withAppendedPath(cardsuri,cid)
        val cursor = mContentResolver.query(carduri,proj,null,null,null)
        if(cursor != null) {
            val CardNameIndex = cursor.getColumnIndex(FlashCardsContract.Card.CARD_NAME)
            val DeckIDIndex = cursor.getColumnIndex(FlashCardsContract.Card.DECK_ID)
            val AnswerIndex = cursor.getColumnIndex(FlashCardsContract.Card.ANSWER_PURE)
            val QuestionIndex = cursor.getColumnIndex(FlashCardsContract.Card.QUESTION_SIMPLE)
            while (cursor.moveToNext()) {
                card.mName = cursor.getString(CardNameIndex)
                card.mDeckID = cursor.getString(DeckIDIndex)
                card.mAnswer = cursor.getString(AnswerIndex)
                card.mQuestion = cursor.getString(QuestionIndex)
            }
            return card
        }
        return null
    }
}