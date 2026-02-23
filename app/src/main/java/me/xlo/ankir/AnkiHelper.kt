package me.xlo.ankir

import Card
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.ichi2.anki.FlashCardsContract
import com.ichi2.anki.api.AddContentApi

class AnkiHelper(
    context : Context
) {
    data class DeckInfo(
        val deckId: String,
        val deckName: String
    )
    val TAG = "AnkiR"
    val mContentResolver: ContentResolver = context.contentResolver
    val mApi = AddContentApi(context)
    fun getReviewInfo(did : String) : List<Pair<String, String>> {
        val arr = mutableListOf<Pair<String, String>>()
        val deckName = mApi.getDeckName(did.toLong())
        var noteID : String //key
        var cardOrd : String //value

        val cursor = mContentResolver.query(FlashCardsContract.ReviewInfo.CONTENT_URI,
            arrayOf(FlashCardsContract.ReviewInfo.NOTE_ID, FlashCardsContract.ReviewInfo.CARD_ORD),
            "limit=?, deckID=?",
            arrayOf("500",did),
            null)
        cursor?.use{
            val nid = cursor.getColumnIndex(FlashCardsContract.ReviewInfo.NOTE_ID)
            val cid = cursor.getColumnIndex(FlashCardsContract.ReviewInfo.CARD_ORD)
            while (cursor.moveToNext()) {
                noteID = cursor.getLong(nid).toString()
                cardOrd = cursor.getInt(cid).toString()
                arr.add(Pair(noteID,cardOrd))
            }
        }
        Log.i(TAG,"Get review info of deck $deckName total:${arr.size}")
        return arr
    }
    fun getCard(nid : String,cord : String) : Card? {
        val proj = arrayOf(
//            FlashCardsContract.Card.CARD_NAME,
            FlashCardsContract.Card.DECK_ID,
            FlashCardsContract.Card.ANSWER_PURE,
            FlashCardsContract.Card.QUESTION_SIMPLE
        )
        val noteUri = Uri.withAppendedPath(FlashCardsContract.Note.CONTENT_URI,nid)
        val cardsUri = Uri.withAppendedPath(noteUri,"cards")
        val cardUri = Uri.withAppendedPath(cardsUri,cord)
        val cursor = mContentResolver.query(cardUri,proj,null,null,null)
        cursor?.use{
            if (it.moveToFirst()) {
                return Card(
                    nid,
                    cord,
                    it.getString(0) ?: "",
                    it.getString(1) ?: "",
                    it.getString(2) ?: ""
                )
            }
        }
        Log.e(TAG,"Card($cord) NOT FOUND")
        return null
    }
//    fun getDeckID(DeckName : String) : String {
//        val DeckList = mApi.deckList
//        DeckList.forEach {
//            if(it.value == DeckName) {
//                Log.i(TAG,"Get deck id of $DeckName")
//                return it.key.toString()
//            }
//        }
//        Log.e(TAG,"Deck $DeckName NOT FOUND")
//        return "-1"
//    }
    fun getFilteredDecks(filter : String?): List<DeckInfo> {
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

            if(filter.isNullOrBlank()) {
                while (it.moveToNext()) {
                    val deckId = it.getLong(idIndex).toString()
                    val deckName = it.getString(nameIndex)
                    if(!deckName.contains("::")) {
                        decks.add(DeckInfo(deckId, deckName))
                        Log.i(TAG,"Add $deckName to decks list")
                    }
                }
            } else {
                while (it.moveToNext()) {
                    val deckId = it.getLong(idIndex).toString()
                    val deckName = it.getString(nameIndex)
                    if(filter == deckName) {
                        decks.add(DeckInfo(deckId,deckName))
                    }
                }
            }
        }
        return decks
    }
    fun getFilteredReviewCards(filterDeck : String?) : MutableList<Card> {
        val decks = getFilteredDecks(filterDeck)
        val cards = mutableListOf<Card>()
        for(deck in decks) {
            val review = getReviewInfo(deck.deckId)
            for ((nid, cord) in review) {
                val card = getCard(nid, cord)
                if (card != null) {
                    cards.add(card)
                } else {
                    Log.e(TAG,"Card $nid is null")
                }
            }
        }
        return cards
    }
}
