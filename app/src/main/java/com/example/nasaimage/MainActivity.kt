
package com.example.nasaimage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import androidx.compose.ui.Alignment
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.FileProvider
import kotlinx.parcelize.Parcelize
import okhttp3.Response
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
fun AstronomyImageList(context: Context) { // Receive context as a parameter
    var images by rememberSaveable { mutableStateOf<List<NASAImage>>(emptyList()) }
    var loading by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(true) {
        fetchLatestImages(context) { newImages ->
            images = newImages
            loading = false
        }
    }

    if (loading) {
        // Display a loading indicator while images are being fetched
        LoadingScreen()
    } else {
        DisplayImageList(images = images)
    }
}

@Composable
fun DisplayImageList(images: List<NASAImage>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        Text(
            text = "Filter by Date : 2013-05-20 - 2013-06-24",
            style = MaterialTheme.typography.h6.copy(color = Color.Blue),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp) // Adjust vertical padding between items
        ) {
            items(images) { image ->
                ImageListItem(image = image, context = LocalContext.current)
            }
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

                // Create a content URI using FileProvider
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                // Create an intent to view the content URI
                val intent = Intent(Intent.ACTION_VIEW).apply {
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

private suspend fun fetchLatestImages(context: Context, onImagesFetched: (List<NASAImage>) -> Unit) {
    val imagesDirectory = context.filesDir // Directory where images are saved

    // Check if images exist in the internal storage
    val localImages = mutableListOf<NASAImage>()
    imagesDirectory.listFiles()?.forEach { file ->
        // Assuming each image file name corresponds to its title
        val title = file.nameWithoutExtension
        val url = file.toURI().toString() // Convert file path to URL
        localImages.add(NASAImage(title, url))
    }

    if (localImages.isNotEmpty()) {
        // Images exist locally, so fetch them from the internal storage
        onImagesFetched(localImages)
    } else {
        // Images don't exist locally, fetch them from the remote server
        val apiKey = "oOUSTZGFdvatm1MEf8NG0Wvo3T38GUcI8vul3lrl"
        val start_date = "2013-05-20"
        val end_date = "2013-06-24"
        val url = "https://api.nasa.gov/planetary/apod?api_key=$apiKey&start_date=$start_date&end_date=$end_date"

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        var retries = 3
        var response: Response? = null
        while (retries > 0) {
            try {
                withContext(Dispatchers.IO) {
                    response = client.newCall(request).execute()
                }
                if (response?.isSuccessful == true) {
                    // Handle successful response
                    val responseData = response?.body()?.string() // Read the response body as a string
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

                    // Save the images to internal storage
                    saveImagesToInternalStorage(context, newImages)

                    onImagesFetched(newImages.reversed()) // Reverse the list to display in correct order
                    break // Exit the loop on successful response
                } else {
                    // Handle unsuccessful response
                }
            } catch (e: Exception) {
                retries--
                if (retries == 0) {
                    // Handle retry exhaustion
                }
            } finally {
                response?.close()
            }
        }
    }
}

private suspend fun saveImagesToInternalStorage(context: Context, images: List<NASAImage>) {
    withContext(Dispatchers.IO) { // Wrap the operation inside a coroutine
        // Iterate through the list of images and save them to internal storage
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
                e.printStackTrace()
            }
        }
    }
}