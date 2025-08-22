package com.ms.gotomars

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.*
import kotlin.random.Random

class SolarSystemRenderer(private val context: Context) : GLSurfaceView.Renderer {
    // Simulation scaling and time
    private val KM_PER_UNIT = 10_000_000f // 10 million km per world unit
    private val TIME_SCALE_HOURS_PER_SEC = 24f // 1 sec = 24 hours of sim time

    // Planet data (mean orbital radius and sidereal period)
    private data class PlanetSpec(
        val name: String,
        val orbitRadiusKm: Float,
        val periodDays: Float,
        val color: FloatArray,
        val visualRadius: Float
    )

    private val planets = listOf(
        PlanetSpec("Mercury", 57_909_050f, 87.969f, floatArrayOf(0.8f, 0.8f, 0.7f, 1f), 0.18f),
        PlanetSpec("Venus", 108_208_000f, 224.701f, floatArrayOf(1f, 0.8f, 0.4f, 1f), 0.45f),
        PlanetSpec("Earth", 149_597_870f, 365.256f, floatArrayOf(0.2f, 0.7f, 1f, 1f), 0.6f),
        PlanetSpec("Mars", 227_939_200f, 686.971f, floatArrayOf(1f, 0.4f, 0.3f, 1f), 0.5f),
        PlanetSpec("Jupiter", 778_570_000f, 4332.59f, floatArrayOf(0.9f, 0.7f, 0.5f, 1f), 1.2f),
        PlanetSpec("Saturn", 1_433_530_000f, 10759.22f, floatArrayOf(0.9f, 0.8f, 0.6f, 1f), 1.1f),
        PlanetSpec("Uranus", 2_872_460_000f, 30685.4f, floatArrayOf(0.7f, 0.9f, 1f, 1f), 0.9f),
        PlanetSpec("Neptune", 4_495_060_000f, 60190f, floatArrayOf(0.5f, 0.7f, 1f, 1f), 0.9f)
    )
    private val earthIdx = planets.indexOfFirst { it.name == "Earth" }
    private val marsIdx = planets.indexOfFirst { it.name == "Mars" }

    private val orbitR = FloatArray(planets.size) { planets[it].orbitRadiusKm / KM_PER_UNIT }
    private val omegas = FloatArray(planets.size) { (2f * PI_F) / (planets[it].periodDays * 24f) }
    private val angles = FloatArray(planets.size) { Random.nextFloat() * 2f * PI_F }

    // Visual sizes (not to physical scale)
    private val sunRadius = 2.0f

    // Ships along Earth<->Mars route
    private val SHIP_SPEED_KMH = 100_000f
    private val shipSpeedUnitsPerHour = SHIP_SPEED_KMH / KM_PER_UNIT
    private val NUM_SHIPS = 24
    private val shipsU = FloatArray(NUM_SHIPS) { it / NUM_SHIPS.toFloat() }

    // Matrices
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    // GL program and attrib/uniforms
    private var program = 0
    private var aPosition = 0
    private var uMvp = 0
    private var uColor = 0
    private var uPointSize = 0

    // Meshes
    private lateinit var sphere: Mesh
    private lateinit var circleUnit: Polyline
    private lateinit var cube: Mesh

    // Starfield
    private lateinit var stars: FloatBuffer
    private var starCount = 0

    // Dynamic route polyline
    private var routePts = floatArrayOf()
    private var routeCum = floatArrayOf()
    private var routeLen = 0f

    // Timekeeping
    private var lastNanos = 0L

    // User orbit control (radians)
    @Volatile private var userYaw: Float = 0f
    @Volatile private var userPitch: Float = 0f // positive tilts up
    @Volatile private var zoomDistMul: Float = 1f // <1 = closer, >1 = farther

    // Camera billboard axes (world-space)
    private val camRight = FloatArray(3)
    private val camUp = FloatArray(3)

    // Data structures
    data class Mesh(
        val vertexBuffer: FloatBuffer,
        val indexBuffer: ShortBuffer,
        val vertexCount: Int,
        val indexCount: Int
    )
    data class Polyline(
        val vertexBuffer: FloatBuffer,
        val vertexCount: Int
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE) // render both sides -> full spheres visible

        program = buildProgram(VS, FS)
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        uMvp = GLES20.glGetUniformLocation(program, "uMVP")
        uColor = GLES20.glGetUniformLocation(program, "uColor")
        uPointSize = GLES20.glGetUniformLocation(program, "uPointSize")

        sphere = createSphere(1f, stacks = 48, slices = 64)
        circleUnit = createCircle(512)
        cube = createCube(1f)
        createStars(1500, 800f)

        Matrix.setLookAtM(view, 0, 0f, 14f, 160f, 0f, 0f, 0f, 0f, 1f, 0f)
        lastNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / max(1, height).toFloat()
        Matrix.perspectiveM(proj, 0, 60f, aspect, 0.1f, 3000f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Time step
        val now = System.nanoTime()
        val dtSec = if (lastNanos == 0L) 0f else (now - lastNanos) / 1_000_000_000f
        lastNanos = now
        val dtHours = dtSec * TIME_SCALE_HOURS_PER_SEC

        // Update planet angles and positions
        for (i in angles.indices) angles[i] = (angles[i] + omegas[i] * dtHours) % (2f * PI_F)
        val pos = Array(planets.size) { FloatArray(3) }
        for (i in planets.indices) {
            val r = orbitR[i]
            val a = angles[i]
            pos[i][0] = r * cosf(a)
            pos[i][1] = 0f
            pos[i][2] = r * sinf(a)
        }
        val earthPos = pos[earthIdx]
        val marsPos = pos[marsIdx]

        // Build route using quadratic Bézier (Earth -> control on Mars orbit -> Mars)
        buildRouteBezier(earthPos, marsPos)
        val du = if (routeLen > 1e-6f) (shipSpeedUnitsPerHour / routeLen) * dtHours else 0f
        for (i in 0 until NUM_SHIPS) shipsU[i] = (shipsU[i] + du) % 1f

        // Camera distances derived from Earth-Mars separation, but target Earth
        val dx = marsPos[0] - earthPos[0]
        val dz = marsPos[2] - earthPos[2]
        val dist = max(1e-4f, sqrtf(dx*dx + dz*dz))
        val back = 35f + dist * 1.7f
        val height = 12f + dist * 0.35f

        // Target Earth
        val targetY = 0f
        val baseTargetX = earthPos[0]
        val baseTargetZ = earthPos[2]
        val baseEyeX = baseTargetX
        val baseEyeY = height
        val baseEyeZ = baseTargetZ + back

        // Convert base vector to spherical and apply user yaw/pitch and zoom
        val vx0 = baseEyeX - baseTargetX
        val vy0 = baseEyeY - targetY
        val vz0 = baseEyeZ - baseTargetZ
        val baseRadius = max(1e-4f, sqrtf(vx0*vx0 + vy0*vy0 + vz0*vz0))
        val yaw0 = atan2f(vx0, vz0) // 0 faces +Z
        val pitch0 = atan2f(vy0, sqrtf(vx0*vx0 + vz0*vz0))
        val yaw = yaw0 + userYaw
        val minPitch = (-80f / 180f * PI_F)
        val maxPitch = (80f / 180f * PI_F)
        val pitch = clampf(pitch0 + userPitch, minPitch, maxPitch)
        val radius = baseRadius * clampf(zoomDistMul, 0.25f, 4f)
        val h = radius * cosf(pitch)
        val eyeX = baseTargetX + h * sinf(yaw)
        val eyeY = targetY + radius * sinf(pitch)
        val eyeZ = baseTargetZ + h * cosf(yaw)

        Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, baseTargetX, targetY, baseTargetZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)

        // Update camera billboard axes
        val fx = baseTargetX - eyeX
        val fy = targetY - eyeY
        val fz = baseTargetZ - eyeZ
        normalize3(fx, fy, fz, out = tmp3a)
        // right = normalize(cross(forward, worldUp))
        cross3(tmp3a[0], tmp3a[1], tmp3a[2], 0f, 1f, 0f, out = camRight)
        val rl = sqrtf(camRight[0]*camRight[0] + camRight[1]*camRight[1] + camRight[2]*camRight[2])
        if (rl < 1e-5f) { camRight[0]=1f; camRight[1]=0f; camRight[2]=0f } else { val inv=1f/rl; camRight[0]*=inv; camRight[1]*=inv; camRight[2]*=inv }
        // up = normalize(cross(right, forward))
        cross3(camRight[0], camRight[1], camRight[2], tmp3a[0], tmp3a[1], tmp3a[2], out = camUp)
        normalize3(camUp[0], camUp[1], camUp[2], out = camUp)

        // Clear and bind program
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        // Stars (no depth)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        drawPoints(stars, starCount, floatArrayOf(0.85f, 0.85f, 1f, 1f), 2f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Orbits (always visible)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        for (i in planets.indices) {
            val col = when (i) {
                earthIdx -> floatArrayOf(0.3f, 0.6f, 1f, 1f)
                marsIdx -> floatArrayOf(1f, 0.5f, 0.3f, 1f)
                else -> floatArrayOf(0.6f, 0.6f, 0.6f, 1f)
            }
            drawCircle(circleUnit, col, orbitR[i])
        }
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Sun
        drawSphere(sphere, floatArrayOf(0f, 0f, 0f), sunRadius, floatArrayOf(1f, 0.95f, 0.4f, 1f))

        // Planets
        for (i in planets.indices) {
            val sp = planets[i]
            drawSphere(sphere, pos[i], sp.visualRadius, sp.color)
            if (sp.name == "Saturn") {
                Matrix.setIdentityM(model, 0)
                Matrix.translateM(model, 0, pos[i][0], pos[i][1], pos[i][2])
                Matrix.scaleM(model, 0, sp.visualRadius * 3f, 1f, sp.visualRadius * 3f)
                Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
                GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
                GLES20.glUniform4fv(uColor, 1, floatArrayOf(0.9f, 0.8f, 0.6f, 0.7f), 0)
                GLES20.glEnableVertexAttribArray(aPosition)
                GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 3*4, circleUnit.vertexBuffer)
                GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, circleUnit.vertexCount)
                GLES20.glDisableVertexAttribArray(aPosition)
            }
            // Labels: initial letter beside sphere, billboarding to camera
            val initial = sp.name.first()
            val size = sp.visualRadius * 0.9f
            val offset = sp.visualRadius * 1.3f
            drawLetterBillboard(initial, pos[i], size, offset, floatArrayOf(1f,1f,1f,1f))
        }

        // Route polyline (always visible)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        drawPolyline(routePts, floatArrayOf(1f, 1f, 0.2f, 1f))
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Ships
        for (i in 0 until NUM_SHIPS) {
            val p = samplePath(shipsU[i])
            drawCube(cube, p, 0.2f, floatArrayOf(0.95f, 0.95f, 1f, 1f))
        }
    }

    // Drawing helpers
    private fun drawSphere(mesh: Mesh, position: FloatArray, radius: Float, color: FloatArray) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, position[0], position[1], position[2])
        Matrix.scaleM(model, 0, radius, radius, radius)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniform4fv(uColor, 1, color, 0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 3 * 4, mesh.vertexBuffer)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)
        GLES20.glDisableVertexAttribArray(aPosition)
    }

    private fun drawCube(mesh: Mesh, position: FloatArray, size: Float, color: FloatArray) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, position[0], position[1], position[2])
        Matrix.scaleM(model, 0, size, size, size)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniform4fv(uColor, 1, color, 0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 3 * 4, mesh.vertexBuffer)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)
        GLES20.glDisableVertexAttribArray(aPosition)
    }

    private fun drawCircle(poly: Polyline, color: FloatArray, scale: Float) {
        Matrix.setIdentityM(model, 0)
        Matrix.scaleM(model, 0, scale, 1f, scale)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniform4fv(uColor, 1, color, 0)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 3*4, poly.vertexBuffer)
        GLES20.glLineWidth(1f)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, poly.vertexCount)
        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun drawPolyline(points: FloatArray, color: FloatArray) {
        if (points.size < 6) return
        val vb = asFloatBuffer(points)
        Matrix.setIdentityM(model, 0)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniform4fv(uColor, 1, color, 0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 3 * 4, vb)
        GLES20.glLineWidth(2f)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, points.size / 3)
        GLES20.glDisableVertexAttribArray(aPosition)
    }

    private fun drawPoints(buffer: FloatBuffer, count: Int, color: FloatArray, pointSize: Float) {
        Matrix.setIdentityM(model, 0)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniform4fv(uColor, 1, color, 0)
        GLES20.glUniform1f(uPointSize, pointSize)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 3 * 4, buffer)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, count)
        GLES20.glDisableVertexAttribArray(aPosition)
    }

    // Geometry creation
    private fun createSphere(radius: Float, stacks: Int, slices: Int): Mesh {
        val verts = ArrayList<Float>()
        val indices = ArrayList<Short>()
        for (i in 0..stacks) {
            val v = i / stacks.toFloat()
            val phi = PI_F * v // 0..PI
            val y = cosf(phi)           // [-1..1]
            val r = sinf(phi)           // [0..1]
            for (j in 0..slices) {
                val u = j / slices.toFloat()
                val theta = (2f * PI_F) * u
                val x = r * cosf(theta)
                val z = r * sinf(theta)
                verts.add(x * radius)
                verts.add(y * radius)
                verts.add(z * radius)
            }
        }
        val cols = slices + 1
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val a = (i * cols + j).toShort()
                val b = ((i + 1) * cols + j).toShort()
                val c = ((i + 1) * cols + (j + 1)).toShort()
                val d = (i * cols + (j + 1)).toShort()
                indices.add(a); indices.add(b); indices.add(c)
                indices.add(a); indices.add(c); indices.add(d)
            }
        }
        return Mesh(asFloatBuffer(verts.toFloatArray()), asShortBuffer(indices.toShortArray()), verts.size/3, indices.size)
    }

    private fun createCircle(segments: Int): Polyline {
        val verts = FloatArray((segments + 1) * 3)
        var k = 0
        for (i in 0..segments) {
            val t = (2f * PI_F) * (i / segments.toFloat())
            verts[k++] = cosf(t)
            verts[k++] = 0f
            verts[k++] = sinf(t)
        }
        return Polyline(asFloatBuffer(verts), segments + 1)
    }

    private fun createCube(size: Float): Mesh {
        val s = size / 2f
        val verts = floatArrayOf(
            -s, -s, -s,  s, -s, -s,  s,  s, -s,  -s,  s, -s,
            -s, -s,  s,  s, -s,  s,  s,  s,  s,  -s,  s,  s,
        )
        val idx = shortArrayOf(
            0,1,2, 0,2,3,
            4,5,6, 4,6,7,
            0,1,5, 0,5,4,
            2,3,7, 2,7,6,
            1,2,6, 1,6,5,
            0,3,7, 0,7,4
        )
        return Mesh(asFloatBuffer(verts), asShortBuffer(idx), verts.size/3, idx.size)
    }

    private fun createStars(count: Int, radius: Float) {
        val verts = FloatArray(count * 3)
        var k = 0
        var seed = 42L
        fun rand(): Float { // simple LCG for stable positions
            seed = (seed * 1664525L + 1013904223L) and 0xffffffffL
            return ((seed ushr 8) and 0xffffff).toFloat() / 0xFFFFFF
        }
        repeat(count) {
            val u = rand() * 2f - 1f
            val t = rand() * 2f * PI_F
            val rxy = sqrtf(max(0f, 1f - u*u))
            val x = rxy * cosf(t)
            val y = u
            val z = rxy * sinf(t)
            verts[k++] = x * radius
            verts[k++] = y * radius
            verts[k++] = z * radius
        }
        stars = asFloatBuffer(verts)
        starCount = count
    }

    // Route building: Quadratic Bézier from Earth to Mars via control point on Mars orbit near midpoint
    private fun buildRouteBezier(earth: FloatArray, mars: FloatArray) {
        val ex = earth[0]; val ez = earth[2]
        val mx = mars[0]; val mz = mars[2]
        // Midpoint between Earth and Mars
        val midX = 0.5f * (ex + mx)
        val midZ = 0.5f * (ez + mz)
        // Direction from origin to midpoint
        var dirX = midX
        var dirZ = midZ
        val dirLen = sqrtf(dirX*dirX + dirZ*dirZ)
        if (dirLen < 1e-6f) { // fallback to Mars direction if midpoint is at origin
            dirX = mx; dirZ = mz
        }
        val inv = 1f / max(1e-6f, sqrtf(dirX*dirX + dirZ*dirZ))
        dirX *= inv; dirZ *= inv
        val rMars = orbitR[marsIdx]
        val cx = dirX * rMars
        val cz = dirZ * rMars

        // If Earth and Mars are extremely close, just use a straight tiny segment
        val dEm = sqrtf((mx-ex)*(mx-ex) + (mz-ez)*(mz-ez))
        if (dEm < 1e-5f) {
            routePts = floatArrayOf(ex,0f,ez, mx,0f,mz)
            routeCum = floatArrayOf(0f, dEm)
            routeLen = dEm
            return
        }

        // Sample quadratic Bézier B(t) = (1-t)^2*E + 2(1-t)t*C + t^2*M
        val segments = 128
        val pts = FloatArray((segments + 1) * 3)
        var k = 0
        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val omt = 1f - t
            val b0 = omt * omt
            val b1 = 2f * omt * t
            val b2 = t * t
            val x = b0*ex + b1*cx + b2*mx
            val z = b0*ez + b1*cz + b2*mz
            pts[k++] = x
            pts[k++] = 0f
            pts[k++] = z
        }
        routePts = pts
        val n = pts.size / 3
        routeCum = FloatArray(n)
        var acc = 0f
        routeCum[0] = 0f
        for (i in 1 until n) {
            val i3 = i*3; val j3 = (i-1)*3
            val dx = pts[i3] - pts[j3]
            val dz = pts[i3+2] - pts[j3+2]
            acc += sqrtf(dx*dx + dz*dz)
            routeCum[i] = acc
        }
        routeLen = acc
    }

    private fun samplePath(u: Float): FloatArray {
        if (routePts.size < 6) return floatArrayOf(0f,0f,0f)
        val target = u * routeLen
        var i = 1
        while (i < routeCum.size && routeCum[i] < target) i++
        val i1 = min(max(1, i), routeCum.size - 1)
        val i0 = i1 - 1
        val d0 = routeCum[i0]
        val d1 = routeCum[i1]
        val t = if (d1 > d0) (target - d0) / (d1 - d0) else 0f
        val j0 = i0*3; val j1 = i1*3
        val x = lerpf(routePts[j0], routePts[j1], t)
        val z = lerpf(routePts[j0+2], routePts[j1+2], t)
        return floatArrayOf(x, 0f, z)
    }

    // Public input API: called from UI thread via queueEvent
    fun onDrag(deltaXPx: Float, deltaYPx: Float) {
        val yawSensitivity = 0.004f // rad per px
        val pitchSensitivity = 0.003f // rad per px
        userYaw += deltaXPx * yawSensitivity
        userPitch += -deltaYPx * pitchSensitivity // drag up to look up
    }
    fun onScale(scaleFactor: Float) {
        // Typical: scaleFactor>1 => zoom in (closer). We reduce distance multiplier accordingly.
        val sf = clampf(scaleFactor, 0.5f, 2.0f)
        zoomDistMul /= sf
        zoomDistMul = clampf(zoomDistMul, 0.25f, 4f)
    }

    // Math helpers
    private fun asFloatBuffer(arr: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(arr); position(0) }
    private fun asShortBuffer(arr: ShortArray): ShortBuffer =
        ByteBuffer.allocateDirect(arr.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(arr); position(0) }

    private fun buildProgram(vs: String, fs: String): Int {
        val v = compileShader(GLES20.GL_VERTEX_SHADER, vs)
        val f = compileShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) { val log = GLES20.glGetProgramInfoLog(p); GLES20.glDeleteProgram(p); throw RuntimeException("Link failed: $log") }
        return p
    }
    private fun compileShader(type: Int, src: String): Int {
        val sh = GLES20.glCreateShader(type)
        GLES20.glShaderSource(sh, src)
        GLES20.glCompileShader(sh)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(sh, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) { val log = GLES20.glGetShaderInfoLog(sh); GLES20.glDeleteShader(sh); throw RuntimeException("Compile failed: $log\n$src") }
        return sh
    }

    private fun distance(ax: Float, az: Float, bx: Float, bz: Float) = sqrtf((bx-ax)*(bx-ax) + (bz-az)*(bz-az))
    private fun distanceSegmentToOrigin(ax: Float, az: Float, bx: Float, bz: Float): Float {
        val vx = bx - ax; val vz = bz - az
        val len2 = vx*vx + vz*vz
        if (len2 <= 1e-8f) return sqrtf(ax*ax + az*az)
        val t = clampf((0 - ax)*vx + (0 - az)*vz, 0f, len2) / len2
        val px = ax + t * vx; val pz = az + t * vz
        return sqrtf(px*px + pz*pz)
    }
    private fun directedAngleDelta(a1: Float, a2: Float, sign: Float): Float {
        var d = a2 - a1
        d = ((d + PI_F) % (2f * PI_F)) - PI_F
        return if (sign > 0f) { if (d < 0f) d + 2f * PI_F else d } else { if (d > 0f) d - 2f * PI_F else d }
    }

    // Float math wrappers
    private fun sinf(x: Float) = kotlin.math.sin(x.toDouble()).toFloat()
    private fun cosf(x: Float) = kotlin.math.cos(x.toDouble()).toFloat()
    private fun sqrtf(x: Float) = kotlin.math.sqrt(x)
    private fun atan2f(y: Float, x: Float) = kotlin.math.atan2(y.toDouble(), x.toDouble()).toFloat()
    private fun acosf(x: Float) = kotlin.math.acos(x.toDouble()).toFloat()
    private fun absf(x: Float) = kotlin.math.abs(x)
    private fun clampf(v: Float, lo: Float, hi: Float) = max(lo, min(hi, v))
    private fun lerpf(a: Float, b: Float, t: Float) = a + (b - a) * t

    companion object {
        private const val PI_F = 3.1415927f
        private const val VS = """
            attribute vec3 aPosition;
            uniform mat4 uMVP;
            uniform float uPointSize;
            void main() {
                gl_Position = uMVP * vec4(aPosition, 1.0);
                gl_PointSize = uPointSize;
            }
        """
        private const val FS = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """
    }

    // Simple stroke font for capital letters (segments: x0,y0,x1,y1 in [-0.5,0.5])
    private fun letterSegments(ch: Char): FloatArray {
        return when (ch.uppercaseChar()) {
            'E' -> floatArrayOf(
                -0.5f,-0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f,  0.5f, 0.5f,
                -0.5f, 0.0f,  0.3f, 0.0f,
                -0.5f,-0.5f,  0.5f,-0.5f
            )
            'M' -> floatArrayOf(
                -0.5f,-0.5f, -0.5f, 0.5f,
                 0.5f,-0.5f,  0.5f, 0.5f,
                -0.5f, 0.5f,  0.0f, 0.0f,
                 0.0f, 0.0f,  0.5f, 0.5f
            )
            'V' -> floatArrayOf(
                -0.5f, 0.5f,  0.0f,-0.5f,
                 0.0f,-0.5f,  0.5f, 0.5f
            )
            'J' -> floatArrayOf(
                -0.5f, 0.5f,  0.5f, 0.5f,
                 0.5f, 0.5f,  0.5f, 0.0f,
                 0.5f, 0.0f,  0.0f,-0.5f,
                 0.0f,-0.5f, -0.3f,-0.5f
            )
            'S' -> floatArrayOf(
                0.5f, 0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f, -0.5f, 0.0f,
                -0.5f, 0.0f,  0.5f, 0.0f,
                 0.5f, 0.0f,  0.5f,-0.5f,
                 0.5f,-0.5f, -0.5f,-0.5f
            )
            'U' -> floatArrayOf(
                -0.5f, 0.5f, -0.5f,-0.3f,
                -0.5f,-0.3f,  0.0f,-0.5f,
                 0.0f,-0.5f,  0.5f,-0.3f,
                 0.5f,-0.3f,  0.5f, 0.5f
            )
            'N' -> floatArrayOf(
                -0.5f,-0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f,  0.5f,-0.5f,
                 0.5f,-0.5f,  0.5f, 0.5f
            )
            else -> floatArrayOf(
                -0.5f,-0.5f,  0.5f, 0.5f,
                -0.5f, 0.5f,  0.5f,-0.5f
            )
        }
    }

    private val tmp3a = FloatArray(3)

    private fun drawLetterBillboard(ch: Char, worldPos: FloatArray, size: Float, rightOffset: Float, color: FloatArray) {
        val seg = letterSegments(ch)
        if (seg.isEmpty()) return
        val verts = FloatArray((seg.size/2) * 3)
        var vi = 0
        // Origin offset to the side of the sphere along camera right
        val ox = worldPos[0] + camRight[0] * rightOffset
        val oy = worldPos[1] + camRight[1] * rightOffset
        val oz = worldPos[2] + camRight[2] * rightOffset
        // For each 2D endpoint, place in world using camRight and camUp
        var i = 0
        while (i < seg.size) {
            val lx = seg[i] * size; val ly = seg[i+1] * size
            val wx = ox + camRight[0]*lx + camUp[0]*ly
            val wy = oy + camRight[1]*lx + camUp[1]*ly
            val wz = oz + camRight[2]*lx + camUp[2]*ly
            verts[vi++] = wx; verts[vi++] = wy; verts[vi++] = wz
            i += 2
        }
        val vb = asFloatBuffer(verts)
        Matrix.setIdentityM(model, 0)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniform4fv(uColor, 1, color, 0)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 3*4, vb)
        GLES20.glLineWidth(2f)
        // Draw as lines: each pair of points is one segment
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, verts.size / 3)
        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    // Vector math helpers
    private fun cross3(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float, out: FloatArray) {
        out[0] = ay*bz - az*by
        out[1] = az*bx - ax*bz
        out[2] = ax*by - ay*bx
    }
    private fun normalize3(x: Float, y: Float, z: Float, out: FloatArray) {
        val l = sqrtf(x*x + y*y + z*z)
        if (l < 1e-8f) { out[0]=0f; out[1]=1f; out[2]=0f } else { val inv = 1f/l; out[0]=x*inv; out[1]=y*inv; out[2]=z*inv }
    }
}
