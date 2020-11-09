package com.kuang.barchartview

import android.content.Context
import android.graphics.*
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.collections.ArrayList
import kotlin.math.log

open class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val TAG = "BarChartView"
    }

    // 柱子的Rect，用于判断点击事件
    private lateinit var mBarRectList: ArrayList<Rect>
    private var mOnClickListener: OnClickListener? = null
    private val mBarWidth = pxToSp(30.0F).toInt()
    private val commonRect = Rect()

    // 绘制左轴文字的相关属性
    private var mLeftAxisTextSize = pxToSp(20.0F)
    private var mLeftAxisColor = Color.BLACK
    private var mLeftAxisTextColor = Color.BLACK
    private var mLeftAxisWidth = pxToSp(2.0F)
    private var mLeftAxisText = ArrayList<Float>()
    private val mLeftAxisTextPaint: Paint = Paint()
    private val mLeftAxisLinePaint: Paint = Paint()

    // 绘制底轴文字的相关属性
    private var mBottomAxisTextSize = pxToSp(20.0F)
    private var mBottomAxisTextColor = Color.BLACK
    private var mBottomAxisColor = Color.BLACK
    private var mBottomAxisWith = pxToSp(2.0F)
    private var mBottomAxisText = ArrayList<String>()
    private val mBottomAxisTextPaint: Paint = Paint()
    private val mBottomAxisLinePaint: Paint = Paint() // 绘制底轴线的Paint

    private var mIsShowOrientationLine = true
    private var mOrientationGuidesWidth = pxToSp(1.0F)
    private var mOrientationGuidesColor = Color.GRAY
    private var mOrientationGuidesStyle = 0

    private var mIsShowVerticalLine = false
    private var mVerticalGuidesWidth = pxToSp(1.0F)
    private var mVerticalGuidesColor = Color.GRAY
    private var mVerticalGuidesStyle = 0

    // 绘制X轴参考线的Paint
    private val mOrientationAxisLinePaint: Paint = Paint()
    private var mBarValueTextSize = pxToSp(14.0F)
    private var mBarValueTextColor = Color.GRAY
    private var mBarValueText = ArrayList<Float>()

    private val mVerticalLinePaint = Paint()

    // 绘制柱子上面值的Paint
    private val mBarValuePaint: Paint = Paint()
    private val mBarPaint = Paint()

    private lateinit var slideCanvas: Canvas
    private lateinit var slideBitmap: Bitmap
    private lateinit var leftCanvas: Canvas
    private lateinit var leftBitmap: Bitmap

    private var mMotionEventLastX = 0F
    private var mMoveX = 0F
    init {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.BarChartView)
        mLeftAxisWidth =
            typeArray.getDimension(R.styleable.BarChartView_leftAxisWidth, mLeftAxisWidth)
        mLeftAxisColor =
            typeArray.getColor(R.styleable.BarChartView_leftAxisColor, mLeftAxisTextColor)
        mLeftAxisTextSize =
            typeArray.getDimension(R.styleable.BarChartView_leftAxisTextSize, mLeftAxisTextSize)
        mLeftAxisTextColor =
            typeArray.getColor(R.styleable.BarChartView_leftAxisTextColor, mLeftAxisTextColor)

        mBottomAxisWith =
            typeArray.getDimension(R.styleable.BarChartView_bottomAxisWidth, mBottomAxisWith)
        mBottomAxisColor = typeArray.getColor(R.styleable.BarChartView_bottomAxisColor, Color.BLACK)
        mBottomAxisTextSize =
            typeArray.getDimension(R.styleable.BarChartView_bottomAxisTextSize, mBottomAxisTextSize)
        mBottomAxisTextColor =
            typeArray.getColor(R.styleable.BarChartView_bottomAxisTextColor, Color.BLACK)

        mOrientationGuidesWidth = typeArray.getDimension(
            R.styleable.BarChartView_orientationGuidesWidth,
            mOrientationGuidesWidth
        )
        mOrientationGuidesColor =
            typeArray.getColor(R.styleable.BarChartView_orientationGuidesColor, Color.GRAY)
        mOrientationGuidesStyle =
            typeArray.getInt(R.styleable.BarChartView_orientationGuidesStyle, 0)

        mVerticalGuidesWidth = typeArray.getDimension(
            R.styleable.BarChartView_verticalGuidesWidth,
            mVerticalGuidesWidth
        )
        mVerticalGuidesColor =
            typeArray.getColor(R.styleable.BarChartView_verticalGuidesColor, Color.GRAY)
        mVerticalGuidesStyle = typeArray.getInt(R.styleable.BarChartView_verticalGuidesStyle, 0)
        typeArray.recycle()

        initArgs()
    }

    private fun initArgs() {
        // 设置绘制线的相关Paint属性
        mLeftAxisLinePaint.color = mLeftAxisColor
        mBottomAxisLinePaint.color = mBottomAxisColor
        mOrientationAxisLinePaint.color = mOrientationGuidesColor
        mVerticalLinePaint.color = mVerticalGuidesColor
        // 设置绘制左轴线相关的Paint属性
        mLeftAxisTextPaint.isAntiAlias = true
        mLeftAxisTextPaint.color = mLeftAxisTextColor
        mLeftAxisTextPaint.textSize = mLeftAxisTextSize
        // 设置绘制底轴线相关的Paint属性
        mBottomAxisTextPaint.isAntiAlias = true
        mBottomAxisTextPaint.color = mBottomAxisTextColor
        mBottomAxisTextPaint.textSize = mBottomAxisTextSize
        // 设置绘制柱子值相关的Paint属性
        mBarValuePaint.isAntiAlias = true
        mBarValuePaint.color = mBarValueTextColor
        mBarValuePaint.textSize = mBarValueTextSize
        // 设置绘制柱子的Paint属性
        mBarPaint.color = Color.parseColor("#FF4DD0E1")
    }

    fun setLeftAxisValue(leftAxisValue: List<Float>) {
        mLeftAxisText = leftAxisValue as ArrayList<Float>
    }

    fun setBottomAxisValue(leftAxisValue: List<String>) {
        mBottomAxisText = leftAxisValue as ArrayList<String>
    }

    fun setBarValue(barValue: List<Float>) {
        mBarValueText = barValue as ArrayList<Float>
        mBarRectList = ArrayList(barValue.size)
    }

    fun setClickListener(listener: OnClickListener) {
        mOnClickListener = listener
    }

    fun show() {
        showInternal()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        showInternal()
    }
    
    private fun showInternal() {
        if (width <= 0) {
            return
        }
        val leftPoint = getLeftAxisEndPoint().x + mLeftAxisWidth.toInt()
        val slideCanvasLeft = width - leftPoint
        slideBitmap = Bitmap.createBitmap(slideCanvasLeft, height, Bitmap.Config.RGB_565)
        slideCanvas = Canvas(slideBitmap)
        slideCanvas.drawColor(Color.WHITE)

        leftBitmap = Bitmap.createBitmap(leftPoint, height, Bitmap.Config.RGB_565)
        leftCanvas = Canvas(leftBitmap)
        leftCanvas.drawColor(Color.WHITE)
    }

    override fun onSaveInstanceState(): Parcelable? {
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mMotionEventLastX = event.x
            }
            MotionEvent.ACTION_MOVE -> {
                mMoveX -= mMotionEventLastX - event.x
                mMotionEventLastX = event.x
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                mBarRectList.forEachIndexed { index, rect ->
                    if (rect.contains(event.x.toInt(), event.y.toInt())) {
                        mOnClickListener?.onClick(index)
                    }
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) {
            return
        }
        showInternal()
        drawLeftAxisLine(leftCanvas)
        drawLeftAxisText(leftCanvas)
        drawBottomAxisLine(slideCanvas)
        drawBottomAxisText(slideCanvas)
        drawOrientationLine(slideCanvas)
        drawBar(slideCanvas)
        val leftPoint = getLeftAxisEndPoint().x + mLeftAxisWidth.toInt()
        canvas.drawBitmap(slideBitmap, leftPoint.toFloat() + mMoveX, 0F, null)
        canvas.drawBitmap(leftBitmap, 0F, 0F, null)
    }

    // 绘制横向的参考线
    private fun drawOrientationLine(canvas: Canvas) {
        if (mIsShowOrientationLine) {
            val itemHeight = getLeftAxisHeight() / mLeftAxisText.size
            val width = width
            val x = mLeftAxisWidth
            val h = getOriginalPoint().y
            for (index in 1..mLeftAxisText.size) {
                val y = (h - (itemHeight * index))
                canvas.drawLine(
                    x,
                    y.toFloat(),
                    width.toFloat(),
                    y.toFloat(),
                    mOrientationAxisLinePaint
                )
            }
        }
    }

    // 绘制柱子
    private fun drawBar(canvas: Canvas) {
        val itemWidth = getBottomAxisWidth() / mBarValueText.size
        val itemCurrent = itemWidth / 2
        val bottom = getBottomAxisEndPoint().y - mBottomAxisWith.toInt()
        mBarValueText.forEachIndexed { index, value ->
            val left = (itemWidth * (index + 1) - itemCurrent - (mBarWidth / 2)) +  + getLeftAxisEndPoint().x
            val top = getBarTop(value)
            val right = left + mBarWidth
            val rect = Rect(left, top.toInt(), right, bottom)
            mBarRectList.add(rect)
            canvas.drawRect(rect, mBarPaint)
            drawBarValue(canvas, value, left, top)
        }
    }

    private fun getBarTop(value: Float): Float {
        mLeftAxisText.forEachIndexed { index, i ->
            val next = mLeftAxisText[index + 1]
            if (value >= i && value < next) {
                //Y轴的最大值是多少，那么就可以分割多少个item。暂时没有考虑float的情况
                val itemHeight = getLeftAxisHeight() / mLeftAxisText.size
                // 先求出占用了多少item，并得出所占item的总高度
                val baseHeight = getOriginalPoint().y - (itemHeight * (index + 1))
                // 在求出第index个item距index+1个item的高度
                val apartHeight = itemHeight * ((value - i) / (next - i))
                return baseHeight - apartHeight
            }
        }
        return 0F
    }

    private fun drawBarValue(canvas: Canvas, value: Float, left: Int, top: Float) {
        val text = value.toString()
        mBarValuePaint.getTextBounds(text, 0, text.length, commonRect)
        // x的值为Bar宽度的中间减去文字的中间，可以使文字相对于Bar始终处于中间
        val x = (left + (mBarWidth / 2)) - (commonRect.right / 2)
        val y = top - getTextPadding()
        canvas.drawText(text, x.toFloat(), y, mBarValuePaint)
    }

    private fun drawLeftAxisLine(canvas: Canvas) {
        val originalPoint = getOriginalPoint()
        val leftPoint = getLeftAxisEndPoint()

        val rect = Rect()
        rect.left = leftPoint.x
        rect.top = leftPoint.y
        rect.right = leftPoint.x + mLeftAxisWidth.toInt()
        rect.bottom = originalPoint.y

        canvas.drawRect(rect, mLeftAxisLinePaint)
    }

    private fun drawLeftAxisText(canvas: Canvas) {
        if (mLeftAxisText.isEmpty()) {
            return
        }
        val rect = Rect()
        val itemHeight = getLeftAxisHeight() / mLeftAxisText.size
        mLeftAxisText.forEachIndexed { index, leftAxisValue ->
            val y = getOriginalPoint().y - itemHeight * (index + 1)
            val text = leftAxisValue.toString()
            mLeftAxisTextPaint.getTextBounds(text, 0, text.length, rect)
            val textCurrent = rect.top / 2
            val availableWidth = getLeftAxisEndPoint().x
            // 文字靠右
            val x = availableWidth - rect.right - getTextPadding()
            canvas.drawText(text, x, y - textCurrent.toFloat(), mLeftAxisTextPaint)
        }
    }

    //绘制底轴的线
    private fun drawBottomAxisLine(canvas: Canvas) {
        val originalPoint = getOriginalPoint()
        val bottomPoint = getBottomAxisEndPoint()
        val rect = Rect()
        rect.left = 0
        rect.top = originalPoint.y - mBottomAxisWith.toInt()
        rect.right = bottomPoint.x
        rect.bottom = originalPoint.y
        canvas.drawRect(rect, mBottomAxisLinePaint)
    }

    //绘制底部轴的文字
    private fun drawBottomAxisText(canvas: Canvas) {
        if (mBottomAxisText.isEmpty()) {
            return
        }

        //每个item所能使用的最大宽度
        val itemWidth = getBottomAxisWidth() / mBottomAxisText.size
        val itemCurrent = itemWidth / 2
        mBottomAxisText.forEachIndexed { index, text ->
            commonRect.setEmpty()
            mBottomAxisTextPaint.getTextBounds(text, 0, text.length, commonRect)
            val current = itemWidth * (index + 1) - itemCurrent
            val x = (current - (commonRect.right / 2))
            val y = height.toFloat() - getTextPadding()
            canvas.drawText(text, x.toFloat(), y, mBottomAxisTextPaint)
            drawVerticalLine(canvas, current, y)
        }
    }

    private fun drawVerticalLine(canvas: Canvas, current: Int, y: Float) {
        if (mIsShowVerticalLine) {
            val point = getLeftAxisEndPoint()
            val originPoint = getOriginalPoint()
            val x = current + getLeftAxisEndPoint().x
            canvas.drawLine(
                x.toFloat(),
                originPoint.y.toFloat(),
                x.toFloat(),
                point.y.toFloat(),
                mVerticalLinePaint
            )
        }
    }

    // 绘制线条，所有线条最终进入这里绘制
    // 可能是X轴Y轴也有可能是刻度线
    private fun drawLine(canvas: Canvas, paint: Paint, start: Point, end: Point) {
        canvas.drawLine(
            start.x.toFloat(),
            start.y.toFloat(),
            end.x.toFloat(),
            end.y.toFloat(),
            paint
        )
    }

    //获取X，Y轴的起始点
    private fun getOriginalPoint(): Point {
        val point = Point()
        point.y = getBottomAxisEndPoint().y
        point.x = getLeftAxisEndPoint().x
        return point
    }

    // 获取左轴结束位置
    // 由于坐标系是从左下角开始的，所以结束位置为顶部
    private fun getLeftAxisEndPoint(): Point {
        var maxLength = 0
        // 获取x轴最长的数字
        mLeftAxisText.forEachIndexed { index, text ->
            if (text.toString().length > mLeftAxisText[maxLength].toString().length) {
                maxLength = index
            }
        }
        val maxText = mLeftAxisText[maxLength].toString()
        mLeftAxisTextPaint.getTextBounds(maxText, 0, maxText.length, commonRect)
        val padding = getTextPadding() * 2
        val x = commonRect.right + padding
        return Point(x.toInt(), 0)
    }

    // 获取底轴结束位置
    private fun getBottomAxisEndPoint(): Point {
        //参数Text可以随便设置，仅仅只是获取一下文字的高度而已
        mBottomAxisTextPaint.getTextBounds("A", 0, 1, commonRect)
        val y = (height + commonRect.top) - (getTextPadding() * 2)
        return Point(width, y.toInt())
    }

    //获取左轴的真实可用高度
    private fun getLeftAxisHeight(): Int {
        // 上边留白的部分，避免网格线绘制到最上面
        val paddingHeight = pxToSp(10.0F).toInt()
        return height - getLeftAxisEndPoint().x
    }

    //获取底轴的真实可用宽度
    private fun getBottomAxisWidth(): Int {
        val x = getLeftAxisEndPoint().x
        return width - x
    }

    private fun getTextPadding(): Float {
        return pxToSp(4.0F)
    }

    private fun pxToSp(pxValue: Float): Float {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (pxValue * fontScale)
    }

    interface OnClickListener {
        fun onClick(index: Int)
    }
}

