package com.example.nasaimage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AstronomyImageList(this) // Pass context to the function
                }
            }
        }
    }
}

@Parcelize
data class NASAImage(val title: String, val url: String) : Parcelable

@Composable
fun AstronomyImageList(context: Context) {
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var images by rememberSaveable { mutableStateOf<List<NASAImage>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(false) } // Initialize as false
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Start Date") },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("End Date") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                    loading = true // Set loading to true when button is clicked
                    coroutineScope.launch {
                        val newImages = fetchLatestImages(context, startDate, endDate)
                        images = newImages
                        loading = false // Set loading to false after images are fetched
                        // Save only the displayed images to internal storage
                        saveImagesToInternalStorage(context, images.takeLast(5))

                    }
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Fetch Images")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            LoadingScreen()
        } else {
            DisplayImageList(images = images)
        }
    }
}

private suspend fun fetchLatestImages(context: Context, startDate: String, endDate: String): List<NASAImage> {
    try {
        val apiKey = "oOUSTZGFdvatm1MEf8NG0Wvo3T38GUcI8vul3lrl"
        val url = "https://api.nasa.gov/planetary/apod?api_key=$apiKey&start_date=$startDate&end_date=$endDate"

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }

        if (!response.isSuccessful) {
            throw IOException("Unexpected response code: ${response.code()}")
        }

        val responseData = response.body()?.string() ?: throw IOException("Empty response body")
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

        return newImages.reversed()
    } catch (e: IOException) {
        // Handle IOException here
        Log.e("FetchImages", "Error fetching images: ${e.message}", e)
        return emptyList()
    }
}

private suspend fun saveImagesToInternalStorage(context: Context, images: List<NASAImage>) {
    withContext(Dispatchers.IO) {
        for (image in images) {
            try {
                val inputStream = URL(image.url).openStream()
                val file = File(context.filesDir, "${image.title}.jpg")
                val outputStream = FileOutputStream(file)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                Log.e("SaveImages", "Error saving image: ${e.message}", e)
            }
        }
    }
}

@Composable
fun DisplayImageList(images: List<NASAImage>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
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
                val file = File(context.filesDir, "${image.title}.jpg")

                // Create an intent to view the image using an external viewer
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to read the content URI
                }

                // Start activity with the intent
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
            style = MaterialTheme.typography.overline,
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

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}