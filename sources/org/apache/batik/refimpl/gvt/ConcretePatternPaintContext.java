/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.refimpl.gvt;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.RenderContext;

import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.filter.AffineRable;
import org.apache.batik.gvt.filter.FilterRegion;
import org.apache.batik.gvt.filter.GraphicsNodeRable;
import org.apache.batik.gvt.filter.PadMode;
import org.apache.batik.gvt.filter.PadRable;

import org.apache.batik.refimpl.gvt.filter.ConcreteAffineRable;
import org.apache.batik.refimpl.gvt.filter.ConcreteGraphicsNodeRable;
import org.apache.batik.refimpl.gvt.filter.ConcretePadRable;

/**
 * <tt>PaintContext</tt> for the <tt>ConcretePatterPaint</tt>
 * paint implementation.
 *
 * @author <a href="vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 */
public class ConcretePatternPaintContext implements PaintContext {
    /**
     * ColorModel for the Rasters created by this Paint
     */
    private ColorModel rasterCM = ColorModel.getRGBdefault();

    /**
     * Working Raster
     */
    private WritableRaster raster;

    /**
     * Tile
     */
    private RenderedImage tile;
    
    /**
     * Tile info
     */
    private double tileX, tileY, tileWidth, tileHeight;

    private AffineTransform dev2usr;

    private AffineTransform padTxf;

    /**
     * @param destCM ColorModel that receives the paint data
     * @param deviceBounds device space bounds of the graphics primitive
     * @param paintedBounds user space bounds of the graphics primitive
     * @param usr2dev user space to device space transform
     * @param hints 
     * @param node GraphicsNode generating the pixel pattern
     * @param patternRegion region to which the pattern is constrained
     */
    public ConcretePatternPaintContext(ColorModel destCM,
                                       Rectangle deviceBounds,
                                       Rectangle2D paintedBounds,
                                       AffineTransform usr2dev,
                                       RenderingHints hints,
                                       GraphicsNode node,
                                       FilterRegion patternRegion){
        // System.out.println("paintedBounds : " + paintedBounds);

        if(paintedBounds == null){
            throw new IllegalArgumentException();
        }

        if(usr2dev == null){
            throw new IllegalArgumentException();
        }

        if(hints == null){
            hints = new RenderingHints(null);
        }

        if(node == null){
            throw new IllegalArgumentException();
        }

        if(patternRegion == null){
            throw new IllegalArgumentException();
        }

        Rectangle2D nodeBounds = node.getBounds();
        Rectangle2D patternBounds = patternRegion.getRegion();
        tileX = patternBounds.getX();
        tileY = patternBounds.getY();
        tileWidth = patternBounds.getWidth();
        tileHeight = patternBounds.getHeight();

        // System.out.println("nodeBounds    : " + nodeBounds);
        // System.out.println("patternBounds : " + patternBounds);

        AffineTransform adjustTxf = new AffineTransform();
        adjustTxf.translate(patternBounds.getX(),
                            patternBounds.getY());

        GraphicsNodeRable gnr 
            = new ConcreteGraphicsNodeRable(node);

        AffineRable atr 
            = new ConcreteAffineRable(gnr, adjustTxf);

        PadRable pad 
            = new ConcretePadRable(atr,
                                   patternBounds,
                                   PadMode.ZERO_PAD);

        padTxf = new AffineTransform(usr2dev);
        padTxf.concatenate(node.getTransform());

        RenderContext rc = new RenderContext(padTxf,
                                             patternBounds,
                                             hints);
        tile = pad.createRendering(rc);

        if(tile == null){
            WritableRaster wr = rasterCM.createCompatibleWritableRaster(32, 32);
            tile = new BufferedImage(rasterCM, wr, false, null);
        }

        try{
            dev2usr = padTxf.createInverse();
        }catch(NoninvertibleTransformException e){
            // Degenerate case. Use identity
            dev2usr = new AffineTransform();
        }
    }

    public void dispose(){
        raster = null;
    }

    public ColorModel getColorModel(){
        return rasterCM;
    }

    public Raster getRaster(int x, int y, int width, int height){
        if(raster == null){
            raster = rasterCM.createCompatibleWritableRaster(width, height);
        }
        else if(raster.getWidth() < width || 
                raster.getHeight() < height){
            raster = rasterCM.createCompatibleWritableRaster(width, height);
        }

        // System.out.println("getRaster : " + x + "/" + y + "/" + width + "/" + height);
        WritableRaster wr = raster.createWritableChild(0, 0, width, height, 0, 0, null);
        BufferedImage bi = new BufferedImage(rasterCM, wr, false, null);
        Graphics2D g = bi.createGraphics();
        g.setPaint(new Color(0, 0, 0, 0));
        g.setComposite(AlphaComposite.Src);
        g.fillRect(0, 0, width, height);
        g.setComposite(AlphaComposite.SrcOver);
        g.translate(-x, -y);

        Rectangle rasterBounds = new Rectangle(x, y, width, height);
        Rectangle2D rasterBounds2D = dev2usr.createTransformedShape(rasterBounds).getBounds();

        //
        // Do tiling in user space
        //
        double fx = rasterBounds2D.getX();
        double fy = rasterBounds2D.getY();
        double fw = rasterBounds2D.getWidth();
        double fh = rasterBounds2D.getHeight();

        // System.out.println("rasterBounds in user space : " + fx + "/" + fy 
        //+ "/" + fw + "/" + fh);

        // (rx, ry) is the current tile's origin, in translated user space,
        // (i.e., translated by (-fx, -fy).
        // It will be moved to different location to cover the whole
        // raster.
        double rx = (fx - tileX)%tileWidth;
        double ry = (fy - tileY)%tileHeight;

        if(rx > 0){
            rx *= -1;
        }
        else if(rx < 0){
            rx = -tileWidth - rx;
        }

        if(ry > 0){
            ry *= -1;
        }
        else if(ry < 0){
            ry = -tileHeight - ry;
        }

        Point2D.Double tileOrigin 
            = new Point2D.Double(tileWidth, 0);

        padTxf.deltaTransform(tileOrigin, tileOrigin);
        int txx = (int)Math.floor(tileOrigin.x);
        int txy = (int)Math.floor(tileOrigin.y);

        tileOrigin.x = 0;
        tileOrigin.y = tileHeight;

        padTxf.deltaTransform(tileOrigin, tileOrigin);
        int tyx = (int)Math.floor(tileOrigin.x);
        int tyy = (int)Math.floor(tileOrigin.y);

        double curX = rx, curY = ry;
        // System.out.println("tileWidth/tileHeight - txx/txy/tyx/tyy : " + tileWidth + "/" + tileHeight + " - " + txx + "/" + txy + "/" + tyx + "/" + tyy);

        while(curY < fh){
            while(curX < fw){
                // Paint a tile with upper left corner in 
                // (fx + curX, fy + curY) in user space. 
                tileOrigin.x = fx + curX;
                tileOrigin.y = fy + curY;

                tileOrigin.x = tileOrigin.x - tileX;
                tileOrigin.y = tileOrigin.y - tileY;
                
                double nTileX = Math.round(tileOrigin.x / tileWidth);
                double nTileY = Math.round(tileOrigin.y / tileHeight);
                
                // System.out.println("Tile : " + nTileX + "/" + nTileY);

                AffineTransform tileTxfBad
                = AffineTransform.getTranslateInstance(txx*nTileX + tyx*nTileY, 
                                                       txy*nTileX + tyy*nTileY);

                padTxf.deltaTransform(tileOrigin, tileOrigin);

                AffineTransform tileTxf 
                    = AffineTransform.getTranslateInstance(Math.floor(tileOrigin.x),
                                                           Math.floor(tileOrigin.y));
                

                // System.out.println("Good : " + tileTxf.getTranslateX() + "/" + tileTxf.getTranslateY());
                // System.out.println("Bad  : " + tileTxfBad.getTranslateX() + "/" + tileTxfBad.getTranslateY());
                /*double nTileX = Math.floor(tileOrigin.x / tileWidth);
                double nTileY = Math.floor(tileOrigin.y / tileHeight);
                AffineTransform tileTxf 
                = AffineTransform.getTranslateInstance(nTileX*tx, nTileY*ty);*/

                g.drawRenderedImage(tile, tileTxfBad);

                curX += tileWidth;
            }
            curY += tileHeight;
            curX = rx;
        }

        return wr;

    }
}
