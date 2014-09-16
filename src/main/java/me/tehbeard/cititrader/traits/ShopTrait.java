package me.tehbeard.cititrader.traits;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.tehbeard.cititrader.CitiTrader;
import me.tehbeard.cititrader.Trader;
import me.tehbeard.cititrader.TraderInterface;
import me.tehbeard.cititrader.TraderStatus;
import me.tehbeard.cititrader.WalletTrait;
import me.tehbeard.cititrader.TraderStatus.Status;
import me.tehbeard.cititrader.WalletTrait.WalletType;
import me.tehbeard.cititrader.utils.TraderUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.ItemStorage;
import net.citizensnpcs.api.util.Messaging;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

public class ShopTrait extends Trait implements TraderInterface {

	private Map<ItemStack, Double> sellPrices;
	private Map<ItemStack, Double> buyPrices;
	private Map<ItemStack, Integer> stackSizes;
	private Map<ItemStack, Integer> buyStackSizes;
	private boolean disabled;
	private int linkedNPCID;
	private boolean isStatic;

	public ShopTrait() {
		super("shop");
		sellPrices = new HashMap<ItemStack, Double>();
		buyPrices = new HashMap<ItemStack, Double>();
		stackSizes = new HashMap<ItemStack, Integer>();
		buyStackSizes = new HashMap<ItemStack, Integer>();
		disabled = false;
		linkedNPCID = -1;
		isStatic = false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void load(DataKey data) throws NPCLoadException {
		// load selling prices
		for (DataKey priceKey : data.getRelative("prices").getIntegerSubKeys()) {
			ItemStack k = ItemStorage.loadItemStack(priceKey.getRelative("item"));
			
			try{
				// Add repair Cost Meta Data
				if (priceKey.keyExists("repairCost")) {
					int repairCost = priceKey.getInt("repairCost");
					Repairable repariCostMeta = (Repairable) k.getItemMeta();
					repariCostMeta.setRepairCost(repairCost);
					k.setItemMeta((ItemMeta) repariCostMeta);
				}
				
				if(priceKey.keyExists("eSM")){
					EnchantmentStorageMeta esm = (EnchantmentStorageMeta) k.getItemMeta(); 
					for(DataKey eSMIds : priceKey.getRelative("eSM").getSubKeys()){
						Enchantment enchant = Enchantment.getById(Integer.parseInt(eSMIds.name()));
						esm.addStoredEnchant(enchant, eSMIds.getInt("level"), true);
					}
					k.setItemMeta((ItemMeta)esm);
				}
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
				System.out.println(e.getCause());
			}

			if (CitiTrader.self.getConfig().getBoolean("debug.debugText", false)) {
				System.out.println(k.toString());
			}
			double price = priceKey.getDouble("price");
			int stacksize = priceKey.getInt("stack", 1);
			sellPrices.put(k, price);
			stackSizes.put(k, stacksize);
		}
		// load buy prices
		for (DataKey priceKey : data.getRelative("buyprices").getIntegerSubKeys()) {
			ItemStack k = ItemStorage.loadItemStack(priceKey.getRelative("item"));
			// Assume once that if there is an item, that it is real.
			if (k == null) {
				int test = priceKey.getInt("item.id");
				Material mat = Material.getMaterial(test);
				priceKey.setString("item.id", mat.name());
				k = ItemStorage.loadItemStack((priceKey.getRelative("item")));
			}	
			
			try{
				// Add repair Cost Meta Data
				if (priceKey.keyExists("repairCost")) {
					int repairCost = priceKey.getInt("repairCost");
					Repairable repariCostMeta = (Repairable) k.getItemMeta();
					repariCostMeta.setRepairCost(repairCost);
					k.setItemMeta((ItemMeta) repariCostMeta);
				}
				
				if(priceKey.keyExists("eSM")){
					EnchantmentStorageMeta esm = (EnchantmentStorageMeta) k.getItemMeta(); 
					for(DataKey eSMIds : priceKey.getRelative("eSM").getSubKeys()){
						Enchantment enchant = Enchantment.getById(Integer.parseInt(eSMIds.name()));
						esm.addStoredEnchant(enchant, eSMIds.getInt("level"), true);
					}
					k.setItemMeta((ItemMeta)esm);
				}
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
				System.out.println(e.getCause());
			}
			
			if (CitiTrader.self.getConfig().getBoolean("debug.debugText", false)) {
				System.out.println(k);
			}
			double price = priceKey.getDouble("price");
			int stacksize = priceKey.getInt("stack", 1);
			// System.out.println(price);
			buyStackSizes.put(k, stacksize);
			buyPrices.put(k, price);
		}

		// load if disabled or enabled
		disabled = data.getBoolean("disabled");

		// load if Trader is linked to another NPC
		linkedNPCID = data.getInt("linkedNPCID", -1);

		// Load if Stock is static (or infinite)
		isStatic = data.getBoolean("static");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void save(DataKey data) {

		data.setBoolean("disabled", disabled);
		data.setInt("linkedNPCID", linkedNPCID);

		data.removeKey("prices");
		DataKey sellPriceIndex = data.getRelative("prices");

		int i = 0;
		for (Entry<ItemStack, Double> price : sellPrices.entrySet()) {
			if (CitiTrader.self.getConfig().getBoolean("debug.debugText", false)) {
				System.out.println("Saving: " + price.getKey().getType().toString());
				System.out.println(price.getKey().toString());
			}
			try {
				if (price.getValue() > 0.0D) {
					ItemStorage.saveItem(sellPriceIndex.getRelative("" + i).getRelative("item"), price.getKey());
					if (stackSizes.containsKey(price.getKey())) {
						sellPriceIndex.getRelative("" + i).setInt("stack", stackSizes.get(price.getKey()));
					} else {
						sellPriceIndex.getRelative("" + i).setInt("stack", 1);
					}

					// Save repair Cost Meta Data
					if (price.getKey().getItemMeta() instanceof Repairable) {
						Repairable repariCostMeta = (Repairable) price.getKey().getItemMeta();
						sellPriceIndex.getRelative("" + i).setInt("repairCost", repariCostMeta.getRepairCost());
					}
					// Save enchanted Books
					if (price.getKey().getItemMeta() instanceof EnchantmentStorageMeta) {
						EnchantmentStorageMeta enchMeta = (EnchantmentStorageMeta) price.getKey().getItemMeta();
						if(enchMeta.hasStoredEnchants()){
							for(Entry<Enchantment, Integer> enchantMent : enchMeta.getStoredEnchants().entrySet()){
								sellPriceIndex.getRelative("" + i).getRelative("eSM").getRelative("" + enchantMent.getKey().getId()).setInt("level", enchantMent.getValue());
							}
						}
					}
					sellPriceIndex.getRelative("" + i).setDouble("price", price.getValue());
				}
			} catch (Exception e) {
				CitiTrader.self.getLogger().log(Level.WARNING, "Failed to save item: " + price.getKey().toString());
				if (CitiTrader.self.getConfig().getBoolean("debug.debugText", false)) {
					e.printStackTrace();
				}
			} finally {
				i++;
			}

		}

		data.removeKey("buyprices");
		DataKey buyPriceIndex = data.getRelative("buyprices");
		i = 0;
		for (Entry<ItemStack, Double> price : buyPrices.entrySet()) {
			if (price.getValue() > 0.0D) {
				
				ItemStorage.saveItem(buyPriceIndex.getRelative("" + i).getRelative("item"), price.getKey());
				if (buyStackSizes.containsKey(price.getKey())) {
					sellPriceIndex.getRelative("" + i).setInt("stack", buyStackSizes.get(price.getKey()));
				} else {
					sellPriceIndex.getRelative("" + i).setInt("stack", 1);
				}

				// Save repair Cost Meta Data
				if (price.getKey().getItemMeta() instanceof Repairable) {
					Repairable repariCostMeta = (Repairable) price.getKey().getItemMeta();
					sellPriceIndex.getRelative("" + i).setInt("repairCost", repariCostMeta.getRepairCost());
				}

				// Save enchanted Books
				if (price.getKey().getItemMeta() instanceof EnchantmentStorageMeta) {
					EnchantmentStorageMeta enchMeta = (EnchantmentStorageMeta) price.getKey().getItemMeta();
					if(enchMeta.hasStoredEnchants()){
						for(Entry<Enchantment, Integer> enchantMent : enchMeta.getStoredEnchants().entrySet()){
							sellPriceIndex.getRelative("" + i).getRelative("eSM").getRelative("" + enchantMent.getKey().getId()).setInt("level", enchantMent.getValue());
						}
					}
				}
				
				// Save enchanted Books
				if (price.getKey().getItemMeta() instanceof EnchantmentStorageMeta) {
					EnchantmentStorageMeta enchMeta = (EnchantmentStorageMeta) price.getKey().getItemMeta();
					if(enchMeta.hasStoredEnchants()){
						for(Entry<Enchantment, Integer> enchantMent : enchMeta.getStoredEnchants().entrySet()){
							sellPriceIndex.getRelative("" + i).getRelative("eSM").getRelative("" + enchantMent.getKey().getId()).setInt("level", enchantMent.getValue());
						}
					}
				}
				buyPriceIndex.getRelative("" + i++).setDouble("price", price.getValue());
			}
		}
		data.setBoolean("static", isStatic);
	}

	/**
	 * Construct a viewing inventory
	 * 
	 * @return
	 */
	private Inventory constructViewing() {

		Inventory display = Bukkit.createInventory(null, 54, "Left Click Buy-Right Click Price");

		buildSalesWindow(display);

		return display;
	}

	private Inventory constructSellBox() {

		Inventory display = Bukkit.createInventory(null, 36, "Selling");
		return display;

	}

	/**
	 * Does this stockroom contain this item
	 * 
	 * @param locate
	 *            Item to look for
	 * @param checkAmount
	 * @return
	 */
	public boolean hasStock(ItemStack locate, boolean checkAmount) {

		if (npc.hasTrait(StockRoomTrait.class)) {
			return npc.getTrait(StockRoomTrait.class).hasStock(locate, checkAmount);
		}

		if (npc.hasTrait(LinkedChestTrait.class)) {
			return npc.getTrait(LinkedChestTrait.class).hasStock(locate);
		}

		return false;
	}

	public boolean setLinkedNPC(String name) {
		Iterator<NPC> it = CitizensAPI.getNPCRegistry().iterator();
		while (it.hasNext()) {
			NPC linkedNPC = it.next();
			if (linkedNPC.getName().equals(name)) {
				if (linkedNPC.hasTrait(ShopTrait.class)) {
					// Check to see trying to link to NPC that is already linked
					// to this NPC.
					if (linkedNPC.getTrait(ShopTrait.class).getLinkedNPC() != null) {
						if (linkedNPC.getTrait(ShopTrait.class).getLinkedNPC().getId() == npc.getId()) {
							return false;
						}
					}

					linkedNPCID = linkedNPC.getId();
					return true;
				}
			}
		}

		return false;
	}

	public boolean removeLinkedNPC() {
		linkedNPCID = -1;
		return true;
	}

	public boolean isLinkedNPC() {
		if (linkedNPCID > -1) {
			return true;
		}

		return false;
	}

	public NPC getLinkedNPC() {
		// Can't get NPC with id -1
		if (linkedNPCID == -1) {
			return null;
		}

		NPC linkedNPC = CitizensAPI.getNPCRegistry().getById(linkedNPCID);
		if (linkedNPC != null) {
			if (linkedNPC.hasTrait(ShopTrait.class)) {
				return linkedNPC;
			}
		}
		return null;
	}

	public void setDisabled(boolean value) {
		disabled = value;
	}

	public boolean getDisabled() {
		return disabled;
	}

	public double getSellPrice(ItemStack is) {
		ItemStack i = is.clone();
		i.setAmount(1);
		if (isLinkedNPC()) {
			NPC linkedNPC = getLinkedNPC();
			if (linkedNPC != null) {
				return linkedNPC.getTrait(ShopTrait.class).getSellPrices().containsKey(i) ? linkedNPC
						.getTrait(ShopTrait.class).getSellPrices().get(i) : 0;
			}
		}
		double price = sellPrices.containsKey(i) ? sellPrices.get(i) : 0;
		return price;

	}

	public Map<ItemStack, Double> getSellPrices() {
		return sellPrices;
	}

	public void setSellPrice(ItemStack is, double price) {
		ItemStack i = is.clone();
		i.setAmount(1);
		if (price == -1) {
			if (sellPrices.containsKey(i)) {
				sellPrices.remove(i);
			}
			return;
		}

		sellPrices.put(i, price);

	}

	public boolean setSellStack(ItemStack is, int amount) {
		ItemStack i = is.clone();
		i.setAmount(1);
		if (amount == -1) {
			if (stackSizes.containsKey(i)) {
				stackSizes.remove(i);
			}
			return true;
		}
		stackSizes.put(i, amount);
		return true;
	}

	public int getSellStack(ItemStack is) {
		ItemStack i = is.clone();
		i.setAmount(1);

		return stackSizes.containsKey(i) ? stackSizes.get(i) : 1;
	}

	public double getBuyPrice(ItemStack is) {
		ItemStack i = is.clone();
		i.setAmount(1);
		if (isLinkedNPC()) {
			NPC linkedNPC = CitizensAPI.getNPCRegistry().getById(linkedNPCID);
			return linkedNPC.getTrait(ShopTrait.class).getBuyPrices().containsKey(i) ? linkedNPC
					.getTrait(ShopTrait.class).getBuyPrices().get(i) : 0;
		}
		return buyPrices.containsKey(i) ? buyPrices.get(i) : 0;

	}

	public Map<ItemStack, Double> getBuyPrices() {
		return buyPrices;
	}

	public void setBuyPrice(ItemStack is, double price) {
		ItemStack i = is.clone();
		i.setAmount(1);
		if (price == -1) {
			if (buyPrices.containsKey(i)) {
				buyPrices.remove(i);
			}
			return;
		}
		buyPrices.put(i, price);
	}

	public boolean isStatic() {
		return isStatic;
	}

	public void setStatic(boolean value) {
		this.isStatic = value;
	}

	public void openSalesWindow(Player player) {
		TraderStatus state = Trader.getStatus(player.getName());

		if (state.getStatus() == Status.ITEM_SELECT || state.getStatus() == Status.AMOUNT_SELECT) {
			state.setStatus(Status.ITEM_SELECT);
			buildSalesWindow(state.getInventory());
		} else {
			state.setTrader(npc);
			state.setStatus(Status.ITEM_SELECT);
			state.setInventory(constructViewing());
			player.openInventory(state.getInventory());
			// buildSalesWindow(state);
		}
	}

	public void openBuyWindow(Player player) {
		TraderStatus state = Trader.getStatus(player.getName());
		state.setTrader(npc);
		if (state.getStatus() == Status.NOT) {

			state.setStatus(Status.SELL_BOX);
			Inventory i = constructSellBox();
			state.setInventory(i);
			player.openInventory(i);

		}

	}

	public void processInventoryClick(InventoryClickEvent event) {
		TraderStatus state = Trader.getStatus(event.getWhoClicked().getName());

		// stop if not item or not in trade windows
		if (event.getCurrentItem() == null || state.getStatus() == Status.NOT) {
			return;
		}

		// cancel the event.
		if (state.getStatus() != Status.STOCKROOM && state.getStatus() != Status.SELL_BOX) {
			event.setCancelled(true);
		}

		if (event.getRawSlot() == 45 && state.getStatus() == Status.AMOUNT_SELECT) {
			openSalesWindow((Player) event.getWhoClicked());
			return;
		}
		// Return if no item is selected.
		if (event.getCurrentItem().getType().equals(Material.AIR)) {
			return;
		}

		switch (state.getStatus()) {

		// selecting item to purchase
		case ITEM_SELECT: {
			if (!TraderUtils.isTopInventory(event)) {
				break;
			}
			// if (event.isShiftClick()) {
			// event.setCancelled(true);
			// }
			if (event.isLeftClick()) {
				if (CitiTrader.self.getConfig().getBoolean("debug.debugText", false)) {
					System.out.println(event.getCurrentItem().clone());
				}
				buildSellWindow(event.getCurrentItem().clone(), state);
			} else {
				Player p = (Player) event.getWhoClicked();
				ItemStack is = event.getCurrentItem();
				if (is == null) {
					return;
				}
				double price = state.getTrader().getTrait(ShopTrait.class).getSellPrice(is);

				if (getSellStack(is) > 1) {
					p.sendMessage("Item costs (" + getSellStack(is) + "):");
				} else {
					p.sendMessage("Item costs: ");
				}
				p.sendMessage("" + price);
			}
		}
			break;

		// Amount selection window
		case AMOUNT_SELECT: {
			if (!TraderUtils.isTopInventory(event)) {
				break;
			}
			if (event.isLeftClick()) {
				Player player = (Player) event.getWhoClicked();
				sellToPlayer(player, state.getTrader(), event.getCurrentItem());
			} else {
				Player p = (Player) event.getWhoClicked();
				// double price =
				// state.getTrader().getTrait(StockRoomTrait.class).getSellPrice(event.getCurrentItem())
				// * event.getCurrentItem().getAmount();
				double price = state.getTrader().getTrait(ShopTrait.class).getSellPrice(event.getCurrentItem())
						* (event.getCurrentItem().getAmount() / getSellStack(event.getCurrentItem()));

				p.sendMessage("Stack costs:");
				p.sendMessage("" + price);
			}
		}
			break;

		case SELL_BOX: {
			if (!TraderUtils.isTopInventory(event) && !TraderUtils.isBottomInventory(event)) {
				break;
			}

			if (event.isShiftClick()) {
				event.setCancelled(true);
				return;
			}
			if (event.isRightClick()) {
				Player p = (Player) event.getWhoClicked();
				double price = state.getTrader().getTrait(ShopTrait.class).getBuyPrice(event.getCurrentItem());
				p.sendMessage(ChatColor.GOLD + "Item price: ");
				p.sendMessage(ChatColor.GOLD + "" + price);
				p.sendMessage(ChatColor.GOLD + "Stack price:");
				p.sendMessage(ChatColor.GOLD + "" + price * event.getCurrentItem().getAmount());
				event.setCancelled(true);
			}
		}
		default:
			break;

		}

	}

	@SuppressWarnings("deprecation")
	private void sellToPlayer(Player player, NPC npc, final ItemStack isold) {
		ShopTrait store = npc.getTrait(ShopTrait.class);
		TraderStatus state = Trader.getStatus(player.getName());
		ItemStack is = isold.clone();

		if (!store.hasStock(is, true)) {
			player.sendMessage(ChatColor.RED + "Not enough items to purchase");
			return;
		}

		final Inventory playerInv = player.getInventory();

		if (!TraderUtils.hasInventorySpace(playerInv, is)) {
			player.sendMessage(ChatColor.RED + "You do not have enough space to purchase that item");
		} else {
			// check econ
			WalletTrait wallet = npc.getTrait(WalletTrait.class);
			double cost = (isold.getAmount() / getSellStack(isold)) * store.getSellPrice(isold);
			String playerName = player.getName();

			// Does player have the money?
			if (!CitiTrader.economy.has(playerName, cost)) {
				Messaging.sendError(player, "You don't have enough money to purchase this item.");
				return;
			}

			CitiTrader.economy.withdrawPlayer(playerName, cost);

			// Check if npc wallet is sane
			if (!wallet.deposit(cost)) {
				Messaging.sendError(player, "Error while handling NPC wallet.");
				CitiTrader.economy.depositPlayer(playerName, cost);
				return;
			}

			// Rewrite of checking stock
			// Does the NPC have the items? if so remove them.
			if (npc.hasTrait(LinkedChestTrait.class) && !wallet.getType().equals(WalletType.ADMIN)) {
				LinkedChestTrait linkedchest = npc.getTrait(LinkedChestTrait.class);
				if (linkedchest.hasLinkedChest()) {
					linkedchest.removeItem(is);
				} else {
					Messaging.sendError(player, "Error retrieving Chests.");
					return;
					// transaction = false;
				}
			} else if (npc.hasTrait(StockRoomTrait.class) && !wallet.getType().equals(WalletType.ADMIN)) {
				if (!npc.getTrait(StockRoomTrait.class).removeItem(is)) {
					Messaging.sendError(player, "Error removing items, contact admin.");
					return;
				}
			}

			player.sendMessage(ChatColor.GOLD + isold.getType().name() + "*" + isold.getAmount());
			player.sendMessage(ChatColor.GOLD + "purchased");
			player.sendMessage(ChatColor.GOLD + "" + cost);
			
			if (CitiTrader.self.getConfig().getBoolean("Transactions-To-Log", false)) {
				CitiTrader.self.getLogger().log(Level.INFO, player.getName() + 
						" purchased " + isold.getType().name() + "*" + isold.getAmount() + 
						" for " + cost );
			}

			playerInv.addItem(isold);

			buildSellWindow(isold, state);
		}
	}

	@SuppressWarnings("deprecation")
	public void processInventoryClose(InventoryCloseEvent event) {
		TraderStatus state = Trader.getStatus(event.getPlayer().getName());

		if (state.getStatus() == Status.SELL_BOX) {
			Inventory sellbox = state.getInventory();
			Double total = 0.0D;
			for (int i = 0; i < sellbox.getSize(); i++) {
				ItemStack is = sellbox.getItem(i);

				if (is == null) {
					continue;
				}

				double price = state.getTrader().getTrait(ShopTrait.class).getBuyPrice(is);

				// check we buy it.
				if (price == 0.0D) {
					continue;
				}

				double sale = price * is.getAmount();
				WalletTrait wallet = state.getTrader().getTrait(WalletTrait.class);

				if (!wallet.has(sale)) {
					((CommandSender) event.getPlayer()).sendMessage(ChatColor.RED
							+ "Trader does not have the funds to pay you.");
					break;
				}

				// give cash
				if (!wallet.withdraw(sale)) {
					((CommandSender) event.getPlayer()).sendMessage(ChatColor.RED
							+ "Trader couldn't find their wallet.");
					break;
				}

				if (!CitiTrader.economy.depositPlayer(event.getPlayer().getName(), sale).transactionSuccess()) {
					((CommandSender) event.getPlayer()).sendMessage(ChatColor.RED + "Couldn't find your wallet.");
					wallet.deposit(sale);
					break;
				}

				// start chest loop here

				// check space

				if (wallet.getType() == WalletType.ADMIN) {
					// Don't add purchased item into inventory.
					// ((CommandSender)
					// event.getPlayer()).sendMessage(ChatColor.RED
					// +"The admins destroy your item in return for money");
				} else if (npc.hasTrait(LinkedChestTrait.class)) {
					if (npc.getTrait(LinkedChestTrait.class).hasSpace(is)) {
						npc.getTrait(LinkedChestTrait.class).addItem(is);
					} else {
						Messaging.sendError((CommandSender) event.getPlayer(),
								CitiTrader.self.getLang().getString("shop.buynospace"));
						continue;
					}
				} else {
					if (!TraderUtils.hasInventorySpace(npc.getTrait(StockRoomTrait.class).getInventory(), is)) {
						Messaging.sendError((CommandSender) event.getPlayer(),
								CitiTrader.self.getLang().getString("shop.buynospace"));
						continue;
					} else {
						npc.getTrait(StockRoomTrait.class).addItem(is);
					}
				}
				total += sale;
				// take item
				sellbox.setItem(i, null);

				if (CitiTrader.self.getConfig().getBoolean("Transactions-To-Log", false)) {
					CitiTrader.self.getLogger().log(Level.INFO, event.getPlayer().getName() + 
						" sold " + is.getType().name() + "*" + is.getAmount() + 
						" for " + sale );
				}

			}
			((Player) event.getPlayer()).sendMessage("Total money from sale to trader: " + total);
			// drop all items in sellbox inventory
			Iterator<ItemStack> it = state.getInventory().iterator();
			while (it.hasNext()) {
				ItemStack is = it.next();
				if (is != null) {
					event.getPlayer().getWorld()
							.dropItemNaturally(event.getPlayer().getLocation().add(0.5, 0.0, 0.5), is);
				}
			}

		}
		Trader.clearStatus(event.getPlayer().getName());
	}

	public void buildSellWindow(final ItemStack item, final TraderStatus state) {
		CitiTrader.self.getServer().getScheduler().runTaskLaterAsynchronously(CitiTrader.self, new Runnable() {
			public void run() {
				for (int i = 0; i < 54; i++) {
					state.getInventory().setItem(i, null);
				}

				ItemStack is = item.clone();
				ItemStack newIs = is.clone();
				int k = 0;
				int in = 1;

				newIs.setAmount(1);
				if (stackSizes.containsKey(newIs)) {
					in = stackSizes.get(newIs);
				}

				for (int i = in; i <= 64; i *= 2) {
					if (i <= is.getMaxStackSize()) {
						newIs = is.clone();
						newIs.setAmount(i);
						if (hasStock(newIs, true)) {
							state.getInventory().setItem(k, newIs);
						}
						k++;
					}
				}
				state.getInventory().setItem(45, new ItemStack(Material.ARROW, 1));
				state.setStatus(Status.AMOUNT_SELECT);
			}
		}, 2l);
	}

	public void buildSalesWindow(final Inventory inv) {// scheduleAsyncDelayedTask
		CitiTrader.self.getServer().getScheduler().runTaskAsynchronously(CitiTrader.self, new Runnable() {
			@SuppressWarnings("deprecation")
			public void run() {
				// clear the inventory
				for (int i = 0; i < 54; i++) {
					inv.setItem(i, null);
				}
				if (npc.hasTrait(LinkedChestTrait.class)) {
					// if (hasLinkedChest()) {
					for (Entry<Location, String> loc : npc.getTrait(LinkedChestTrait.class).getLinkedChests()
							.entrySet()) {
						if (LinkedChestTrait.isValidChest(loc.getKey().getBlock().getType())) {
							if (loc.getKey().distance(npc.getBukkitEntity().getLocation()) < 10) {
								for (ItemStack is : ((Chest) loc.getKey().getBlock().getState()).getBlockInventory()) {
									if (is == null) {
										continue;
									}
									ItemStack chk;
									chk = is.clone();
									chk.setAmount(1);
									if (inv.contains(chk) == false && getSellPrice(is) > 0.0D) {
										inv.addItem(chk);
									}
								}
							} else {
								// warn own that chest is to far away.
							}
						} else {
							// warn if owner is online, chest doesn'texist.
						}
					}
				} else {
					for (ItemStack is : npc.getTrait(StockRoomTrait.class).getInventory()) {
						if (is == null) {
							continue;
						}
						ItemStack chk;
						chk = is.clone();
						chk.setAmount(1);
						if (inv.contains(chk) == false && getSellPrice(is) > 0.0D) {
							inv.addItem(chk);
						}
					}
				}
			}
		});
	}
}
