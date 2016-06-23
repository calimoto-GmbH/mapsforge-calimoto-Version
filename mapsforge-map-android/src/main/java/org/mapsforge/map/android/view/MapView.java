/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
 * Copyright 2015 Andreas Schildbach
 * Copyright 2016 Sebastian Dambeck & Luca Osten
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.map.android.view;
  	
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
  	
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.input.TouchGestureHandler;
import org.mapsforge.map.controller.FrameBufferController;
import org.mapsforge.map.controller.LayerManagerController;
import org.mapsforge.map.controller.MapViewController;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.labels.LabelStore;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.scalebar.MapScaleBar;
import org.mapsforge.map.util.MapPositionUtil;
import org.mapsforge.map.util.MapViewProjection;
import org.mapsforge.map.view.FpsCounter;
import org.mapsforge.map.view.FrameBuffer;
  	
public class MapView extends ViewGroup implements org.mapsforge.map.view.MapView, Observer {
  	
  	/**
  	 * Child view Layout information associated with MapView.
  	 */
  	public static class LayoutParams extends ViewGroup.LayoutParams {
  	
  	  	/**
  	  	 * Special values for the alignment requested by child views.
  	  	 */
  	  	public enum Alignment {
  	  	  	TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
  	  	}
  	
  	  	/**
  	  	 * The location of the child view within the map view.
  	  	 */
  	  	public LatLong latLong;
  	
  	  	/**
  	  	 * The alignment of the view compared to the location.
  	  	 */
  	  	public Alignment alignment;
  	
  	  	public LayoutParams(Context c, AttributeSet attrs) {
  	  	  	super(c, attrs);
  	  	  	this.alignment = LayoutParams.Alignment.BOTTOM_CENTER;
  	  	}
  	
  	  	/**
  	  	 * Creates a new set of layout parameters for a child view of MapView.
  	  	 *
  	  	 * @param width  	 the width of the child, either {@link #MATCH_PARENT}, {@link #WRAP_CONTENT} or a fixed size in pixels.
  	  	 * @param height  	the height of the child, either {@link #MATCH_PARENT}, {@link #WRAP_CONTENT} or a fixed size in pixels.
  	  	 * @param latLong   the location of the child within the map view.
  	  	 * @param alignment the alignment of the view compared to the location.
  	  	 */
  	  	public LayoutParams(int width, int height, LatLong latLong, Alignment alignment) {
  	  	  	super(width, height);
  	  	  	this.latLong = latLong;
  	  	  	this.alignment = alignment;
  	  	}
  	
  	  	public LayoutParams(ViewGroup.LayoutParams source) {
  	  	  	super(source);
  	  	}
  	}
  	
  	private static final GraphicFactory GRAPHIC_FACTORY = AndroidGraphicFactory.INSTANCE;
  	
  	private final FrameBuffer frameBuffer;
  	private final FrameBufferController frameBufferController;
  	private final GestureDetector gestureDetector;
  	private GestureDetector gestureDetectorExternal;
  	private final LayerManager layerManager;
  	private final Handler layoutHandler = new Handler();
  	private final MapViewProjection mapViewProjection;
  	private final Model model;
  	private final ScaleGestureDetector scaleGestureDetector;
  	private final TouchGestureHandler touchGestureHandler;
  	private final Matrix matrixRotateEvent = new Matrix();
  	
  	public MapView(Context context) {
  	  	this(context, null);
  	}
  	
  	public MapView(Context context, AttributeSet attributeSet) {
  	  	super(context, attributeSet);
  	
  	  	setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
  	  	setWillNotDraw(false);
  	
  	  	this.model = new Model();
  	
  	  	this.frameBuffer = new FrameBuffer(this.model.frameBufferModel, this.model.displayModel, GRAPHIC_FACTORY);
  	  	this.frameBufferController = FrameBufferController.create(this.frameBuffer, this.model);
  	
  	  	this.layerManager = new LayerManager(this, this.model.mapViewPosition, GRAPHIC_FACTORY);
  	  	this.layerManager.start();
  	  	LayerManagerController.create(this.layerManager, this.model);
  	
  	  	MapViewController.create(this, this.model);
  	
  	  	this.touchGestureHandler = new TouchGestureHandler(this);
  	  	this.gestureDetector = new GestureDetector(context, touchGestureHandler);
  	  	this.scaleGestureDetector = new ScaleGestureDetector(context, touchGestureHandler);
  	
  	  	this.mapViewProjection = new MapViewProjection(this);
  	
  	  	model.mapViewPosition.addObserver(this);
  	}
  	
  	@Override
  	public void addLayer(Layer layer) {
  	  	this.layerManager.getLayers().add(layer);
  	}
  	
  	@Override
  	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
  	  	return (p instanceof MapView.LayoutParams);
  	}
  	
  	/**
  	 * Clear map view.
  	 */
  	@Override
  	public void destroy() {
  	  	this.touchGestureHandler.destroy();
  	  	this.layoutHandler.removeCallbacksAndMessages(null);
  	  	this.layerManager.interrupt();
  	  	this.frameBufferController.destroy();
  	  	this.frameBuffer.destroy();
  	  	this.getModel().mapViewPosition.destroy();
  	}
  	
  	/**
  	 * Clear all map view elements.<br/>
  	 * i.e. layers, tile cache, label store, map view, resources, etc.
  	 */
  	@Override
  	public void destroyAll() {
  	  	for (Layer layer : this.layerManager.getLayers()) {
  	  	  	this.layerManager.getLayers().remove(layer);
  	  	  	layer.onDestroy();
  	  	  	if (layer instanceof TileLayer) {
  	  	  	  	((TileLayer<?>) layer).getTileCache().destroy();
  	  	  	}
  	  	  	if (layer instanceof TileRendererLayer) {
  	  	  	  	LabelStore labelStore = ((TileRendererLayer) layer).getLabelStore();
  	  	  	  	if (labelStore != null) {
  	  	  	  	  	labelStore.clear();
  	  	  	  	}
  	  	  	}
  	  	}
  	  	destroy();
  	  	AndroidGraphicFactory.clearResourceMemoryCache();
  	}
  	
  	@Override
  	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
  	  	return new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
  	  	  	  	null, MapView.LayoutParams.Alignment.BOTTOM_CENTER);
  	}
  	
  	@Override
  	public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
  	  	return new MapView.LayoutParams(getContext(), attrs);
  	}
  	
  	@Override
  	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
  	  	return new MapView.LayoutParams(p);
  	}
  	
  	@Override
  	public BoundingBox getBoundingBox() {
  	  	return MapPositionUtil.getBoundingBox(this.model.mapViewPosition.getMapPosition(),
  	  	  	  	getDimension(), this.model.displayModel.getTileSize());
  	}
  	
  	@Override
  	public Dimension getDimension() {
  	  	return new Dimension(getWidth(), getHeight());
  	}
  	
  	@Override
  	public FpsCounter getFpsCounter() {
  	  	return null;
  	}
  	
  	@Override
  	public FrameBuffer getFrameBuffer() {
  	  	return this.frameBuffer;
  	}
  	
  	@Override
  	public LayerManager getLayerManager() {
  	  	return this.layerManager;
  	}
  	
  	@Override
  	public MapScaleBar getMapScaleBar() {
  	  	return null;
  	}
  	
  	@Override
  	public MapViewProjection getMapViewProjection() {
  	  	return this.mapViewProjection;
  	}
  	
  	@Override
  	public Model getModel() {
  	  	return this.model;
  	}
  	
  	public TouchGestureHandler getTouchGestureHandler() {
  	  	return touchGestureHandler;
  	}
  	
  	@Override
  	public void onChange() {
  	  	// Request layout for child views (besides zoom controls)
  	  	layoutHandler.post(new Runnable() {
  	  	  	@Override
  	  	  	  	public void run() {
  	  	  	  	  	requestLayout();
  	  	  	  	}
  	  	});
  	}
  	
  	@Override
  	protected void onDraw(Canvas androidCanvas) {
  	  	org.mapsforge.core.graphics.Canvas graphicContext = AndroidGraphicFactory.createGraphicContext(androidCanvas);
  	  	this.frameBuffer.draw(graphicContext);
  	  	graphicContext.destroy();
  	}
  	
  	@Override
  	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
  	
  	  	// Child views (besides zoom controls)
  	  	int count = getChildCount();
  	  	for (int i = 0; i < count; i++) {
  	  	  	View child = getChildAt(i);
  	  	  	if (child.getVisibility() != View.GONE && checkLayoutParams(child.getLayoutParams())) {
  	  	  	  	MapView.LayoutParams params = (MapView.LayoutParams) child.getLayoutParams();
  	  	  	  	int childWidth = child.getMeasuredWidth();
  	  	  	  	int childHeight = child.getMeasuredHeight();
  	  	  	  	Point point = mapViewProjection.toPixels(params.latLong);
  	  	  	  	if (point != null) {
  	  	  	  	  	int childLeft = getPaddingLeft() + (int) Math.round(point.x);
  	  	  	  	  	int childTop = getPaddingTop() + (int) Math.round(point.y);
  	  	  	  	  	switch (params.alignment) {
  	  	  	  	  	  	case TOP_LEFT:
  	  	  	  	  	  	  	break;
  	  	  	  	  	  	case TOP_CENTER:
  	  	  	  	  	  	  	childLeft -= childWidth / 2;
  	  	  	  	  	  	  	break;
  	  	  	  	  	  	case TOP_RIGHT:
  	  	  	  	  	  	  	childLeft -= childWidth;
  	  	  	  	  	  	  	break;
  	  	  	  	  	  	case CENTER_LEFT:
  	  	  	  	  	  	  	childTop -= childHeight / 2;
  	  	  	  	  	  	  	break;
  	  	  	  	  	  	case CENTER:
  	  	  	  	  	  	  	childLeft -= childWidth / 2;
  	  	  	  	  	  	  	childTop -= childHeight / 2;
  	  	  	  	  	  	  	break;
  	  	  	  	  	  	case CENTER_RIGHT:
  	  	  	  	  	  	  	childLeft -= childWidth;
  	  	  	  	  	  	  	childTop -= childHeight / 2;
  	  	  	  	  	  	  	break;
  	  	  	  	  	  	case BOTTOM_LEFT:
  	  	  	  	  	  	  	childTop -= childHeight;
  	  	  	  	  	  	  	break;
  	  	  	  	  	  	case BOTTOM_CENTER:
  	  	  	  	  	  	  	childLeft -= childWidth / 2;
  	  	  	  	  	  	  	childTop -= childHeight;
  	  	  	  	  	  	  	break;
  	  	  	  	  	  	case BOTTOM_RIGHT:
  	  	  	  	  	  	  	childLeft -= childWidth;
  	  	  	  	  	  	  	childTop -= childHeight;
  	  	  	  	  	  	  	break;
  	  	  	  	  	}
  	  	  	  	  	child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
  	  	  	  	}
  	  	  	}
  	  	}
  	}
  	
  	@Override
  	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
  	  	measureChildren(widthMeasureSpec, heightMeasureSpec);
  	  	super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  	}
  	
  	@Override
  	protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
  	  	this.model.mapViewDimension.setDimension(new Dimension(width, height));
  	}
  	
  	@Override
  	public boolean dispatchTouchEvent(MotionEvent event) {
  	  	if (this.layerManager.getRotation() != 0) {
  	  	  	matrixRotateEvent.reset();
  	  	  	matrixRotateEvent.setRotate(-this.layerManager.getRotation(), getWidth()/2, getHeight()/2);
  	  	  	event.transform(matrixRotateEvent);
  	  	}
  	  	return super.dispatchTouchEvent(event);
  	}
  	
  	@Override
  	public boolean onTouchEvent(MotionEvent event) {
  	  	if (!isClickable()) {
  	  	  	return false;
  	  	}
  	
  	  	if (this.gestureDetectorExternal != null && this.gestureDetectorExternal.onTouchEvent(event)) {
  	  	  	return true;
  	  	}
  	
  	  	boolean retVal = this.scaleGestureDetector.onTouchEvent(event);
  	  	if (!this.scaleGestureDetector.isInProgress()) {
  	  	  	retVal = this.gestureDetector.onTouchEvent(event);
  	  	}
  	  	return retVal;
  	}
  	
  	@Override
  	public void repaint() {
  	  	if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
  	  	  	invalidate();
  	  	} else {
  	  	  	postInvalidate();
  	  	}
  	}
  	
  	@Override
  	public void setCenter(LatLong center) {
  	  	this.model.mapViewPosition.setCenter(center);
  	}
  	
  	public void setGestureDetector(GestureDetector gestureDetector) {
  	  	this.gestureDetectorExternal = gestureDetector;
  	}
  	
  	@Override
  	public void setMapScaleBar(MapScaleBar mapScaleBar) {
  	  	// do nothing
  	}
  	
  	@Override
  	public void setZoomLevel(byte zoomLevel) {
  	  	this.model.mapViewPosition.setZoomLevel(zoomLevel);
  	}
  	
  	@Override
  	public void setZoomLevelMax(byte zoomLevelMax) {
  	  	this.model.mapViewPosition.setZoomLevelMax(zoomLevelMax);
  	}
  	
  	@Override
  	public void setZoomLevelMin(byte zoomLevelMin) {
  	  	this.model.mapViewPosition.setZoomLevelMin(zoomLevelMin);
  	}
}