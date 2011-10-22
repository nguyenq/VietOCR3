/**
 * Copyright @ 2010 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.vietpad.utilities;

import java.awt.*;
import java.awt.Point;
import javax.swing.text.*;

/**
 * Adapted from http://forums.sun.com/thread.jspa?forumID=57&threadID=708866
 */
public class WavyLineHighlighter extends DefaultHighlighter.DefaultHighlightPainter {

    public WavyLineHighlighter(Color color) {
        super(color);
    }

    @Override
    public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
        Color color = getColor();

        if (color == null) {
            g.setColor(c.getSelectionColor());
        } else {
            g.setColor(color);
        }

        if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
            // Contained in view, can just use bounds.
            Rectangle alloc;
            if (bounds instanceof Rectangle) {
                alloc = (Rectangle) bounds;
            } else {
                alloc = bounds.getBounds();
            }
            drawWaveLine(g, alloc.x, alloc.y + alloc.height - 2, alloc.x + alloc.width - 1, alloc.y + alloc.height - 2);
            return alloc;
        } else {
            // Should only render part of View.
            try {
                // --- determine locations ---
                Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
                Rectangle rect = (shape instanceof Rectangle)? (Rectangle) shape : shape.getBounds();
                drawWaveLine(g, rect.x, rect.y + rect.height - 2, rect.x + rect.width - 1, rect.y + rect.height - 2);
                return rect;
            } catch (BadLocationException e) {
                // can't render
            }
        }

        // Only if exception
        return null;
    }

    void drawWaveLine(Graphics g, Point start, Point end) {
        if ((end.x - start.x) > 2) {
            int points = (end.x - start.x) / 2 + 1;
            int[] xPoints = new int[points];
            int[] yPoints = new int[points];
            boolean down = true;

            int index = 0;
            for (int i = start.x; i <= end.x; i += 2) {
                if (down) {
                    xPoints[index] = i;
                    yPoints[index] = start.y;
                } else {
                    xPoints[index] = i;
                    yPoints[index] = start.y + 2;
                }
                index++;
                down ^= true;
            }
            g.drawPolyline(xPoints, yPoints, points);
        } else {
            g.drawPolyline(new int[]{start.x, end.x}, new int[]{start.y, end.y}, 2);
        }
    }

    void drawWaveLine(Graphics g, int startX, int startY, int endX, int endY) {
        drawWaveLine(g, new Point(startX, startY), new Point(endX, endY));
    }
}
