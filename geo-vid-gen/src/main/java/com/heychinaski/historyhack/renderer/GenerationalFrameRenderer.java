package com.heychinaski.historyhack.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.heychinaski.historyhack.displayobjects.StatefulBlob;
import com.heychinaski.historyhack.displayobjects.StatefulBlobFactory;
import com.heychinaski.historyhack.model.GeoEventPage;

public class GenerationalFrameRenderer implements FrameRenderer<List<GeoEventPage>> {
    private static final int RESOLUTION_FACTOR = 1000;

    public static final int LONGITUDE_DEGREES = 360;
    public static final int LATITUDE_DEGREES = 180;
    
    public static final int LONGITUDE_DEGREES_MULTIPLIED = LONGITUDE_DEGREES * RESOLUTION_FACTOR;
    public static final int LATITUDE_DEGREES_MULTIPLIED = LATITUDE_DEGREES * RESOLUTION_FACTOR;
    
    BufferedImage imageWithRetireesOnly;
    Graphics2D retireesImageG2d;
    private List<StatefulBlob> blobsToRetire;
    private List<StatefulBlob> blogsToDraw;
    
    private BufferedImage currentFrame;

    private int width;

    private int height;

    private StatefulBlobFactory blobFactory;
    
    /**
     * 
     * @param yearEvents ALL year events
     */
    public GenerationalFrameRenderer(int width, int height, Color backgroundColor, StatefulBlobFactory blobFactory) {
        this.width = width;
        this.height = height;
        this.blobFactory = blobFactory;
        blogsToDraw = new LinkedList<StatefulBlob>();
        blobsToRetire = Collections.emptyList();
        
        // initialize images
        imageWithRetireesOnly = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        retireesImageG2d = imageWithRetireesOnly.createGraphics();
        retireesImageG2d.setColor(backgroundColor);
        retireesImageG2d.fillRect(0, 0, width, height);
        
        retireesImageG2d.scale((float)width / (float)LONGITUDE_DEGREES_MULTIPLIED, (float)height / (float)LATITUDE_DEGREES_MULTIPLIED);
        retireesImageG2d.translate((LONGITUDE_DEGREES_MULTIPLIED) / 2, (LATITUDE_DEGREES_MULTIPLIED) / 2);
    }
    
    public BufferedImage getCurrentFrame() {
        return currentFrame;
    }


    
    public void renderNextFrame(List<GeoEventPage> incomingEvents) {
        // draw only the retirees onto the stored image
        for (StatefulBlob blob : blobsToRetire) {
            blob.draw(retireesImageG2d);
        }
        blobsToRetire = Collections.emptyList();
        
        // copy this before drawing others
        currentFrame = deepCopy(imageWithRetireesOnly);
        Graphics2D currentg2d = currentFrame.createGraphics();
        currentg2d.scale((float)width / (float)LONGITUDE_DEGREES_MULTIPLIED, (float)height / (float)LATITUDE_DEGREES_MULTIPLIED);
        currentg2d.translate((LONGITUDE_DEGREES_MULTIPLIED) / 2, (LATITUDE_DEGREES_MULTIPLIED) / 2);
        
        for (GeoEventPage page : incomingEvents) {
            Point point = new Point((int)Math.round(page.getLongitude() * RESOLUTION_FACTOR), 
                    -(int)Math.round(page.getLatitude() * RESOLUTION_FACTOR));
            blogsToDraw.add(blobFactory.createStatefulBlob(page, point));
        }
        
        // draw the rest on top of the retired blobs. Any blobs that will
        // draw their last state next time are ready to retire.
        Iterator<StatefulBlob> it = blogsToDraw.iterator();
        List<StatefulBlob> willRetireNextFrame = new ArrayList<StatefulBlob>();
        while (it.hasNext()) {
            StatefulBlob blob = it.next();
            blob.draw(currentg2d);
            if(blob.willDrawFinalState()) {
                it.remove();
                willRetireNextFrame.add(blob);
            }
        }
        blobsToRetire = willRetireNextFrame;
        currentg2d.dispose();
    }

    private BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        BufferedImage cloned = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
        return cloned;
    }

}
