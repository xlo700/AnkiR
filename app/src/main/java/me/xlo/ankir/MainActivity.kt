package me.xlo.ankir

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.xlo.ankir.ui.theme.AnkiRTheme
import Card
import android.content.Context
import android.util.Log

class MainActivity : ComponentActivity() {

    val mHelper by lazy { AnkiHelper(this) }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnkiRTheme {
                var isPermissionAllowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(this.applicationContext,"com.ichi2.anki.permission.READ_WRITE_DATABASE") == PackageManager.PERMISSION_GRANTED) }
                val context = LocalContext.current
                var isSave by remember { mutableStateOf(context.getSharedPreferences("config", MODE_PRIVATE).getBoolean("is_save", false)) }
                val isFinishNew = this.getSharedPreferences("config",MODE_PRIVATE).getBoolean("finish_new",false)

                //var cardIndex by remember { mutableStateOf(this.getSharedPreferences("config",MODE_PRIVATE).getInt("card_index",0)) }
                var cardIndex =this.getSharedPreferences("config",MODE_PRIVATE).getInt("card_index",0)
                var mFilterDeck by remember { mutableStateOf(this.getSharedPreferences("config",MODE_PRIVATE).getString("filter",null)) }
                var replaceAnswer by remember { mutableStateOf(this.getSharedPreferences("config",MODE_PRIVATE).getString("replace_answer",null)) }
                if(replaceAnswer.isNullOrBlank()) replaceAnswer = "(?!)"

                val mSQLHelper = SQLHelper(this,"ankir",2)

                var list by remember { mutableStateOf<List<Card>?>(null) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->

                    if (!isGranted) {
                        Toast.makeText(context, "Permission denied", Toast.LENGTH_LONG).show()
                    }
                    isPermissionAllowed = isGranted
                }
                Scaffold(modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("AnkiR")
                            },
                            actions = {
                                FilterBtn(
                                    deckChange = {
                                        mFilterDeck = context.getSharedPreferences("config", MODE_PRIVATE).getString("filter", null)
                                        mSQLHelper.clear()
                                        isSave = false
                                        list = null
                                    },
                                    characterChange = {
                                        replaceAnswer = context.getSharedPreferences("config",MODE_PRIVATE).getString("replace_answer",null)
                                    },
                                    clearDatabase = {
                                        mSQLHelper.clear()
                                        isSave = false
                                        list = null
                                        Log.i("AnkiR","clear database")
                                    }
                                )
                            }
                        )
                    }
                ) {paddingValues ->
                    LaunchedEffect(isPermissionAllowed,mFilterDeck,isSave,list) {
                        if(isPermissionAllowed) {
                            if (!isSave) {
                                withContext(Dispatchers.IO) {
                                    list = mHelper.getFilteredReviewCards(mFilterDeck).shuffled()
                                    mSQLHelper.clear()
                                    for(l in list) {
                                        mSQLHelper.addCard(l,mSQLHelper.CARD_TABLE)
                                    }
                                    context.getSharedPreferences("config",MODE_PRIVATE).edit {
                                        putBoolean("is_save",true)
                                    }
                                    cardIndex = context.getSharedPreferences("config",MODE_PRIVATE).getInt("card_index",0)
                                    isSave = true
                                }
                            } else {
                                if(!isFinishNew) {
                                    list = mSQLHelper.queryCards(mSQLHelper.CARD_TABLE)
                                    Log.i("AnkiR","Get cards from card database")
                                } else {
                                    list = mSQLHelper.queryCards(mSQLHelper.REVIEW_TABLE)
                                    Log.i("AnkiR","Get cards from review database")
                                }
                            }
                        }
                    }
                    when(isPermissionAllowed) {
                        false -> {
                            permissionLauncher.launch("com.ichi2.anki.permission.READ_WRITE_DATABASE")
                            NoCard()
                        }
                        true -> {
                            if(list == null) {
                                Box(modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Text("Loading")
                                    }
                                }
                            } else if(list!!.isEmpty()) {
                                NoCard()
                            } else {
                                ReviewScreen(
                                    list!!,
                                    modifier = Modifier.padding(paddingValues),
                                    replaceAnswer = replaceAnswer!!,
                                    cardIndexArg = cardIndex,
                                    finishNew = {
                                        list = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ReviewScreen(list : List<Card>, modifier : Modifier, replaceAnswer : String,cardIndexArg : Int,finishNew : () -> Unit) {

    var isQuestion by remember { mutableStateOf(false) }
    val context = LocalContext.current
    //var cardIndex by remember { mutableIntStateOf(0) }
    var cardIndex by remember { mutableStateOf(cardIndexArg) }
    var show by remember { mutableStateOf(list[cardIndex].mAnswer.replace(replaceAnswer.toRegex(), "")) }
    Column(modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween) {
        Text("Total:${cardIndex + 1}/${list.size}", modifier = Modifier)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            HtmlWebView(htmlContent = show)
        }
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            if(!isQuestion) {
                Button(
                    onClick = {
                        show = list[cardIndex].mQuestion
                        isQuestion = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text("Show question")
                }
            } else {
                Button(
                    onClick = {
                        if(cardIndex >= (list.size - 1)) {
                            Toast.makeText(context,"You've completed today's tasks!",Toast.LENGTH_LONG)
                                .show()
                        } else {
                            cardIndex = cardIndex + 1
                            show = list[cardIndex].mAnswer.replace(replaceAnswer.toRegex(), "")
                            context.getSharedPreferences("config",MODE_PRIVATE).edit {
                                putInt("card_index",cardIndex)
                            }
                        }
                        isQuestion = false
                    }, modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text("Next")
                }
                Button(
                    onClick = {
                        if(cardIndex >= (list.size - 1)) {
                            Toast.makeText(context,"You've completed today's new tasks!",Toast.LENGTH_LONG)
                                .show()
                            context.getSharedPreferences("config", MODE_PRIVATE).edit {
                                putBoolean("finish_new", true)
                            }
                            addReviewCard(context.applicationContext,list[cardIndex])
                            finishNew()
                            context.getSharedPreferences("config", MODE_PRIVATE).edit {
                                putInt("card_index", 0)
                            }
                            cardIndex = 0
                            //TODO
                        } else {
                            cardIndex = cardIndex + 1
                            show = list[cardIndex].mAnswer.replace(replaceAnswer.toRegex(), "")
                            context.getSharedPreferences("config",MODE_PRIVATE).edit {
                                putInt("card_index",cardIndex)
                            }
                            addReviewCard(context.applicationContext,list[cardIndex])
                            isQuestion = false
                        }
                    }, modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text("Forget")
                }
            }

//            Button(
//                onClick = {
//                    if(cardIndex >= (list.size - 1)) {
//                        Toast.makeText(context,"You've completed today's tasks!",Toast.LENGTH_LONG)
//                            .show()
//                    } else {
//                        cardIndex = cardIndex + 1
//                        show = list[cardIndex].mAnswer.replace(replaceAnswer.toRegex(), "")
//                        context.getSharedPreferences("config",MODE_PRIVATE).edit {
//                            putInt("card_index",cardIndex)
//                        }
//                    }
//                },
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(horizontal = 8.dp)
//            ) {
//                Text("Next")
//            }
//            Button(
//                onClick = {
//                    if(isQuestion) {
//                        show = list[cardIndex].mAnswer.replace(replaceAnswer.toRegex(), "")
//                        isQuestion = false
//                    } else {
//                        show = list[cardIndex].mQuestion
//                        isQuestion = true
//                    }
//                },
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(horizontal = 8.dp)
//            ) {
//                Text("Reverse")
//            }
        }
    }
}
@Composable
fun NoCard() {
    Box(modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("No cards or permission denied")
    }
}
@Composable
fun FilterBtn(deckChange : () -> Unit,characterChange : () -> Unit,clearDatabase : () -> Unit) {
    var state by remember { mutableStateOf(false) }
    if (state) FilterDialog(
        onDismiss = { isDeckChanged, isCharacterChanged ->
            state = false
            if (isDeckChanged) deckChange()
            if (isCharacterChanged) characterChange()
        },
        clearDatabase = clearDatabase)
    Text(
        "Setting",
        textAlign = TextAlign.Right,
        modifier = Modifier
            .clickable(
                enabled = true,
            ) {
                state = true
            }
    )
}
@Composable
fun FilterDialog(onDismiss : (isDeckChanged : Boolean,isCharacterChanged : Boolean) -> Unit,clearDatabase : () -> Unit) {
    var isDeckChanged = false
    var isCharacterChanged = false
    val mSharedPreferences = LocalContext.current.getSharedPreferences("config",MODE_PRIVATE)
    var filterDeck by remember { mutableStateOf(mSharedPreferences.getString("filter","") ?: "") }
    var replaceAnswer by remember { mutableStateOf(mSharedPreferences.getString("replace_answer","") ?: "") }
    Dialog(onDismissRequest = { onDismiss(isDeckChanged,isCharacterChanged) },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)) {
        Column {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = filterDeck,
                onValueChange = {
                    mSharedPreferences.edit {
                        putString("filter",it) }
                    filterDeck = it
                    isDeckChanged = true    //TODO
                },
                label = { Text("Filter cards by deck name:") },
                shape = RoundedCornerShape(10.dp)
            )
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = replaceAnswer,
                onValueChange = {
                    mSharedPreferences.edit {
                        putString("replace_answer",it) }
                    replaceAnswer = it
                    isCharacterChanged = true   //TODO
                },
                label = { Text("Replace specified character in answer") },
                shape = RoundedCornerShape(10.dp)
            )
            Button(
                onClick = {
                    onDismiss(isDeckChanged, isCharacterChanged)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm")
            }
            Button(
                onClick = {
                    clearDatabase()
                    onDismiss(isDeckChanged,isCharacterChanged)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Clear cached cards")
            }
        }
    }
}
@Composable
fun HtmlWebView(htmlContent: String) {
    val isDarkTheme = isSystemInDarkTheme()
    AndroidView(
        factory = { context ->
            WebView(context).apply {

                settings.javaScriptEnabled = true

                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false

                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            val backgroundColor = if (isDarkTheme) "#121212" else "#FFFFFF"
            val textColor = if (isDarkTheme) "#E0E0E0" else "#000000"
            val linkColor = if (isDarkTheme) "#BB86FC" else "#0000EE"
            val wrappedHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: sans-serif;
                            padding: 16px;
                            margin: 0;
                            font-size: 16px;
                            background-color: $backgroundColor;
                            color: $textColor;
                            line-height: 1.5;
                        }
                        a {
                            color: $linkColor;
                        }
                        img {
                            max-width: 100%;
                            height: auto;
                        }
                    </style>
                </head>
                <body>
                    $htmlContent
                </body>
                </html>
            """.trimIndent()

            webView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
        }
    )
}
fun addReviewCard(context : Context, card : Card) {
    SQLHelper(context,"ankir",2).apply {
        addCard(card,REVIEW_TABLE)
    }
}