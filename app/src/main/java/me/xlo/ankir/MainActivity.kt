package me.xlo.ankir

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.xlo.ankir.ui.theme.AnkiRTheme

class MainActivity : ComponentActivity() {

    val mHelper by lazy { AnkiHelper(this) }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        //request permission for ankidroid
        requestPermission()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnkiRTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Text("AnkiR")
                        }
                    )
                    ReviewScreen(getReviewCards())
                }
            }
        }
    }
    fun getReviewCards() : MutableList<ACard> {
        val IDs = mHelper.getReviewInfo()
        val Cards = mutableListOf<ACard>()
        IDs.forEach {
            val card = mHelper.getCard(it.first,it.second)
            if(card != null) {
                Cards.add(card)
            }
        }
        return Cards
    }
    fun requestPermission() {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Please allow the permission to access AnkiDroid", Toast.LENGTH_LONG).show()
            }
        }
        permissionLauncher.launch("com.ichi2.anki.permission.READ_WRITE_DATABASE")
    }
}
@Composable
fun ReviewScreen(list : MutableList<ACard>) {
    val context = LocalContext.current
    var CardIndex by remember { mutableStateOf(0) }
    var Show by remember { mutableStateOf(list[CardIndex].mAnswer) }
    Column(modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(Show)
        }
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = {
                    if(CardIndex >= (list.size - 1)) {
                        Toast.makeText(context,"复习完毕",Toast.LENGTH_LONG)
                            .show()
                    } else {
                        CardIndex = CardIndex + 1
                        Show = list[CardIndex].mAnswer
                    }

                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text("Next")
            }
            Button(
                onClick = {
                    Show = list[CardIndex].mQuestion
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text("Q")
            }
        }
    }
}