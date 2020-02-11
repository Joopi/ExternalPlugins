package net.runelite.client.plugins.modelviewer;

import net.runelite.api.model.Triangle2D;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.*;

public class ModelViewerOverlay extends Overlay {

    private final ModelViewerPlugin plugin;

    @Inject
    ModelViewerOverlay(final ModelViewerPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Triangle2D[] projected = plugin.getDrawnTriangles();
        if (projected != null)
        {
            graphics.setColor(Color.RED);
            for (Triangle2D tri : projected)
            {
                graphics.drawLine(tri.getA().getX(),tri.getA().getY(),tri.getB().getX(),tri.getB().getY());
                graphics.drawLine(tri.getB().getX(),tri.getB().getY(),tri.getC().getX(),tri.getC().getY());
                graphics.drawLine(tri.getC().getX(),tri.getC().getY(),tri.getA().getX(),tri.getA().getY());
            }
        }
        return null;
    }
}
