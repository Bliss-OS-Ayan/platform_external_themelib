/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.lwawt.macosx;

import java.awt.*;

public class CCustomCursor extends Cursor {
    static Dimension sMaxCursorSize;
    static Dimension getMaxCursorSize() {
        if (sMaxCursorSize != null) return sMaxCursorSize;
        final Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        return sMaxCursorSize = new Dimension(bounds.width / 2, bounds.height / 2);
    }

    Image fImage;
    Point fHotspot;

    public CCustomCursor(final Image cursor, final Point hotSpot, final String name) throws IndexOutOfBoundsException, HeadlessException {
        super(name);
        fImage = cursor;
        fHotspot = hotSpot;

        // This chunk of code is copied from sun.awt.CustomCursor
        final Toolkit toolkit = Toolkit.getDefaultToolkit();

        // Make sure image is fully loaded.
        final Component c = new Canvas(); // for its imageUpdate method
        final MediaTracker tracker = new MediaTracker(c);
        tracker.addImage(fImage, 0);
        try {
            tracker.waitForAll();
        } catch (final InterruptedException e) {}

        int width = fImage.getWidth(c);
        int height = fImage.getHeight(c);

        // Fix for bug 4212593 The Toolkit.createCustomCursor does not
        // check absence of the image of cursor
        // If the image is invalid, the cursor will be hidden (made completely
        // transparent). In this case, getBestCursorSize() will adjust negative w and h,
        // but we need to set the hotspot inside the image here.
        if (tracker.isErrorAny() || width < 0 || height < 0) {
            fHotspot.x = fHotspot.y = 0;
        }

        // Scale image to nearest supported size
        final Dimension nativeSize = toolkit.getBestCursorSize(width, height);
        if (nativeSize.width != width || nativeSize.height != height) {
            fImage = fImage.getScaledInstance(nativeSize.width, nativeSize.height, Image.SCALE_DEFAULT);
            width = nativeSize.width;
            height = nativeSize.height;
        }

        // NOTE: this was removed for 3169146, but in 1.5 the JCK tests for an exception and fails if one isn't thrown.
        // See what JBuilder does.
        // Verify that the hotspot is within cursor bounds.
        if (fHotspot.x >= width || fHotspot.y >= height || fHotspot.x < 0 || fHotspot.y < 0) {
            throw new IndexOutOfBoundsException("invalid hotSpot");
        }

        // Must normalize the hotspot
        if (fHotspot.x >= width) {
            fHotspot.x = width - 1; // it is zero based.
        } else if (fHotspot.x < 0) {
            fHotspot.x = 0;
        }
        if (fHotspot.y >= height) {
            fHotspot.y = height - 1; // it is zero based.
        } else if (fHotspot.y < 0) {
            fHotspot.y = 0;
        }
    }

    public static Dimension getBestCursorSize(final int preferredWidth, final int preferredHeight) {
        // With Panther, cursors have no limit on their size. So give the client their
        // preferred size, but no larger than half the dimensions of the main screen
        // This will allow large cursors, but not cursors so large that they cover the
        // screen. Since solaris nor windows allow cursors this big, this shouldn't be
        // a limitation.
        // JCK triggers an overflow in the int -- if we get a bizarre value normalize it.
        final Dimension maxCursorSize = getMaxCursorSize();
        final Dimension d = new Dimension(Math.max(1, Math.abs(preferredWidth)), Math.max(1, Math.abs(preferredHeight)));
        return new Dimension(Math.min(d.width, maxCursorSize.width), Math.min(d.height, maxCursorSize.height));
    }

    // Called from native when the cursor is set
    // Returns long array of [NSImage ptr, x hotspot, y hotspot]
    CImage fCImage;
    long getImageData() {
        if (fCImage == null) {
            try {
                fCImage = CImage.getCreator().createFromImage(fImage);
            } catch (IllegalArgumentException iae) {
                // Silently return null - we want to hide cursor by providing an empty
                // ByteArray or just null
                return 0L;
            }
        }

        return fCImage.ptr;
    }

    Point getHotSpot() {
        return fHotspot;
    }
}
