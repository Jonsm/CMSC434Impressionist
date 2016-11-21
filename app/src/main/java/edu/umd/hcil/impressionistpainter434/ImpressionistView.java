package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private float _defaultSpeed = .5f;
    int _currentRadius = _defaultRadius;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private Paint _paintBorder = new Paint();
    private Paint _canvasPaint = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;
    private int _splatterCount = 7;
    private float _splatterRadiusFactor = 4.5f;

    private ArrayList<Bitmap> _historyList = new ArrayList<Bitmap> ();
    private int _historyPosition = 0;
    private boolean _lastChangeAdded = true;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        _historyList.clear();
        _historyPosition = 0;
        _lastChangeAdded = true;
        clearHelper();
    }

    private void clearHelper() {
        _offScreenCanvas = null;
        _offScreenBitmap = null;
        _lastPoint = null;
        _lastPointTime = -1;
        invalidate();
    }

    /*
     * saves the painting
     */
    public void savePainting(String title, String description, Context context) {
        if (_offScreenBitmap != null) {
            MediaStore.Images.Media.insertImage(context.getContentResolver(), _offScreenBitmap, title, description);
        }
    }

    /*
     * undoes last change
     */
    public void undo() {
        if (_lastChangeAdded == false) {
            addToHistoryList();
            _lastChangeAdded = true;
        }

        if (_historyPosition > 0) {
            clearHelper();
            _historyPosition--;
            if (_historyPosition > 0) {
                _offScreenBitmap = copyBitmap(_historyList.get(_historyPosition - 1));
                _offScreenCanvas = new Canvas(_offScreenBitmap);
            }
            invalidate();
        }
        System.out.println(_historyList.size());
        System.out.println(_historyPosition);
    }

    /*
     * redoes last undo
     */
    public void redo() {
        if (_lastChangeAdded == false) {
            addToHistoryList();
            _lastChangeAdded = true;
        }

        if (_historyPosition < _historyList.size()) {
            clearHelper();
            _historyPosition++;
            _offScreenBitmap = copyBitmap(_historyList.get(_historyPosition - 1));
            _offScreenCanvas = new Canvas(_offScreenBitmap);
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (_lastPoint != null) {
            if (_offScreenBitmap == null) _offScreenBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
            if (_offScreenCanvas == null) {
                _offScreenCanvas = new Canvas(_offScreenBitmap);
                _offScreenCanvas.drawColor(Color.WHITE);
            }
            _offScreenCanvas.drawBitmap(_offScreenBitmap, 0, 0, _canvasPaint);

            switch (_brushType) {
                case Circle:
                    _offScreenCanvas.drawCircle(_lastPoint.x, _lastPoint.y, _currentRadius, _paint);
                    break;
                case CircleSplatter:
                    for (int i = 0; i < _splatterCount; i++) {
                        int xDisp = _lastPoint.x + (int)(Math.random() * _currentRadius * _splatterRadiusFactor / 2);
                        int yDisp = _lastPoint.y + (int)(Math.random() * _currentRadius * _splatterRadiusFactor / 2);
                        int radius = (int)(Math.random()*_currentRadius);
                        _offScreenCanvas.drawCircle(xDisp, yDisp, radius, _paint);
                    }
                    break;
                case Square:
                    //the save-rotate-restore trick comes from
                    //http://stackoverflow.com/questions/36606463/drawing-bunch-of-rotated-rectangles-on-android-canvas
                    _offScreenCanvas.save();
                    _offScreenCanvas.rotate((float) Math.random() * 360, _lastPoint.x, _lastPoint.y);
                    _offScreenCanvas.drawRect(_lastPoint.x - _currentRadius / 2, _lastPoint.y + _currentRadius / 2,
                            _lastPoint.x + _currentRadius / 2, _lastPoint.y - _currentRadius / 2, _paint);
                    _offScreenCanvas.restore();
                    break;
            }
        }

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _canvasPaint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();

        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN || motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            if (_imageView != null && _imageView.getDrawable() != null) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN && _offScreenBitmap != null && !_lastChangeAdded) {
                    addToHistoryList();
                }
                _lastChangeAdded = false;

                Rect viewRect = getBitmapPositionInsideImageView(_imageView);
                Bitmap image = ((BitmapDrawable) _imageView.getDrawable()).getBitmap();
                int xCoord = (int) Math.max(0, (touchX - viewRect.left) * image.getWidth() / viewRect.width());
                xCoord = (int) Math.min(xCoord, image.getWidth() - 1);
                int yCoord = (int) Math.max(0, (touchY - viewRect.top) * image.getHeight() / viewRect.height());
                yCoord = (int) Math.min(yCoord, image.getHeight() - 1);
                _paint.setColor(image.getPixel(xCoord, yCoord));
                _paint.setAlpha(_alpha);

                if (_lastPointTime == -1) {
                    _currentRadius = _defaultRadius;
                } else {
                    float dx = (float) Math.sqrt(Math.pow(_lastPoint.x - touchX, 2) + Math.pow(_lastPoint.y - touchY, 2));
                    float dt = System.currentTimeMillis() - _lastPointTime;
                    _currentRadius = (int) Math.max(Math.sqrt((dx / dt) / _defaultSpeed) * _defaultRadius, _minBrushRadius);
                }

                _lastPointTime = System.currentTimeMillis();
                _lastPoint = new Point((int) touchX, (int) touchY);
                invalidate();
            }
        }


        return true;
    }

    //for undo and redo
    private void addToHistoryList () {
        Bitmap copy = copyBitmap(_offScreenBitmap);
        System.out.println("adding");
        while (_historyPosition < _historyList.size()) {
            System.out.println("removing");
            _historyList.remove(_historyList.size() - 1);
        }
        _historyList.add(copy);
        _historyPosition++;
    }

    //deep copies a bitmap
    private Bitmap copyBitmap (Bitmap bitmap) {
        Bitmap copy = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(copy);
        canvas2.drawBitmap(bitmap, 0, 0, _canvasPaint);
        return copy;
    }

    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

