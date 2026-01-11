package edu.konditer.cameraapp.ui.screens
import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.konditer.cameraapp.ui.theme.CameraAppTheme
@Composable
fun PermissionsScreen(
    missingPermissions: List<String>,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    CameraAppTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Необходимы разрешения",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Для работы приложения необходимо предоставить следующие разрешения:",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                missingPermissions.forEach { permission ->
                    PermissionItem(
                        permission = permission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Открыть настройки",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}
@Composable
private fun PermissionItem(
    permission: String,
    modifier: Modifier = Modifier
) {
    val (icon, title, description) = when (permission) {
        Manifest.permission.CAMERA -> Triple(
            Icons.Default.CameraAlt,
            "Камера",
            "Необходимо для съёмки фотографий и видео"
        )
        Manifest.permission.RECORD_AUDIO -> Triple(
            Icons.Default.Mic,
            "Микрофон",
            "Необходимо для записи звука при съёмке видео"
        )
        Manifest.permission.READ_MEDIA_IMAGES -> Triple(
            Icons.Default.Folder,
            "Доступ к изображениям",
            "Необходимо для сохранения фотографий"
        )
        Manifest.permission.READ_MEDIA_VIDEO -> Triple(
            Icons.Default.Folder,
            "Доступ к видео",
            "Необходимо для сохранения видео"
        )
        Manifest.permission.WRITE_EXTERNAL_STORAGE -> Triple(
            Icons.Default.Folder,
            "Запись файлов",
            "Необходимо для сохранения фотографий и видео"
        )
        else -> Triple(
            Icons.Default.Folder,
            permission,
            "Необходимо для работы приложения"
        )
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
