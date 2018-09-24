package tw.inspect.scalechart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.Log
import android.util.Pair
import android.util.SparseArray
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Scroller
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val EVENT_RECT_TOP_PERCENTAGE = 0.2
private const val EVENT_RECT_BOTTOM_PERCENTAGE = 0.65
private const val TIME_LINE_TOP_PERCENTAGE = 0.7
private const val TIME_TEXT_TOP_PERCENTAGE = 0.9

class ScaleChartView : View {

    private val paint = Paint()
    private val paint1 = Paint()
    private val paint2 = Paint()
    private val TAG = this.javaClass.simpleName
    private var lineSegments: List<LineSegment> = ArrayList()

    private var onLineSegmentClickedListener: OnLineSegmentClickedListener? = null

    private var onDoubleTapListener: GestureDetector.OnDoubleTapListener? = null

    var lineSegmentList: List<LineSegment>
        get() = lineSegments
        set(lineSegmentList) {
            this.lineSegments = lineSegmentList
            invalidate()
        }


    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    init {
        paint.textSize = 32f
        paint1.textSize = 32f
        paint2.textSize = 32f
        paint1.color = Color.YELLOW
        paint2.color = Color.BLACK
        paint.strokeWidth = 4F
        setPadding(4, 4, 0, 0)
    }

    private var safeStartValue: Long = 0
    private var safeEndValue: Long = 60 * 60 * 24
    private var offset: Int = 0
        set(value) {
            field = value
            safeStartValue = 0L - value
            safeEndValue = bound.second - value.toLong()
            currentSafeUnit += offset
        }

    private var bound: Pair<Int, Int> = Pair(0, 60 * 60 * 24)
        set(value) {
            field = value
            offset = if (value.first < 0) -value.first else 0
        }


    var startAndLength: Pair<Int, Int> = Pair(0, 60 * 60 * 24)
        set(value) {
            bound = Pair(value.first, value.first + value.second)
        }

    var scrollable: Boolean = true

    var currentUnit: Double
        set(value) {
            currentSafeUnit = value + offset
        }
        get() {
            return currentSafeUnit - offset
        }

    private var currentSafeUnit: Double = 0.0

    var loop: Boolean = false

    private fun formatTimeUntilSecond(value: Int): String {
        val hours = value / 3600
        val minutes = (value % 3600) / 60
        val seconds = value % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatTimeUntilMinute(value: Int): String {
        val hours = value / 3600
        val minutes = (value % 3600) / 60
        return String.format("%02d:%02d", hours, minutes)
    }

    /**
     * Should be the order in descending power
     * */
    var scales: List<Scale> = listOf(
            object : Scale(4 * 60 * 60, 0.000001, 0.000001, 0.95) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(2 * 60 * 60, 0.000001, 0.000001, 0.95) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(60 * 60, 0.000001, 0.000001, 0.95) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(30 * 60, 0.00021, 0.00015, 0.8) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(10 * 60, 0.000625, 0.00021, 0.7) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(5 * 60, 0.00138, 0.000625, 0.6) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(60, 0.0077, 0.00138, 0.5) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(30, 0.017, 0.0077, 0.4) {
                override fun parse(value: Int): String {
                    return formatTimeUntilSecond(value)
                }
            },
            object : Scale(10, 0.09, 0.017, 0.3) {
                override fun parse(value: Int): String {
                    return formatTimeUntilSecond(value)
                }
            },
            object : Scale(5, 0.18, 0.09, 0.2) {
                override fun parse(value: Int): String {
                    return formatTimeUntilSecond(value)
                }
            },
            object : Scale(1, 0.6, 0.18, 0.1) {
                override fun parse(value: Int): String {
                    return formatTimeUntilSecond(value)
                }
            }
    )

    /**
     * How many dp / unit
     * */
    var resolution = 1.0 / 6400

    private fun pxToUnit(px: Double): Double {
        return px / context.resources.displayMetrics.xdpi / resolution
    }

    private fun unitToPx(unit: Double): Double {
        return unit * context.resources.displayMetrics.xdpi * resolution
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            Log.e("onScroll", "$e1 $e2 $distanceX $distanceY")
            if(scrollable) {
                scrollToSafeUnit(currentSafeUnit + pxToUnit(distanceX.toDouble()))
            }
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            Log.e("onDown", e.toString())
            //releaseEdgeEffects()
            scroller.forceFinished(true)
            ViewCompat.postInvalidateOnAnimation(this@ScaleChartView)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.e("onDoubleTap", e.toString())
            onDoubleTapListener?.onDoubleTap(e)
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            Log.e("onFling", "$e1 $e2 $velocityX $velocityY")
            fling(velocityX)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            Log.e("onSingleTapConfirmed", e.toString())

            for (i in lineSegments.indices) {

                val EVENT_RECT_TOP = (height * EVENT_RECT_TOP_PERCENTAGE).toFloat()
                val EVENT_RECT_BOTTOM = (height * EVENT_RECT_BOTTOM_PERCENTAGE).toFloat()


                val oneHourEqualPx = width / 25.0f
                Log.e(TAG, oneHourEqualPx.toString() + "")
                val oneSecondEqualPx = oneHourEqualPx / (60f * 60f)

                if (e.x >= lineSegments[i].startPoint * oneSecondEqualPx &&
                        e.x <= lineSegments[i].endPoint * oneSecondEqualPx &&
                        e.y >= EVENT_RECT_TOP &&
                        e.y <= EVENT_RECT_BOTTOM) {

                    if (onLineSegmentClickedListener != null) {
                        onLineSegmentClickedListener!!.onLineSegmentClicked(this@ScaleChartView, i)
                    }

                    return true
                }
            }

            return super.onSingleTapConfirmed(e)
        }
    })


    private fun fling(velocityX: Float) {
        //releaseEdgeEffects()

        scroller.forceFinished(true)
        scroller.fling(
                currentUnit.toInt(),
                0,
                (velocityX * resolution).toInt(),
                0,
                0, safeEndValue.toInt(),
                0, 0
        )
        ViewCompat.postInvalidateOnAnimation(this)
    }

    private var lastSpanX = 1F
    private var lastResolution = resolution
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            Log.e("onScaleBegin", detector.toString())
            lastResolution = resolution
            lastSpanX = detector.currentSpanX
            return super.onScaleBegin(detector)
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            Log.e("onScale", detector.toString())
            resolution = lastResolution * detector.currentSpanX / lastSpanX
            invalidate()
            return super.onScale(detector)
        }
    })

    private val scroller = Scroller(context)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var retVal = scaleGestureDetector.onTouchEvent(event)
        retVal = gestureDetector.onTouchEvent(event) || retVal
        return retVal || super.onTouchEvent(event)
    }


    private val sparseArray = SparseArray<Scale>()

    override fun onDraw(canvas: Canvas) {

        val eventTop = (height * EVENT_RECT_TOP_PERCENTAGE).toFloat()
        val eventBottom = (height * EVENT_RECT_BOTTOM_PERCENTAGE).toFloat()
        val scaleBarTop = (height * TIME_LINE_TOP_PERCENTAGE).toFloat()
        val scaleTextTop = (height * TIME_TEXT_TOP_PERCENTAGE).toFloat()


        paint.color = Color.BLUE
        paint.style = Style.FILL

        val viewStartUnit = currentSafeUnit - pxToUnit(width - paddingLeft.toDouble()) / 2
        val viewEndUnit = currentSafeUnit + pxToUnit(width - paddingRight.toDouble()) / 2

        val underflow = (viewStartUnit < safeStartValue.toDouble())
        val overflow = (viewEndUnit > safeEndValue.toDouble())

/*
        for (event in lineSegments) {

            val oneHourEqualPx = width / 25.0f
            val oneSecondEqualPx = oneHourEqualPx / (60f * 60f)

            rectF.set(event.startPoint * oneSecondEqualPx, EVENT_RECT_TOP, event.endPoint * oneSecondEqualPx, EVENT_RECT_BOTTOM)

            canvas.drawRect(rectF, paint)
            canvas.drawText(event.text, event.startPoint * oneSecondEqualPx, EVENT_RECT_TOP, paint2)

        }
*/

        paint.color = Color.BLACK

        canvas.drawLine(paddingLeft.toFloat(), scaleBarTop, width - paddingRight.toFloat(), scaleBarTop, paint)

        val visibleScales = scales.filter { it.scaleResolution <= resolution }

        sparseArray.clear()

        val currentUnitRounded = currentSafeUnit.roundToInt()
        val oneBelowCurrentUnit = if (currentUnitRounded.toDouble() >= currentSafeUnit) {
            currentUnitRounded - 1
        } else {
            currentUnitRounded
        }

        // draw middle
        val midStartUnit = max(viewStartUnit, safeStartValue.toDouble())
        val midEndUnit = min(viewEndUnit, if (loop) (safeEndValue - 1).toDouble() else safeEndValue.toDouble())

        visibleScales.asReversed().forEach {
            val divisibleStart = if (midStartUnit.toInt() % it.unit == 0) {
                midStartUnit.toInt()
            } else {
                ((midStartUnit.toInt() / it.unit) + 1) * it.unit
            }

            for (i in divisibleStart..midEndUnit.toInt() step it.unit) {
                sparseArray.put(i, it)
            }
        }

        val scaleHeight = height * (TIME_TEXT_TOP_PERCENTAGE - TIME_LINE_TOP_PERCENTAGE)

        for (i in 0 until sparseArray.size()) {
            val safeUnit = sparseArray.keyAt(i)
            val scale = sparseArray.valueAt(i)
            val positionX = (
                    (width - paddingLeft - paddingEnd) / 2 +
                            paddingLeft + unitToPx(safeUnit - currentSafeUnit)
                    ).toFloat()

            if (scale.textResolution <= resolution) {
                scale.parse(safeUnit - offset).also {
                    canvas.drawText(
                            it,
                            positionX - paint.measureText(it) / 2,
                            scaleTextTop + 20,
                            paint
                    )
                }

            }
            canvas.drawLine(
                    positionX,
                    scaleBarTop,
                    positionX,
                    (height * TIME_LINE_TOP_PERCENTAGE + scaleHeight * scale.lengthRatio).toFloat(),
                    paint
            )
        }

        if (loop) {

            if (underflow) {
                // draw left
                //val lowStartUnit =
                visibleScales.asReversed().forEach {


                    for (i in oneBelowCurrentUnit downTo safeStartValue step it.unit.toLong()) {

                    }
                }

            }
            if (overflow) {
                // draw right
                //val highEndUnit =
                visibleScales.asReversed().forEach {


                    for (i in oneBelowCurrentUnit downTo safeStartValue step it.unit.toLong()) {

                    }
                }

            }
        }

    }


    fun setOnLineSegmentClickedListener(listener: OnLineSegmentClickedListener?) {
        this.onLineSegmentClickedListener = listener
    }

    fun setOnDoubleTapListener(listener: GestureDetector.OnDoubleTapListener?) {
        this.onDoubleTapListener = listener
    }


    fun scrollToUnit(unit: Double) {
        scrollToSafeUnit(unit - offset)
    }

    private fun scrollToSafeUnit(unit: Double) {
        Log.e("scrollToSafeUnit", "$unit")
        val newUnit = max(min(unit, safeEndValue.toDouble()), safeStartValue.toDouble())
        if (newUnit != currentSafeUnit) {
            currentSafeUnit = newUnit
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }
}
