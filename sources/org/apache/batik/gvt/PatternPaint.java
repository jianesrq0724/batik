/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.gvt;

import java.awt.Paint;
import java.awt.geom.Rectangle2D;

/**
 * This extension of the <tt>Paint</tt> interface uses the 
 * referenced <tt>GraphicsNode</tt>, constrained to the 
 * input <tt>FilterRegion</tt> produce its pixel pattern.
 *
 * @author <a href="mailto:vincent.hardy@eng.sun.com">Vincent Hardy</a>
 * @version $Id$
 */
public interface PatternPaint extends Paint {
    /**
     * Returns the <tt>GraphicsNode</tt> that is used to 
     * produce the pixel pattern generated by this <tt>Paint</tt>
     */
    public GraphicsNode getGraphicsNode();

    /**
     * Returns the area to which this paint is constrained
     */
    public Rectangle2D getPatternRect();
}
