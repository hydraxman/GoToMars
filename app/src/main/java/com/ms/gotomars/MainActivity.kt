package com.ms.gotomars

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import com.ms.gotomars.ui.theme.GoToMarsTheme
import android.view.MotionEvent
import android.view.View
import android.view.ScaleGestureDetector

class MainActivity : ComponentActivity() {
    private var glView: GLSurfaceView? = null
    private lateinit var renderer: SolarSystemRenderer

    // Track last touch for deltas
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private lateinit var scaleDetector: ScaleGestureDetector
    private var scalingInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create GLSurfaceView and renderer
        renderer = SolarSystemRenderer(this)

        // Scale detector for pinch-to-zoom
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                scalingInProgress = true
                return true
            }
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                scalingInProgress = false
            }
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val sf = detector.scaleFactor
                glView?.queueEvent { renderer.onScale(sf) }
                return true
            }
        })

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            // Ensure we have a depth buffer for proper 3D rendering
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setPreserveEGLContextOnPause(true)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    // Always let the scale detector inspect the event
                    scaleDetector.onTouchEvent(event)

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            lastTouchX = event.x
                            lastTouchY = event.y
                            isDragging = true
                            return true
                        }
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            // Multi-touch starts; pause dragging
                            isDragging = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!scalingInProgress && isDragging) {
                                val x = event.x
                                val y = event.y
                                val dx = x - lastTouchX
                                val dy = y - lastTouchY
                                lastTouchX = x
                                lastTouchY = y
                                // Forward to GL thread
                                this@apply.queueEvent { renderer.onDrag(dx, dy) }
                                return true
                            }
                            return scalingInProgress
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            isDragging = false
                            scalingInProgress = false
                            return true
                        }
                    }
                    return false
                }
            })
        }

        setContent {
            GoToMarsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GLHost(
                        glView,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        glView?.onResume()
    }

    override fun onPause() {
        glView?.onPause()
        super.onPause()
    }
}

@Composable
private fun GLHost(glView: GLSurfaceView?, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { glView ?: GLSurfaceView(it).apply { setEGLContextClientVersion(2) } }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPlaceholder() {
    GoToMarsTheme { }
}
