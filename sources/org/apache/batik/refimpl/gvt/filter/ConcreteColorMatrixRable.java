/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.refimpl.gvt.filter;

import org.apache.batik.gvt.filter.Filter;
import org.apache.batik.gvt.filter.CachableRed;
import org.apache.batik.gvt.filter.ColorMatrixRable;

import java.awt.Point;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.BandCombineOp;
import java.awt.image.ColorModel;
import java.awt.image.renderable.RenderContext;

/**
 * Implements the interface expected from a color matrix
 * operation
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 */
public class ConcreteColorMatrixRable 
    extends  AbstractRable
    implements ColorMatrixRable {
    /**
     * Predefined luminanceToAlpha matrix
     */
    private static float MATRIX_LUMINANCE_TO_ALPHA[][] 
        = { 
            {0,       0,       0,       0, 0},
            {0,       0,       0,       0, 0},
            {0,       0,       0,       0, 0},
            {0.2125f, 0.7154f, 0.0721f, 0, 0}
        };

    /**
     * This matrix type
     */
    private int type;

    /**
     * The matrix
     */
    private float matrix[][];

    /**
     * Sets the source of the blur operation
     */
    public void setSource(Filter src){
        init(src, null);
    }

    /**
     * Returns the source of the blur operation
     */
    public Filter getSource(){
        return (Filter)getSources().get(0);
    }

    /**
     * Returns the type of this color matrix. 
     * @return one of TYPE_MATRIX, TYPE_SATURATE, TYPE_HUE_ROTATE,
     *         TYPE_LUMINANCE_TO_ALPHA
     */
    public int getType(){
        return type;
    }

    /**
     * Returns the rows of the color matrix. This uses
     * the same convention as BandCombineOp.
     */
    public float[][] getMatrix(){
        return matrix;
    }

    /**
     * Instances should be built through the static 
     * factory methods
     */
    private ConcreteColorMatrixRable(){
    }

    /**
     * Builds a TYPE_MATRIX instance
     */
    public static ColorMatrixRable buildMatrix(float matrix[][]){
        if(matrix == null){
            throw new IllegalArgumentException();
        }

        if(matrix.length != 4){
            throw new IllegalArgumentException();
        }

        float newMatrix[][] = new float[4][];
        
        for(int i=0; i<4; i++){
            float m[] = matrix[i];
            if(m == null){
                throw new IllegalArgumentException();
            }
            if(m.length != 5){
                throw new IllegalArgumentException();
            }
            newMatrix[i] = new float[5];
            for(int j=0; j<5; j++){
                newMatrix[i][j] = m[j];
            }
        }

        /*for(int i=0; i<4; i++){
            for(int j=0; j<5; j++)
                System.out.print(newMatrix[i][j] + " ");
            System.out.println();
            }*/

        ConcreteColorMatrixRable filter 
            = new ConcreteColorMatrixRable();
        filter.type = TYPE_MATRIX;
        filter.matrix = newMatrix;
        return filter;
    }

    /**
     * Builds a TYPE_SATURATE instance
     */
    public static ColorMatrixRable buildSaturate(float s){
        ConcreteColorMatrixRable filter 
            = new ConcreteColorMatrixRable();
        filter.type = TYPE_SATURATE;
        filter.matrix = new float[][] {
            { 0.213f+0.787f*s,  0.715f-0.715f*s, 0.072f-0.072f*s, 0, 0 },
            { 0.213f-0.213f*s,  0.715f+0.285f*s, 0.072f-0.072f*s, 0, 0 },
            { 0.213f-0.213f*s,  0.715f-0.715f*s, 0.072f+0.928f*s, 0, 0 },
            { 0,                0,               0,               1, 0 }
        };
        return filter;
    }

    /**
     * Builds a TYPE_HUE_ROTATE instance.
     * @param a angle, in radian
     */
    public static ColorMatrixRable buildHueRotate(float a){
        ConcreteColorMatrixRable filter 
            = new ConcreteColorMatrixRable();
        filter.type = TYPE_HUE_ROTATE;

        float cos = (float)Math.cos(a);
        float sin = (float)Math.sin(a);

        // System.out.println("sin : " + sin + " cos : " + cos);

        float a00 = 0.213f + cos*0.787f - sin*0.213f;
        float a10 = 0.213f - cos*0.212f + sin*0.143f;
        float a20 = 0.213f - cos*0.213f - sin*0.787f;

        float a01 = 0.715f - cos*0.715f - sin*0.715f;
        float a11 = 0.715f + cos*0.285f + sin*0.140f;
        float a21 = 0.715f - cos*0.715f + sin*0.715f;

        float a02 = 0.072f - cos*0.072f + sin*0.928f;
        float a12 = 0.072f - cos*0.072f - sin*0.283f;
        float a22 = 0.072f + cos*0.928f + sin*0.072f;

        filter.matrix = new float[][] {
            { a00, a01, a02, 0, 0 },
            { a10, a11, a12, 0, 0 },
            { a20, a21, a22, 0, 0 },
            { 0,   0,   0,   1, 0 }};

        /*for(int i=0; i<4; i++){
            for(int j=0; j<5; j++)
                System.out.print(filter.matrix[i][j] + " ");
            System.out.println();
            }*/

        return filter;
    }

    /**
     * Builds a TYPE_LUMINANCE_TO_ALPHA instance
     */
    public static ColorMatrixRable buildLuminanceToAlpha(){
        ConcreteColorMatrixRable filter 
            = new ConcreteColorMatrixRable();
        filter.type = TYPE_LUMINANCE_TO_ALPHA;
        filter.matrix = MATRIX_LUMINANCE_TO_ALPHA;
        return filter;
    }        

    public RenderedImage createRendering(RenderContext rc){
        //
        // Get source's rendered image
        //
        RenderedImage srcRI = getSource().createRendering(rc);

        if(srcRI == null){
            return null;
        }

        final int srcMinX = srcRI.getMinX();
        final int srcMinY = srcRI.getMinY();

        RenderingHints hints = rc.getRenderingHints();
        ColorMatrixOp op = new ColorMatrixOp(matrix, null);

        //
        // Wrap source in buffered image
        //
        ColorModel cm = srcRI.getColorModel();
        Raster srcRR = srcRI.getData();
        Point origin = new Point(0, 0);
        WritableRaster srcWR = Raster.createWritableRaster(srcRR.getSampleModel(),
                                                           srcRR.getDataBuffer(),
                                                           origin);
        /*
        BufferedImage srcBI = new BufferedImage(cm,
                                                srcWR,
                                                cm.isAlphaPremultiplied(),
                                                null);*/

        WritableRaster dstWR = op.filter(srcWR, srcWR);

        BufferedImage dstBI = new BufferedImage(cm,
                                                dstWR,
                                                cm.isAlphaPremultiplied(),
                                                null);

        CachableRed cr = new ConcreteBufferedImageCachableRed(dstBI){
                public int getMinX(){
                    return srcMinX;
                }

                public int getMinY(){
                    return srcMinY;
                }
            };

        return cr;
    }

}
