//package com.example.nasaimage
//
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//
//
//@Composable
//fun AstronomyImageList(images: List<AstronomyImage>, onClick: (AstronomyImage) -> Unit) {
//    LazyColumn(modifier = Modifier.fillMaxSize()) {
//        items(images) { image ->
//            Thumbnail(image = image, onClick = onClick)
//        }
//    }
//}
//
//@Composable
//fun Thumbnail(image: AstronomyImage, onClick: (AstronomyImage) -> Unit) {
//    // Display thumbnail here
//    // You can use Coil to load the image from the URL
//}
//
//@Composable
//fun FullSizeImage(image: AstronomyImage) {
//    // Display full-size image here
//    // You can use Coil to load the image from the URL
//}
