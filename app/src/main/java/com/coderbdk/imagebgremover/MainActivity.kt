package com.coderbdk.imagebgremover

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.coderbdk.imagebgremover.ui.theme.ImageBgRemoverTheme
import androidx.core.graphics.scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        OpenCVUtils.init(this)
        setContent {
            ImageBgRemoverTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainContent(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var inputBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var outputBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var revealFraction by remember { mutableFloatStateOf(0f) }
    val coroutine = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it)

            stream?.use {
                val bitmap = BitmapFactory.decodeStream(stream).scale(300,300)
                inputBitmap = bitmap
                outputBitmap?.recycle()
                outputBitmap = null
            }

            inputBitmap?.let {
                coroutine.launch {
                    loading = true
                    outputBitmap = withContext(Dispatchers.Default) {
                        OpenCVUtils.removeImageBackground(it)
                    }
                    loading = false

                    revealFraction = 0f
                    delay(100)
                    revealFraction = 1f
                }
            }
        }
    }

    if (loading) {
        Dialog(onDismissRequest = {}) {
            Box(
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = CircleShape
                    )
                    .padding(8.dp)
            ) {
                CircularProgressIndicator()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        ToolMenu(
            outputBitmap = outputBitmap,
            revealFraction = revealFraction,
            onPickImageClick = {
                launcher.launch("image/*")
            },
            onRevealFractionToggle = {
                revealFraction = it
            })

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(0.3f))
        ) {
            if (inputBitmap == null) {
                Image(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Input",
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            launcher.launch("image/*")
                        }
                        .alpha(0.1f)

                )
            }
            inputBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Input",
                    modifier = Modifier.matchParentSize()
                )

            }

            outputBitmap?.let { output ->
                val animatedFraction by animateFloatAsState(
                    targetValue = revealFraction,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RectangleShape)
                ) {
                    Image(
                        bitmap = output.asImageBitmap(),
                        contentDescription = "Output",
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                clip = true
                                shape = RectangleShape
                            }
                            .drawWithContent {
                                clipRect(right = size.width * animatedFraction) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
            }
        }

    }
}


@Composable
fun ToolMenu(
    outputBitmap: Bitmap?,
    revealFraction: Float,
    onPickImageClick: () -> Unit,
    onRevealFractionToggle: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.LightGray.copy(0.3f)),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onPickImageClick() }) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Pick Image"
            )
        }
        if (outputBitmap != null) {
            IconButton(
                onClick = {
                    onRevealFractionToggle(if (revealFraction == 0f) 1f else 0f)
                },
            ) {
                Icon(
                    imageVector = if (revealFraction == 0f) Icons.Default.Flip else Icons.Default.Flip,
                    contentDescription = if (revealFraction == 0f) "Reveal" else "Hide",
                    modifier = Modifier.graphicsLayer(
                        scaleX = if (revealFraction == 0f) -1f else 1f,
                        scaleY = 1f
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    ImageBgRemoverTheme {
        ToolMenu(
            outputBitmap = createBitmap(20, 20),
            revealFraction = 0f,
            onPickImageClick = {},
            onRevealFractionToggle = {}
        )
    }
}