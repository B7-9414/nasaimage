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
    var images by remember { mutableStateOf<List<NASAImage>>(emptyList()) }

    LaunchedEffect(true) {
        val apiKey = "oOUSTZGFdvatm1MEf8NG0Wvo3T38GUcI8vul3lrl"
        var start_date = "2013-05-20"
        var end_date = "2013-06-24"
        val url = "https://api.nasa.gov/planetary/apod?api_key=$apiKey&start_date=$start_date&end_date=$end_date"

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

            // Start iterating from the end of the array and move backwards
            for (i in jsonArray.length() - 1 downTo 0) {
                val jsonObject = jsonArray.getJSONObject(i)
                val title = jsonObject.getString("title")
                val imageUrl = jsonObject.getString("url")
                newImages.add(NASAImage(title, imageUrl))

                // Stop iterating if we have collected 5 images
                if (newImages.size == 5) {
                    break
                }
            }

            // Reverse the list to maintain the correct order (from latest to oldest)
            images = newImages.reversed()
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
            text = "Filter by Date : 2013-05-20 - 2013-06-24",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 100.dp)
        )
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

