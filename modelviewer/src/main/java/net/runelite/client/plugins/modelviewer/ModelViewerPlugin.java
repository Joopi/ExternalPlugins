package net.runelite.client.plugins.modelviewer;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.model.Triangle2D;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.GameObjectUtil;
import net.runelite.client.util.ModelUtil;

import javax.inject.Inject;


@PluginDescriptor(
        name = "Model viewer",
        description = "Displays the triangles of a GameObject at a WorldPoint based on input indexes",
        tags = {"model", "triangle", "view"},
        type = PluginType.UTILITY
)
public class ModelViewerPlugin extends Plugin
{

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ModelViewerConfig config;

    @Inject
    private Client client;

    @Inject
    private ModelViewerOverlay overlay;

    @Provides
    ModelViewerConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ModelViewerConfig.class);
    }

    int startIndex = 0;
    int endIndex = 0;
    WorldPoint point = null;
    GameObject object = null;
    Triangle2D[] lastProjection = null;
    boolean redraw = true;

    int x = 0;
    int y = 0;
    int z = 0;

    /**
     * called whenever the configuration panel changes
     */
    private void updateConfig()
    {
        startIndex = config.fromTriangle();
        endIndex = config.toTriangle();
        String[] axis = config.objectPosition().split(",");
        if (axis.length == 2)
        {
            point = new WorldPoint(
                    Integer.parseInt(axis[0].replaceAll("[^0-9]", "")),
                    Integer.parseInt(axis[1].replaceAll("[^0-9]", "")),
                    client.getPlane()
            );

            // GameObject reference remains unchanged until a new chunk is loaded,
            // whereas model attribute is constantly shuffled.
            // therefore we cache the GameObject and instead call object.getModel() on each frame.
            object = GameObjectUtil.getObjectAt(point, client);
            redraw = true; //whenever the coordinate or triangle indexes change, we should redraw the object.
        }
    }

    /**
     * retrieves a projection of the sought after triangles for the model.
     * returns null if nothing has changed since previous draw or the
     */
    public Triangle2D[] getDrawnTriangles()
    {
        int len = (endIndex - startIndex);
        if (object == null || len <= 0)
            return null;

        final int
                newX = client.getCameraX(),
                newY = client.getCameraY(),
                newZ = client.getCameraZ();

        if (!redraw && newX == x && newY == y && newZ == z) //Return cached projected triangles in case nothing has changed.
            return lastProjection;

        x = newX;
        y = newY;
        z = newZ;

        Model model = object.getModel();
        if (model.getTrianglesCount() <= endIndex)
            return null;

        LocalPoint localPoint = object.getLocalLocation(); // Local location (relative to our position)
        int tileHeight = Perspective.getTileHeight(client, localPoint, client.getPlane()); //object's local height
        lastProjection = new Triangle2D[len]; // len == amount of triangles to be projected

        int count = 0;
        for (int i = startIndex; i < endIndex; i++)
        {
            lastProjection[count] = Perspective.triangleToCanvas(client, ModelUtil.getTriangle(model, i), localPoint, tileHeight, 0);
            count++;
        }

        redraw = false;
        return lastProjection;
    }

    @Override
    protected void startUp()
    {
        updateConfig();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event)
    {
        updateConfig();
    }
}
