package tw.inspect.scalechart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.RectF
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val EVENT_RECT_TOP_PERCENTAGE = 0.2
private const val EVENT_RECT_BOTTOM_PERCENTAGE = 0.65
private const val TIME_LINE_TOP_PERCENTAGE = 0.7
private const val TIME_TEXT_TOP_PERCENTAGE = 0.9
private const val SCALE_THICKNESS = 4F

class ScaleChartView : View {

    private val paint = Paint()
    private val paint1 = Paint()
    private val paint2 = Paint()
    private val TAG = this.javaClass.simpleName

    private var onLineSegmentClickedListener: OnLineSegmentClickedListener? = null

    private var onDoubleTapListener: GestureDetector.OnDoubleTapListener? = null

    var sortedLineSegments: List<LineSegment> = ArrayList()
        set(lineSegmentList) {
            field = lineSegmentList
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
        paint.strokeWidth = SCALE_THICKNESS
        paint.style = Style.FILL
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
            object : Scale(4 * 60 * 60, 1000000.0, 1000000.0, 0.95) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(2 * 60 * 60, 1000000.0, 1000000.0, 0.95) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(60 * 60, 1000000.0, 1000000.0, 0.95) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(30 * 60, 4761.9, 6666.6, 0.8) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(10 * 60, 1600.0, 4761.9, 0.7) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(5 * 60, 724.6, 1600.0, 0.6) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(60, 130.0, 724.6, 0.5) {
                override fun parse(value: Int): String {
                    return formatTimeUntilMinute(value)
                }
            },
            object : Scale(30, 58.8, 130.0, 0.4) {
                override fun parse(value: Int): String {
                    return formatTimeUntilSecond(value)
                }
            },
            object : Scale(10, 11.1, 58.8, 0.3) {
                override fun parse(value: Int): String {
                    return formatTimeUntilSecond(value)
                }
            },
            object : Scale(5, 5.5, 11.1, 0.2) {
                override fun parse(value: Int): String {
                    return formatTimeUntilSecond(value)
                }
            },
            object : Scale(1, 1.67, 5.5, 0.1) {
                override fun parse(value: Int): String {
                    return formatTimeUntilSecond(value)
                }
            }
    )

    /**
     * How many unit / inch
     * It's your duty to make sure the value is inside the bound
     * */
    var upi = 6400 / 1.0

    var maxUpi = 8000 / 1.0
    var minUpi = 1 / 1.0

    private fun pxToUnit(px: Double): Double {
        return px / context.resources.displayMetrics.xdpi * upi
    }

    private fun unitToPx(unit: Double): Double {
        return unit * context.resources.displayMetrics.xdpi / upi
    }

    private val innerWidth
        get() = width - paddingStart - paddingEnd

    private fun getPositionX(safeUnit: Double): Double {
        return (innerWidth / 2 + paddingLeft + unitToPx(safeUnit - currentSafeUnit))
    }

    private fun getPositionX(safeUnit: Int): Double {
        return (innerWidth / 2 + paddingLeft + unitToPx(safeUnit - currentSafeUnit))
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        private var downSafeUnit = 0.0
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            Log.e("onScroll", "$e1 $e2 $distanceX $distanceY")
            if (scrollable) {
                scrollToSafeUnit(downSafeUnit + pxToUnit(e1.x - e2.x.toDouble()))
            }
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            Log.e("onDown", e.toString())
            downSafeUnit = currentSafeUnit
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

            for (i in sortedLineSegments.indices) {

                val EVENT_RECT_TOP = (height * EVENT_RECT_TOP_PERCENTAGE).toFloat()
                val EVENT_RECT_BOTTOM = (height * EVENT_RECT_BOTTOM_PERCENTAGE).toFloat()


                val oneHourEqualPx = width / 25.0f
                Log.e(TAG, oneHourEqualPx.toString() + "")
                val oneSecondEqualPx = oneHourEqualPx / (60f * 60f)

                if (e.x >= sortedLineSegments[i].startPoint * oneSecondEqualPx &&
                        e.x <= sortedLineSegments[i].endPoint * oneSecondEqualPx &&
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
    }).apply {
        setIsLongpressEnabled(false)
    }


    private fun fling(velocityX: Float) {
        //releaseEdgeEffects()

        scroller.forceFinished(true)
        scroller.fling(
                currentUnit.toInt(),
                0,
                (velocityX / upi).toInt(),
                0,
                0, safeEndValue.toInt(),
                0, 0
        )
        ViewCompat.postInvalidateOnAnimation(this)
    }

    private val scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                private var prev = 1.0
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    Log.e("onScaleBegin", detector.toString())
                    prev = upi * detector.currentSpanX
                    return super.onScaleBegin(detector)
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    Log.e("onScale", detector.toString())
                    upi = max(minUpi, min(maxUpi, prev / detector.currentSpanX))
                    invalidate()
                    return super.onScale(detector)
                }
            }
    )

    private val scroller = Scroller(context)


    private lateinit var lastEvent: MotionEvent
    private var multi = false
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        if (event.pointerCount == 1) {

            // Add the MotionEvent.ACTION_DOWN back
            // I put a second condition "event.action != MotionEvent.ACTION_DOWN"
            // in case multiple pointers leave together
            if (multi && event.action != MotionEvent.ACTION_DOWN) {
                gestureDetector.onTouchEvent(event.apply { action = MotionEvent.ACTION_DOWN })
            }
            gestureDetector.onTouchEvent(event)

        } else {
            if (!multi) {
                // Cancel scrolling
                gestureDetector.onTouchEvent(lastEvent.apply { action = MotionEvent.ACTION_CANCEL })
            }
        }
        this.lastEvent = event
        multi = (event.pointerCount != 1)

        return true
    }


    private val sparseArray = SparseArray<Scale>()
    private val rectF = RectF()

    override fun onDraw(canvas: Canvas) {
        val height = this.height

        val eventTop = (height * EVENT_RECT_TOP_PERCENTAGE).toFloat()
        val eventBottom = (height * EVENT_RECT_BOTTOM_PERCENTAGE).toFloat()
        val scaleBarTop = (height * TIME_LINE_TOP_PERCENTAGE).toFloat()
        val scaleTextTop = (height * TIME_TEXT_TOP_PERCENTAGE).toFloat()


        val viewStartUnit = currentSafeUnit - pxToUnit(width - paddingLeft.toDouble()) / 2
        val viewEndUnit = currentSafeUnit + pxToUnit(width - paddingRight.toDouble()) / 2

        val underflow = (viewStartUnit < safeStartValue.toDouble())
        val overflow = (viewEndUnit > safeEndValue.toDouble())


        paint.color = Color.BLACK


        val visibleScales = scales.filter { it.scaleResolution >= upi }

        sparseArray.clear()

        val currentUnitRounded = currentSafeUnit.roundToInt()
        val oneBelowCurrentUnit = if (currentUnitRounded.toDouble() >= currentSafeUnit) {
            currentUnitRounded - 1
        } else {
            currentUnitRounded
        }

        // draw middle
        val midStartSafeUnit = max(viewStartUnit, safeStartValue.toDouble())
        val midEndSafeUnit = min(viewEndUnit, if (loop) (safeEndValue - 1).toDouble() else safeEndValue.toDouble())


        visibleScales.asReversed().forEach {
            val divisibleStart = if (midStartSafeUnit.toInt() % it.unit == 0) {
                midStartSafeUnit.toInt()
            } else {
                ((midStartSafeUnit.toInt() / it.unit) + 1) * it.unit
            }

            for (i in divisibleStart..midEndSafeUnit.toInt() step it.unit) {
                sparseArray.put(i, it)
            }
        }

        val scaleHeight = height * (TIME_TEXT_TOP_PERCENTAGE - TIME_LINE_TOP_PERCENTAGE)

        for (i in 0 until sparseArray.size()) {
            val safeUnit = sparseArray.keyAt(i)
            val scale = sparseArray.valueAt(i)
            val positionX = getPositionX(safeUnit).toFloat()

            if (scale.textResolution >= upi) {
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

        paint.color = Color.BLUE
        // TODO filter events
        for (event in sortedLineSegments) {

            rectF.set(
                    getPositionX(event.startPoint).toFloat(),
                    (EVENT_RECT_TOP_PERCENTAGE * height).toFloat(),
                    getPositionX(event.endPoint).toFloat(),
                    (EVENT_RECT_BOTTOM_PERCENTAGE * height).toFloat()
            )

            canvas.drawRect(rectF, paint)
            canvas.drawText(event.text,
                    getPositionX(event.startPoint).toFloat(),
                    (EVENT_RECT_TOP_PERCENTAGE * height).toFloat(),
                    paint2
            )

        }
        paint.color = Color.BLACK


        if (loop) {
            // [start, end] <= closed interval
            canvas.drawLine(
                    paddingLeft.toFloat(),
                    scaleBarTop,
                    width - paddingRight.toFloat(),
                    scaleBarTop,
                    paint
            )

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

        } else {
            canvas.drawLine(
                    if (underflow) {
                        (getPositionX(midStartSafeUnit) - SCALE_THICKNESS / 2).toFloat()
                    } else {
                        paddingLeft.toFloat()
                    },
                    scaleBarTop,
                    if (overflow) {
                        (getPositionX(midEndSafeUnit) + SCALE_THICKNESS / 2).toFloat()
                    } else {
                        width - paddingRight.toFloat()
                    },
                    scaleBarTop,
                    paint
            )
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
