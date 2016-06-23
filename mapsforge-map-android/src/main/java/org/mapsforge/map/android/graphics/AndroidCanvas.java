/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014-2016 devemux86
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
package org.mapsforge.map.android.graphics;
  	
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Region;
  	
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Filter;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.map.model.DisplayModel;
  	
class AndroidCanvas implements Canvas {
  	private static final float[] INVERT_MATRIX = {
  	  	  	-1, 0, 0, 0, 255,
  	  	  	0, -1, 0, 0, 255,
  	  	  	0, 0, -1, 0, 255,
  	  	  	0, 0, 0, 1, 0
  	};
  	
  	android.graphics.Canvas canvas;
  	private final android.graphics.Paint bitmapPaint = new android.graphics.Paint();
  	private ColorFilter grayscaleFilter, grayscaleInvertFilter, invertFilter;
  	
  	AndroidCanvas() {
  	  	this.canvas = new android.graphics.Canvas();
  	  	createFilters();
  	}
  	
  	AndroidCanvas(android.graphics.Canvas canvas) {
  	  	this.canvas = canvas;
  	
  	  	createFilters();
  	}
  	
  	private void applyFilter(Filter filter) {
  	  	if (filter == Filter.NONE) {
  	  	  	return;
  	  	}
  	  	switch (filter) {
  	  	  	case GRAYSCALE:
  	  	  	  	bitmapPaint.setColorFilter(grayscaleFilter);
  	  	  	  	break;
  	  	  	case GRAYSCALE_INVERT:
  	  	  	  	bitmapPaint.setColorFilter(grayscaleInvertFilter);
  	  	  	  	break;
  	  	  	case INVERT:
  	  	  	  	bitmapPaint.setColorFilter(invertFilter);
  	  	  	  	break;
  	  	  	case NONE:
  	  	  	  	break;
  	  	}
  	}
  	
  	private void createFilters() {
  	  	ColorMatrix grayscaleMatrix = new ColorMatrix();
  	  	grayscaleMatrix.setSaturation(0);
  	  	grayscaleFilter = new ColorMatrixColorFilter(grayscaleMatrix);
  	
  	  	ColorMatrix grayscaleInvertMatrix = new ColorMatrix();
  	  	grayscaleInvertMatrix.setSaturation(0);
  	  	grayscaleInvertMatrix.postConcat(new ColorMatrix(INVERT_MATRIX));
  	  	grayscaleInvertFilter = new ColorMatrixColorFilter(grayscaleInvertMatrix);
  	
  	  	invertFilter = new ColorMatrixColorFilter(INVERT_MATRIX);
  	}
  	
  	@Override
  	public void destroy() {
  	  	this.canvas = null;
  	}
  	
  	@Override
  	public void drawBitmap(Bitmap bitmap, int left, int top) {
  	  	this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), left, top, bitmapPaint);
  	}
  	
  	@Override
  	public void drawBitmap(Bitmap bitmap, int left, int top, Filter filter) {
  	  	applyFilter(filter);
  	  	this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), left, top, bitmapPaint);
  	  	if (filter != Filter.NONE) {
  	  	  	bitmapPaint.setColorFilter(null);
  	  	}
  	}
  	
  	@Override
  	public void drawBitmap(Bitmap bitmap, Matrix matrix) {
  	  	this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), AndroidGraphicFactory.getMatrix(matrix), bitmapPaint);
  	}
  	
  	@Override
  	public void drawBitmap(Bitmap bitmap, Matrix matrix, Filter filter) {
  	  	applyFilter(filter);
  	  	this.canvas.drawBitmap(AndroidGraphicFactory.getBitmap(bitmap), AndroidGraphicFactory.getMatrix(matrix), bitmapPaint);
  	  	if (filter != Filter.NONE) {
  	  	  	bitmapPaint.setColorFilter(null);
  	  	}
  	}
  	
  	@Override
  	public void drawCircle(int x, int y, int radius, Paint paint) {
  	  	if (paint.isTransparent()) {
  	  	  	return;
  	  	}
  	  	this.canvas.drawCircle(x, y, radius, AndroidGraphicFactory.getPaint(paint));
  	}
  	
  	@Override
  	public void drawLine(int x1, int y1, int x2, int y2, Paint paint) {
  	  	if (paint.isTransparent()) {
  	  	  	return;
  	  	}
  	
  	  	this.canvas.drawLine(x1, y1, x2, y2, AndroidGraphicFactory.getPaint(paint));
  	}
  	
  	@Override
  	public void drawPath(Path path, Paint paint) {
  	  	if (paint.isTransparent()) {
  	  	  	return;
  	  	}
  	  	this.canvas.drawPath(AndroidGraphicFactory.getPath(path), AndroidGraphicFactory.getPaint(paint));
  	}
  	
  	@Override
  	public void drawText(String text, int x, int y, Paint paint) {
  	  	if (text == null || text.trim().isEmpty()) {
  	  	  	return;
  	  	}
  	  	if (paint.isTransparent()) {
  	  	  	return;
  	  	}
  	  	this.canvas.drawText(text, x, y, AndroidGraphicFactory.getPaint(paint));
  	}
  	
  	@Override
  	public void drawTextRotated(String text, int x1, int y1, int x2, int y2, Paint paint) {
  	  	if (text == null || text.trim().isEmpty()) {
  	  	  	return;
  	  	}
  	  	if (paint.isTransparent()) {
  	  	  	return;
  	  	}
  	
  	  	android.graphics.Path path = new android.graphics.Path();
  	  	path.moveTo(x1, y1);
  	  	path.lineTo(x2, y2);
  	  	this.canvas.drawTextOnPath(text, path, 0, 3, AndroidGraphicFactory.getPaint(paint));
  	}
  	
  	@Override
  	public void fillColor(Color color) {
  	  	this.canvas.drawColor(AndroidGraphicFactory.getColor(color), PorterDuff.Mode.CLEAR);
  	}
  	
  	@Override
  	public void fillColor(int color) {
  	  	this.canvas.drawColor(color);
  	}
  	
  	@Override
  	public Dimension getDimension() {
  	  	return new Dimension(getWidth(), getHeight());
  	}
  	
  	@Override
  	public int getHeight() {
  	  	return this.canvas.getHeight();
  	}
  	
  	@Override
  	public int getWidth() {
  	  	return this.canvas.getWidth();
  	}
  	
  	@Override
  	public void resetClip() {
  	  	this.canvas.clipRect(0, 0, getWidth(), getHeight(), Region.Op.REPLACE);
  	}
  	
  	@Override
  	public void setBitmap(Bitmap bitmap) {
  	  	this.canvas.setBitmap(AndroidGraphicFactory.getBitmap(bitmap));
  	}
  	
  	@Override
  	public void setClip(int left, int top, int width, int height) {
  	  	this.setClipInternal(left, top, width, height, Region.Op.REPLACE);
  	}
  	
  	@Override
  	public void setClipDifference(int left, int top, int width, int height) {
  	  	this.setClipInternal(left, top, width, height, Region.Op.DIFFERENCE);
  	}
  	
  	public void setClipInternal(int left, int top, int width, int height, Region.Op op) {
  	  	this.canvas.clipRect(left, top, left + width, top + height, op);
  	}
}