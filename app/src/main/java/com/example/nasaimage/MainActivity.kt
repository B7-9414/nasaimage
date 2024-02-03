package com.example.nasaimage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AstronomyImageList()
                }
            }
        }
    }
}

data class NASAImage(val title: String, val url: String)

@Composable
fun AstronomyImageList() {
    var images by rememberSaveable { mutableStateOf<List<NASAImage>>(emptyList()) }

    LaunchedEffect(true) {
        val apiKey = "oOUSTZGFdvatm1MEf8NG0Wvo3T38GUcI8vul3lrl"
        val url = "https://api.nasa.gov/planetary/apod?api_key=$apiKey&count=5"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }

        if (response.isSuccessful) {
            val responseData = response.body()?.string() // Read the response body as a string
            val jsonArray = JSONArray(responseData)

            val newImages = mutableListOf<NASAImage>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val title = jsonObject.getString("title")
                val imageUrl = jsonObject.getString("url")
                newImages.add(NASAImage(title, imageUrl))
            }

            images = newImages.toList()
        } else {
            // Handle error
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp) // Adjust vertical padding between items
    ) {
        items(images) { image ->
            ImageListItem(image = image, context = LocalContext.current)
        }
    }
}

@Composable
fun ImageListItem(image: NASAImage, context: Context) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(image.url))
                context.startActivity(intent)
            }
    ) {
        Text(
            text = "Title: ${image.title}",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "URL: ${image.url}",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        NetworkImage(
            url = image.url,
            contentDescription = image.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

