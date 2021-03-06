/*
 * Copyright (c) 2018 kulers
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
 *
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
package net.runelite.client.plugins.inventorytags;

import com.google.common.base.MoreObjects;
import com.google.inject.Provides;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuOpcode;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetMenuOptionClicked;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;

@Extension
@PluginDescriptor(
	name = "Nom Inventory Tags",
	enabledByDefault = false,
	description = "Add the ability to tag items in your inventory",
	tags = {"highlight", "items", "overlay", "tagging"},
	type = PluginType.UTILITY
)
public class InventoryTagsPlugin extends Plugin
{
	private static final String ITEM_KEY_PREFIX = "item_";

	private static final String SETNAME_GROUP_1 = "Group 1";
	private static final String SETNAME_GROUP_2 = "Group 2";
	private static final String SETNAME_GROUP_3 = "Group 3";
	private static final String SETNAME_GROUP_4 = "Group 4";
	private static final String SETNAME_GROUP_5 = "Group 5";
	private static final String SETNAME_GROUP_6 = "Group 6";
	private static final String SETNAME_GROUP_7 = "Group 7";
	private static final String SETNAME_GROUP_8 = "Group 8";
	private static final String SETNAME_GROUP_9 = "Group 9";
	private static final String SETNAME_GROUP_10 = "Group 10";
	private static final String SETNAME_GROUP_11 = "Group 11";
	private static final String SETNAME_GROUP_12 = "Group 12";


	private static final String CONFIGURE = "Configure";
	private static final String SAVE = "Save";
	private static final String MENU_TARGET = "Inventory Tags";
	private static final String MENU_SET = "Mark";
	private static final String MENU_REMOVE = "Remove";

	private static final WidgetMenuOption FIXED_INVENTORY_TAB_CONFIGURE = new WidgetMenuOption(CONFIGURE,
		MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption FIXED_INVENTORY_TAB_SAVE = new WidgetMenuOption(SAVE,
		MENU_TARGET, WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption RESIZABLE_INVENTORY_TAB_CONFIGURE = new WidgetMenuOption(CONFIGURE,
		MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption RESIZABLE_INVENTORY_TAB_SAVE = new WidgetMenuOption(SAVE,
		MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB);
	private static final WidgetMenuOption RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_CONFIGURE = new WidgetMenuOption(CONFIGURE,
		MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);
	private static final WidgetMenuOption RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_SAVE = new WidgetMenuOption(SAVE,
		MENU_TARGET, WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB);

	private static final List<String> GROUPS = List.of(SETNAME_GROUP_12, SETNAME_GROUP_11, SETNAME_GROUP_10, SETNAME_GROUP_9,
		SETNAME_GROUP_8, SETNAME_GROUP_7, SETNAME_GROUP_6, SETNAME_GROUP_5, SETNAME_GROUP_4, SETNAME_GROUP_3, SETNAME_GROUP_2, SETNAME_GROUP_1);

	@Inject
	private ConfigManager configManager;

	@Inject
	private InventoryTagsConfig config;

	@Inject
	private MenuManager menuManager;

	@Inject
	private InventoryTagsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	private boolean editorMode;

	@Provides
    InventoryTagsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InventoryTagsConfig.class);
	}

	String getTag(int itemId)
	{
		String tag = configManager.getConfiguration(InventoryTagsConfig.GROUP, ITEM_KEY_PREFIX + itemId);
		if (tag == null || tag.isEmpty())
		{
			return null;
		}

		return tag;
	}

	private void setTag(int itemId, String tag)
	{
		configManager.setConfiguration(InventoryTagsConfig.GROUP, ITEM_KEY_PREFIX + itemId, tag);
	}

	private void unsetTag(int itemId)
	{
		configManager.unsetConfiguration(InventoryTagsConfig.GROUP, ITEM_KEY_PREFIX + itemId);
	}

	@Override
	protected void startUp()
	{
		refreshInventoryMenuOptions();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		removeInventoryMenuOptions();
		overlayManager.remove(overlay);
		editorMode = false;
	}

	@Subscribe
	private void onWidgetMenuOptionClicked(final WidgetMenuOptionClicked event)
	{
		if (event.getWidget() == WidgetInfo.FIXED_VIEWPORT_INVENTORY_TAB
			|| event.getWidget() == WidgetInfo.RESIZABLE_VIEWPORT_INVENTORY_TAB
			|| event.getWidget() == WidgetInfo.RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_TAB)
		{
			editorMode = event.getMenuOption().equals(CONFIGURE) && Text.removeTags(event.getMenuTarget()).equals(MENU_TARGET);
			refreshInventoryMenuOptions();
		}
	}

	@Subscribe
	private void onMenuOptionClicked(final MenuOptionClicked event)
	{
		if (event.getMenuOpcode() != MenuOpcode.RUNELITE)
		{
			return;
		}

		final String selectedMenu = Text.removeTags(event.getTarget());

		if (event.getOption().equals(MENU_SET))
		{
			setTag(event.getIdentifier(), selectedMenu);
		}
		else if (event.getOption().equals(MENU_REMOVE))
		{
			unsetTag(event.getIdentifier());
		}
	}

	@Subscribe
	private void onMenuOpened(final MenuOpened event)
	{
		final MenuEntry firstEntry = event.getFirstEntry();

		if (firstEntry == null)
		{
			return;
		}

		final int widgetId = firstEntry.getParam1();

		// Inventory item menu
		if (widgetId == WidgetInfo.INVENTORY.getId() && editorMode)
		{
			int itemId = firstEntry.getIdentifier();

			if (itemId == -1)
			{
				return;
			}

			MenuEntry[] menuList = new MenuEntry[config.getAmount().toInt() + 1];
			int num = 0;

			// preserve the 'Cancel' option as the client will reuse the first entry for Cancel and only resets option/action
			menuList[num++] = event.getMenuEntries()[0];

			List<String> groups = GROUPS.subList(Math.max(GROUPS.size() - config.getAmount().toInt(), 0), GROUPS.size());

			for (final String groupName : groups)
			{
				final String group = getTag(itemId);
				final MenuEntry newMenu = new MenuEntry();
				final Color color = getGroupNameColor(groupName);
				newMenu.setOption(groupName.equals(group) ? MENU_REMOVE : MENU_SET);
				newMenu.setTarget(ColorUtil.prependColorTag(groupName, MoreObjects.firstNonNull(color, Color.WHITE)));
				newMenu.setIdentifier(itemId);
				newMenu.setParam1(widgetId);
				newMenu.setOpcode(MenuOpcode.RUNELITE.getId());
				menuList[num++] = newMenu;
			}

			// Need to set the event entries to prevent conflicts
			event.setMenuEntries(menuList);
			event.setModified();
		}
	}

	Color getGroupNameColor(final String name)
	{
		switch (name)
		{
			case SETNAME_GROUP_1:
				return config.getGroup1Color();
			case SETNAME_GROUP_2:
				return config.getGroup2Color();
			case SETNAME_GROUP_3:
				return config.getGroup3Color();
			case SETNAME_GROUP_4:
				return config.getGroup4Color();
			case SETNAME_GROUP_5:
				return config.getGroup5Color();
			case SETNAME_GROUP_6:
				return config.getGroup6Color();
			case SETNAME_GROUP_7:
				return config.getGroup7Color();
			case SETNAME_GROUP_8:
				return config.getGroup8Color();
			case SETNAME_GROUP_9:
				return config.getGroup9Color();
			case SETNAME_GROUP_10:
				return config.getGroup10Color();
			case SETNAME_GROUP_11:
				return config.getGroup11Color();
			case SETNAME_GROUP_12:
				return config.getGroup12Color();
		}
		return null;
	}

	private void removeInventoryMenuOptions()
	{
		menuManager.removeManagedCustomMenu(FIXED_INVENTORY_TAB_CONFIGURE);
		menuManager.removeManagedCustomMenu(FIXED_INVENTORY_TAB_SAVE);
		menuManager.removeManagedCustomMenu(RESIZABLE_INVENTORY_TAB_CONFIGURE);
		menuManager.removeManagedCustomMenu(RESIZABLE_INVENTORY_TAB_SAVE);
		menuManager.removeManagedCustomMenu(RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_CONFIGURE);
		menuManager.removeManagedCustomMenu(RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_SAVE);
	}

	private void refreshInventoryMenuOptions()
	{
		removeInventoryMenuOptions();
		if (editorMode)
		{
			menuManager.addManagedCustomMenu(FIXED_INVENTORY_TAB_SAVE);
			menuManager.addManagedCustomMenu(RESIZABLE_INVENTORY_TAB_SAVE);
			menuManager.addManagedCustomMenu(RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_SAVE);
		}
		else
		{
			menuManager.addManagedCustomMenu(FIXED_INVENTORY_TAB_CONFIGURE);
			menuManager.addManagedCustomMenu(RESIZABLE_INVENTORY_TAB_CONFIGURE);
			menuManager.addManagedCustomMenu(RESIZABLE_BOTTOM_LINE_INVENTORY_TAB_CONFIGURE);
		}
	}
}