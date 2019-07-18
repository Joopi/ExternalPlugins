/*
 * Copyright (c) 2019, ganom <https://github.com/Ganom>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.onetick;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import static net.runelite.api.Constants.REGION_SIZE;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ObjectDefinition;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.VarClientStr;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.flexo.Flexo;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.onetick.utils.ExtUtils;
import net.runelite.client.plugins.onetick.utils.Method;
import net.runelite.client.plugins.onetick.utils.ObjectPoint;
import net.runelite.client.plugins.onetick.utils.Tab;
import net.runelite.client.plugins.onetick.utils.TabUtils;
import net.runelite.client.plugins.stretchedmode.StretchedModeConfig;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.HotkeyListener;


@PluginDescriptor(
	name = "One Tick",
	description = "Flexo Sample Plugin",
	tags = {"flexo", "one", "tick", "bot"},
	type = PluginType.EXTERNAL
)
@Slf4j
public class OneTick extends Plugin
{
	private static final String MARK = ColorUtil.prependColorTag("Mark One-Tick", Color.GREEN);
	private static final String UNMARK = ColorUtil.prependColorTag("Unmark One-Tick", Color.RED);

	private static final int[] KARAMBWAN = {3142};
	private static final int[] FEATHERS = {314};
	@Getter
	private final List<TileObject> objects = new ArrayList<>();
	private final Map<Integer, Set<ObjectPoint>> points = new HashMap<>();
	@Inject
	private Client client;
	@Inject
	private OneTickConfig config;
	@Inject
	private KeyManager keyManager;
	@Inject
	private ConfigManager configManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private OneTickOverlay oneTickOverlay;
	@Inject
	private ClientThread clientThread;
	@Setter
	@Getter
	private TileObject target;
	@Setter
	@Getter
	private WidgetItem karam;
	private Flexo flexo;
	private boolean oneTick;
	private boolean activated;
	private List<WidgetItem> karambwans;
	private BlockingQueue queue = new ArrayBlockingQueue(1);
	private ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, 1, 25, TimeUnit.SECONDS, queue,
		new ThreadPoolExecutor.DiscardPolicy());
	private final HotkeyListener oneTickHotkey = new HotkeyListener(() -> config.oneTick())
	{
		@Override
		public void hotkeyPressed()
		{
			oneTick = !oneTick;
			if (config.method() == Method.MAKE_BOLTS)
			{
				WidgetItem bolt = ExtUtils.getItems(ExtUtils.stringToIntArray(config.boltId()), client).iterator().next();
				WidgetItem feather = ExtUtils.getItems(FEATHERS, client).iterator().next();

				executorService.submit(() ->
				{
					while (oneTick)
					{
						if (!ExtUtils.getItems(ExtUtils.stringToIntArray(config.boltId()), client).iterator().hasNext()
							|| !ExtUtils.getItems(FEATHERS, client).iterator().hasNext())
						{
							oneTick = false;
						}
						handleSwitch(bolt.getCanvasBounds(), true);
						handleSwitch(feather.getCanvasBounds(), true);
						if (config.boltDelay() > 10)
						{
							flexo.delay(config.boltDelay());
						}
					}
				});
			}
			else if (config.method().equals(Method.KARAMBWAN))
			{
				karambwans = ExtUtils.getItems(KARAMBWAN, client);
			}
		}
	};

	@Provides
	OneTickConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OneTickConfig.class);
	}

	@Override
	protected void startUp()
	{
		keyManager.registerKeyListener(oneTickHotkey);
		overlayManager.add(oneTickOverlay);
		Flexo.client = client;
		executorService.submit(() ->
		{
			flexo = null;
			try
			{
				flexo = new Flexo();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(oneTickOverlay);
		keyManager.unregisterKeyListener(oneTickHotkey);
		flexo = null;
		setTarget(null);
		setKaram(null);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (!event.getGameState().equals(GameState.LOGGED_IN))
		{
			setTarget(null);
			setKaram(null);
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		final GameObject eventObject = event.getGameObject();
		checkObjectPoints(eventObject);
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		objects.remove(event.getGameObject());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{

		if (oneTick)
		{
			switch (config.method())
			{
				case PRAYER:
					Iterator<WidgetItem> bone = ExtUtils.getItems(ExtUtils.stringToIntArray(config.boneId()), client).iterator();

					if (!bone.hasNext())
					{
						oneTick = false;
						return;
					}

					WidgetItem next = bone.next();
					assignObject();

					executorService.submit(() ->
					{
						final String typedText = client.getVar(VarClientStr.CHATBOX_TYPED_TEXT);

						if (!Strings.isNullOrEmpty(typedText))
						{
							return;
						}

						if (getTarget() == null)
						{
							return;
						}

						handleSwitch(next.getCanvasBounds(), true);
						handleSwitch(target.getCanvasTilePoly().getBounds(), true);
					});
					break;
				case ENCHANT_BOLTS:
					Widget bolts = client.getWidget(WidgetInfo.SPELL_ENCHANT_CROSSBOW_BOLT);

					if (bolts == null)
					{
						return;
					}

					executorService.submit(() ->
					{
						if (bolts.isHidden())
						{
							flexo.keyPress(TabUtils.getTabHotkey(Tab.MAGIC, client));
						}

						flexo.keyPress(32);
						handleSwitch(bolts.getBounds(), true);
						flexo.keyPress(32);
					});
					break;
				case KARAMBWAN:
					Widget inventory = client.getWidget(WidgetInfo.INVENTORY);

					if (inventory == null)
					{
						return;
					}

					Iterator<WidgetItem> itr = karambwans.iterator();

					if (!itr.hasNext())
					{
						oneTick = false;
						executorService.submit(() -> flexo.keyPress(50));
						return;
					}

					karam = itr.next();
					itr.remove();
					assignObject();

					if (target.getClickbox() == null)
					{
						return;
					}

					executorService.submit(() ->
					{
						if (inventory.isHidden())
						{
							flexo.keyPress(TabUtils.getTabHotkey(Tab.INVENTORY, client));
						}

						flexo.keyPress(50);
						handleSwitch(karam.getCanvasBounds(), true);
						handleSwitch(target.getClickbox().getBounds(), true);
						flexo.keyPress(50);
						handleSwitch(itr.next().getCanvasBounds(), false);
					});
/*				case KARAMBWAN:
					Widget inventory = client.getWidget(WidgetInfo.INVENTORY);

					if (inventory == null)
					{
						return;
					}

					karambwans = ExtUtils.getItems(KARAMBWAN, client);

					if (karambwans.isEmpty())
					{
						oneTick = false;
						executorService.submit(() -> flexo.keyPress(50));
						return;
					}

					assignObject();
					assignKaram();

					if (target.getClickbox() == null)
					{
						return;
					}

					executorService.submit(() ->
					{
						if (inventory.isHidden())
						{
							flexo.keyPress(TabUtils.getTabHotkey(Tab.INVENTORY, client));
						}

						flexo.keyPress(50);
						handleSwitch(getKaram().getCanvasBounds());
						handleSwitch(target.getCanvasTilePoly().getBounds());
						karambwans.removeIf(item -> item.equals(karam));
						flexo.keyPress(50);
					});*/
					break;
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getType() != MenuAction.EXAMINE_OBJECT.getId())
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();
		menuEntries = Arrays.copyOf(menuEntries, menuEntries.length + 1);
		MenuEntry menuEntry = menuEntries[menuEntries.length - 1] = new MenuEntry();
		menuEntry.setOption(grabOjbect(event.getActionParam0(), event.getActionParam1(), event.getIdentifier()));
		menuEntry.setTarget(event.getTarget());
		menuEntry.setParam0(event.getActionParam0());
		menuEntry.setParam1(event.getActionParam1());
		menuEntry.setIdentifier(event.getIdentifier());
		menuEntry.setType(MenuAction.RUNELITE.getId());
		client.setMenuEntries(menuEntries);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() != MenuAction.RUNELITE || (!event.getOption().equals(MARK) && !event.getOption().equals(UNMARK)))
		{
			return;
		}

		Scene scene = client.getScene();
		Tile[][][] tiles = scene.getTiles();
		final int x = event.getActionParam0();
		final int y = event.getActionParam1();
		final int z = client.getPlane();
		final Tile tile = tiles[z][x][y];

		TileObject object = findTileObject(tile, event.getIdentifier());

		if (object == null)
		{
			return;
		}

		ObjectDefinition objectDefinition = client.getObjectDefinition(object.getId());
		String name = objectDefinition.getName();
		if (Strings.isNullOrEmpty(name))
		{
			return;
		}

		setObject(name, object);
	}

	private void assignObject()
	{
		objects.forEach(object ->
		{
			List<Integer> tmp = new ArrayList<>();
			final int distance = object.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldArea());
			tmp.add(distance);
			int lowest = Collections.min(tmp);

			if (distance == lowest)
			{
				setTarget(object);
			}
		});
	}

	private String grabOjbect(int x, int y, int id)
	{
		Scene scene = client.getScene();
		Tile[][][] tiles = scene.getTiles();
		final int z = client.getPlane();
		final Tile tile = tiles[z][x][y];
		final TileObject object = findTileObject(tile, id);
		if (object != null)
		{
			final ObjectDefinition objectDefinition = client.getObjectDefinition(object.getId());
			final String name = objectDefinition.getName();

			if (!Strings.isNullOrEmpty(name))
			{
				final WorldPoint loc = WorldPoint.fromLocalInstance(client, tile.getLocalLocation());
				final int regionId = loc.getRegionID();

				final ObjectPoint point = new ObjectPoint(
					name,
					regionId,
					loc.getX() & (REGION_SIZE - 1),
					loc.getY() & (REGION_SIZE - 1),
					client.getPlane());

				final Set<ObjectPoint> objectPoints = points.get(regionId);

				if (objectPoints != null && objectPoints.contains(point))
				{
					return UNMARK;
				}
			}
		}
		return MARK;
	}

	private void checkObjectPoints(TileObject object)
	{
		final WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, object.getLocalLocation());
		final Set<ObjectPoint> objectPoints = points.get(worldPoint.getRegionID());

		if (objectPoints == null)
		{
			return;
		}

		for (ObjectPoint objectPoint : objectPoints)
		{
			if ((worldPoint.getX() & (REGION_SIZE - 1)) == objectPoint.getRegionX()
				&& (worldPoint.getY() & (REGION_SIZE - 1)) == objectPoint.getRegionY()
				&& objectPoint.getName().equals(client.getObjectDefinition(object.getId()).getName()))
			{
				objects.add(object);
				break;
			}
		}
	}

	private TileObject findTileObject(Tile tile, int id)
	{
		if (tile == null)
		{
			return null;
		}

		final GameObject[] tileGameObjects = tile.getGameObjects();
		final DecorativeObject tileDecorativeObject = tile.getDecorativeObject();

		if (tileDecorativeObject != null && tileDecorativeObject.getId() == id)
		{
			return tileDecorativeObject;
		}

		for (GameObject object : tileGameObjects)
		{
			if (object == null)
			{
				continue;
			}

			if (object.getId() == id)
			{
				return object;
			}

			// Check impostors
			final ObjectDefinition comp = client.getObjectDefinition(object.getId());

			if (comp.getImpostorIds() != null)
			{
				for (int impostorId : comp.getImpostorIds())
				{
					if (impostorId == id)
					{
						return object;
					}
				}
			}
		}

		return null;
	}

	private void setObject(String name, final TileObject object)
	{
		if (object == null)
		{
			return;
		}

		final WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, object.getLocalLocation());
		final int regionId = worldPoint.getRegionID();
		final ObjectPoint point = new ObjectPoint(name, regionId, worldPoint.getX() & (REGION_SIZE - 1), worldPoint.getY() & (REGION_SIZE - 1), client.getPlane());

		Set<ObjectPoint> objectPoints = points.computeIfAbsent(regionId, k -> new HashSet<>());

		if (objectPoints.contains(point))
		{
			objectPoints.remove(point);
			objects.remove(object);
		}
		else
		{
			objectPoints.add(point);
			objects.add(object);
		}
	}

	private void handleSwitch(Rectangle rectangle, boolean click)
	{
		ExtUtils.handleSwitch(rectangle, config.actionType(), flexo, client, configManager.getConfig(StretchedModeConfig.class).scalingFactor(), (int) getMillis(), click);
	}

	private long getMillis()
	{
		return (long) (Math.random() * config.randLow() + config.randHigh());
	}
}