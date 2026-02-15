package me.xlo.ankir

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ichi2.anki.FlashCardsContract
import com.ichi2.anki.api.AddContentApi

class AnkiHelper(
    val context : Context
) {
    val TAG = "AnkiR"
    data class DeckInfo(
        val deckId: String,
        val deckName: String
    )
    val mContentResolver = context.contentResolver
    val mApi = AddContentApi(context)
    fun getReviewInfo(did : String) : MutableList<Pair<String, String>> {
        val arr = mutableListOf<Pair<String, String>>()

        //containing "::" means it is a subdeck, leading to return the same cards again
        if(mApi.getDeckName(did.toLong()).contains("::"))return arr

        var NoteID : String //key
        var CardOrd : String //value

        val cursor = mContentResolver.query(FlashCardsContract.ReviewInfo.CONTENT_URI,
            arrayOf(FlashCardsContract.ReviewInfo.NOTE_ID, FlashCardsContract.ReviewInfo.CARD_ORD),
            "limit=?, deckID=?",
            arrayOf("100",did),
            null)
        cursor?.use{
            val nid = cursor.getColumnIndex(FlashCardsContract.ReviewInfo.NOTE_ID)
            val cid = cursor.getColumnIndex(FlashCardsContract.ReviewInfo.CARD_ORD)
            while (cursor.moveToNext()) {
                NoteID = cursor.getLong(nid).toString()
                CardOrd = cursor.getInt(cid).toString()
                arr.add(Pair(NoteID,CardOrd))
            }
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
        cursor?.use{
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
    fun getDeckID(DeckName : String) : String {
        val DeckList = mApi.deckList
        DeckList.forEach {
            if(it.value == DeckName)return it.key.toString()
        }
        return "-1"
    }
    fun filterCards(list : MutableList<ACard>, DeckName: String?) : MutableList<ACard> {
        if((DeckName == null) || (DeckName == "")) return list
        val FilteredList = mutableListOf<ACard>()
        list.forEach {
            if(it.mDeckID == getDeckID(DeckName))FilteredList.add(it)
        }
        return FilteredList
    }
    fun getAllDecks(): List<DeckInfo> {
        val decks = mutableListOf<DeckInfo>()
            val cursor = mContentResolver.query(
                FlashCardsContract.Deck.CONTENT_ALL_URI,
                arrayOf(
                    FlashCardsContract.Deck.DECK_ID,
                    FlashCardsContract.Deck.DECK_NAME
                ),
                null,
                null,
                null
            )
            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(FlashCardsContract.Deck.DECK_ID)
                val nameIndex = it.getColumnIndexOrThrow(FlashCardsContract.Deck.DECK_NAME)

                while (it.moveToNext()) {
                    val deckId = it.getLong(idIndex).toString()
                    val deckName = it.getString(nameIndex)

                    decks.add(DeckInfo(deckId, deckName))
                }
            }
        return decks
    }
}