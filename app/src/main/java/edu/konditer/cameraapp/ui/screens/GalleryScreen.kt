package edu.konditer.cameraapp.ui.screens
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.exifinterface.media.ExifInterface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import edu.konditer.cameraapp.ui.theme.CameraAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
data class MediaItem(
    val uri: android.net.Uri,
    val type: MediaType,
    val dateTaken: Long,
    val id: Long
) {
    enum class MediaType {
        IMAGE, VIDEO
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToVideo: () -> Unit,
    onDeleteItem: (MediaItem) -> Unit,
    onUpdateStatusBar: (Boolean) -> Unit = {}, 
) {
    val context = LocalContext.current
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(selectedItem) {
        onUpdateStatusBar(selectedItem != null)
    }
    LaunchedEffect(Unit) {
        mediaItems = loadMediaItems(context)
        isLoading = false
    }
    CameraAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (selectedItem != null) {
                FullscreenMediaViewer(
                    item = selectedItem!!,
                    allItems = mediaItems,
                    onDismiss = { selectedItem = null },
                    onDelete = { item ->
                        onDeleteItem(item)
                        mediaItems = mediaItems.filter { it.id != item.id }
                        if (selectedItem?.id == item.id) {
                            val currentIndex = mediaItems.indexOfFirst { it.id == item.id }
                            selectedItem = if (currentIndex >= 0 && currentIndex < mediaItems.size) {
                                mediaItems[currentIndex]
                            } else if (mediaItems.isNotEmpty()) {
                                mediaItems[0]
                            } else {
                                null
                            }
                        }
                    }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Галерея",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Назад"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToCamera) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Камера"
                                )
                            }
                            IconButton(onClick = onNavigateToVideo) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Видео"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        windowInsets = WindowInsets.statusBars
                    )
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (mediaItems.isEmpty()) {
                        EmptyGalleryView(
                            onNavigateToCamera = onNavigateToCamera,
                            onNavigateToVideo = onNavigateToVideo
                        )
                    } else {
                        MediaGrid(
                            items = mediaItems,
                            onItemClick = { selectedItem = it }
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun EmptyGalleryView(
    onNavigateToCamera: () -> Unit,
    onNavigateToVideo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Галерея пуста",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Создайте свои первые фото или видео",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateToCamera,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Снять фото")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onNavigateToVideo,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Записать видео")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaGrid(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { item ->
            MediaThumbnail(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}
@Composable
private fun MediaThumbnail(
    item: MediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(item.id) {
        thumbnail = withContext(Dispatchers.IO) {
            loadThumbnail(context, item)
        }
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        if (thumbnail != null) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageBitmap(thumbnail)
                    }
                },
                update = { view ->
                    view.setImageBitmap(thumbnail)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = if (item.type == MediaItem.MediaType.IMAGE) {
                    Icons.Default.Photo
                } else {
                    Icons.Default.Videocam
                },
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .padding(4.dp),
                tint = Color.White
            )
        }
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
        ) {
            Text(
                text = formatDate(item.dateTaken),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullscreenMediaViewer(
    item: MediaItem,
    allItems: List<MediaItem>,
    onDismiss: () -> Unit,
    onDelete: (MediaItem) -> Unit
) {
    var currentIndex by remember { mutableStateOf(allItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)) }
    val currentItem = remember(currentIndex, allItems) {
        if (currentIndex in allItems.indices) allItems[currentIndex] else null
    }
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (currentItem != null) {
            when (currentItem.type) {
                MediaItem.MediaType.IMAGE -> {
                    FullscreenImageView(currentItem.uri)
                }
                MediaItem.MediaType.VIDEO -> {
                    FullscreenVideoView(currentItem.uri)
                }
            }
        }
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        currentItem?.let { onDelete(it) }
                        if (currentIndex < allItems.size - 1) {
                            currentIndex++
                        } else if (currentIndex > 0) {
                            currentIndex--
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}
@Composable
private fun FullscreenImageView(uri: android.net.Uri) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val originalBitmap = BitmapFactory.decodeStream(input)
                    applyExifOrientation(uri, originalBitmap, context)
                }
            } catch (e: IOException) {
                null
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageBitmap(bitmap)
                    }
                },
                update = { view ->
                    view.setImageBitmap(bitmap)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
@Composable
private fun FullscreenVideoView(uri: android.net.Uri) {
    val context = LocalContext.current
    var videoSize by remember(uri) { mutableStateOf<Size?>(null) }
    LaunchedEffect(uri) {
        videoSize = withContext(Dispatchers.IO) {
            getVideoSize(uri, context)
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setVideoURI(uri)
                    setOnPreparedListener { 
                        it.isLooping = true
                        it.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                    }
                    start()
                }
            },
            update = { view ->
                view.setVideoURI(uri)
                view.start()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
private suspend fun loadMediaItems(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<MediaItem>()
    val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val imageProjection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DISPLAY_NAME
    )
    val imageSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    } else {
        null
    }
    val imageSelectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf("%Pictures/CameraApp%")
    } else {
        null
    }
    val imageSortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    context.contentResolver.query(
        imageUri,
        imageProjection,
        imageSelection,
        imageSelectionArgs,
        imageSortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val dateTaken = cursor.getLong(dateColumn)
            val uri = ContentUris.withAppendedId(imageUri, id)
            items.add(MediaItem(uri, MediaItem.MediaType.IMAGE, dateTaken, id))
        }
    }
    val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val videoProjection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DATE_TAKEN,
        MediaStore.Video.Media.DISPLAY_NAME
    )
    val videoSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
    } else {
        null
    }
    val videoSelectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf("%Movies/CameraApp%")
    } else {
        null
    }
    val videoSortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"
    context.contentResolver.query(
        videoUri,
        videoProjection,
        videoSelection,
        videoSelectionArgs,
        videoSortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val dateTaken = cursor.getLong(dateColumn)
            val uri = ContentUris.withAppendedId(videoUri, id)
            items.add(MediaItem(uri, MediaItem.MediaType.VIDEO, dateTaken, id))
        }
    }
    items.sortedByDescending { it.dateTaken }
}
private suspend fun loadThumbnail(context: Context, item: MediaItem): Bitmap? = withContext(Dispatchers.IO) {
    try {
        when (item.type) {
            MediaItem.MediaType.IMAGE -> {
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    val originalBitmap = BitmapFactory.decodeStream(input)
                    val orientedBitmap = applyExifOrientation(item.uri, originalBitmap, context)
                    val size = 300
                    Bitmap.createScaledBitmap(orientedBitmap ?: originalBitmap, size, size, true)
                }
            }
            MediaItem.MediaType.VIDEO -> {
                val thumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val videoId = item.id
                    val thumbnailUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        videoId
                    )
                    context.contentResolver.loadThumbnail(
                        thumbnailUri,
                        android.util.Size(300, 300),
                        null
                    )
                } else {
                    ThumbnailUtils.createVideoThumbnail(
                        item.uri.path ?: "",
                        MediaStore.Video.Thumbnails.MINI_KIND
                    )
                }
                thumbnail?.let { bitmap ->
                    val size = minOf(bitmap.width, bitmap.height)
                    val x = (bitmap.width - size) / 2
                    val y = (bitmap.height - size) / 2
                    Bitmap.createBitmap(bitmap, x, y, size, size)
                }
            }
        }
    } catch (e: Exception) {
        null
    }
}
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
private fun applyExifOrientation(uri: Uri, bitmap: Bitmap?, context: Context): Bitmap? {
    if (bitmap == null) return null
    return try {
        val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap 
        }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        bitmap 
    }
}
private suspend fun getVideoSize(uri: Uri, context: Context): Size? = withContext(Dispatchers.IO) {
    var retriever: MediaMetadataRetriever? = null
    try {
        retriever = MediaMetadataRetriever().apply {
            setDataSource(context, uri)
        }
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        if (width != null && height != null && width > 0 && height > 0) {
            Size(width, height)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    } finally {
        retriever?.release()
    }
}