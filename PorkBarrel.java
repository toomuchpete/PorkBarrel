import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.*;

public class PorkBarrel extends Plugin 
{
	// Base Plugin Variables
	private static String name = "PorkBarrel";
	private static String version = "3.01";
	private boolean debug = false;
	static final Logger log = Logger.getLogger("Minecraft");
	
	// Gifter config
	private int giftId  = 0;
	private int giftQty = 1;
	private String giftRecipientFile = "gift.recipients";
	private ArrayList<String> giftRecipients = new ArrayList<String>();
	
	// Auth config
	private String preAuthGroupName = "guest";
	private String postAuthGroupName = "member";
	private String authPassword = "xyzzy";
	private ArrayList<String> validPasswordList = new ArrayList<String>();
	
	// Repair
	private boolean likeRepairsLike = true;
	private HashMap<Integer,Integer> repairingItems = new HashMap<Integer,Integer>();
	private int lighterRepairPerCharge = 0;
	private boolean allowArmorRepair = true;
	ArrayList<Integer> repairables = new ArrayList<Integer>();
	
	// SetGroup config
	private ArrayList<String> sgGroupList = new ArrayList<String>();
	
	public void enable() {
		// TODO: Put the PB config location in the server.properties file
		PropertiesFile properties = new PropertiesFile("porkbarrel.properties");
		
		try {
			debug = properties.getBoolean("debug", false);
		} catch (Exception ex) {
			log.log(Level.SEVERE, name + ": exception while reading from porkbarrel.properties", ex);
			return;
		}		

		// Gifter
		giftId  = properties.getInt("gift-id", giftId);
		giftQty = properties.getInt("gift-quantity", giftQty);
		giftRecipientFile = properties.getString("gift-recipient-file", giftRecipientFile); 
		
		// Auth
		preAuthGroupName  = properties.getString("pre-auth-group", preAuthGroupName);
		postAuthGroupName = properties.getString("post-auth-group", postAuthGroupName);
		authPassword      = properties.getString("auth-password", authPassword);
		
		// Repair
		likeRepairsLike = properties.getBoolean("like-repairs-like", likeRepairsLike);
		lighterRepairPerCharge = properties.getInt("lighter-repair-per-charge", lighterRepairPerCharge);
		allowArmorRepair = properties.getBoolean("allow-armor-repair", allowArmorRepair);
		String repairTemp = properties.getString("repairing-items", "322*1025");
		
		
		// Parse repair list
		String[] itemList = repairTemp.split(",");
		for (int i = 0; i < itemList.length; i++) {
			String[] itemDetails = itemList[i].split("\\*");
			
			if (itemDetails.length == 2) {
				try {
					int itemId = Integer.parseInt(itemDetails[0]);
					int repVal = Integer.parseInt(itemDetails[1]);
					repairingItems.put(itemId, repVal);
				} catch (Exception e) {
					log.info("Error parsing repairing item: " + itemList[i]);
				}
			}
		}
		
		// Repairable Items:
		
			// Wooden Tools (Durability: 33)
			repairables.add(268); // Sword
			repairables.add(269); // Shovel
			repairables.add(270); // Pick
			repairables.add(271); // Axe
			repairables.add(290); // Hoe

			// Leather Armor
			repairables.add(298); // Helmet
			repairables.add(299); // Chestplate
			repairables.add(300); // Leggings
			repairables.add(301); // Boots

			// Chainmail Armor
			repairables.add(302); // Helmet
			repairables.add(303); // Chestplate
			repairables.add(304); // Leggings
			repairables.add(305); // Boots
			
			
			// Golden Tools (Durability: 33)
			repairables.add(283); // Sword
			repairables.add(284); // Shovel
			repairables.add(285); // Pick
			repairables.add(286); // Axe
			repairables.add(294); // Hoe
			repairables.add(314); // Helmet
			repairables.add(315); // Chestplate
			repairables.add(316); // Leggings
			repairables.add(317); // Boots
	
			// Rock Tools (Durability: 65)
			repairables.add(272); // Sword
			repairables.add(273); // Shovel
			repairables.add(274); // Pick
			repairables.add(275); // Axe
			repairables.add(291); // Hoe
			
			// Iron Tools (Durability: 129)
			repairables.add(267); // Sword
			repairables.add(256); // Shovel
			repairables.add(257); // Pick
			repairables.add(258); // Axe
			repairables.add(292); // Hoe
			repairables.add(306); // Helmet
			repairables.add(307); // Chestplate
			repairables.add(308); // Leggings
			repairables.add(309); // Boots
			
	
			// Diamond Tools (Durability: 1025)
			repairables.add(276); // Sword
			repairables.add(277); // Shovel
			repairables.add(278); // Pick
			repairables.add(279); // Axe
			repairables.add(293); // Hoe
			repairables.add(310); // Helmet
			repairables.add(311); // Chestplate
			repairables.add(312); // Leggings
			repairables.add(313); // Boots
			
		
		// SetGroup
		Collections.addAll(sgGroupList, properties.getString("setgroup-list", "default,vip").split(","));
		
		
		// TODO: Add command help
		
		etc.getInstance().addCommand("/authme", " - Get authorized to build.");
		etc.getInstance().addCommand("/setgroup", " playername groupname - Put a player in a group.");
		etc.getInstance().addCommand("/grouplist", " - List of groups available for /setgroup.");
		etc.getInstance().addCommand("/danger", " - Display the status of monster spawning.");
		etc.getInstance().addCommand("/repair", " - Repair a tool in slot 1 with item in slot 2.");
		etc.getInstance().addCommand("/whatrepairs", " - Tells you what materials repair tools or armor.");
		log.info(name + " v" + version + " enabled.");
	}

	public void disable() {
		etc.getInstance().removeCommand("/authme");
		etc.getInstance().removeCommand("/setgroup");
		etc.getInstance().removeCommand("/grouplist");
		etc.getInstance().removeCommand("/danger");
		etc.getInstance().removeCommand("/repair");
		etc.getInstance().removeCommand("/whatrepairs");
		
		log.info(name + " v" + version + " disabled.");
	}

	public void initialize() {
		PBListener listener = new PBListener();
		
		etc.getLoader().addListener(PluginLoader.Hook.IGNITE, listener, this, PluginListener.Priority.HIGH);
		etc.getLoader().addListener(PluginLoader.Hook.ITEM_USE, listener, this, PluginListener.Priority.HIGH);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_PLACE, listener, this, PluginListener.Priority.HIGH);
		etc.getLoader().addListener(PluginLoader.Hook.MOB_SPAWN, listener, this, PluginListener.Priority.HIGH);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.HIGH);
		etc.getLoader().addListener(PluginLoader.Hook.SERVERCOMMAND, listener, this, PluginListener.Priority.HIGH);
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.LOW);
	}
	
	public void debug(String msg) {
		if (debug == true) {
			log.info("[DEBUG] " + msg);
		}
	}
	
	public String colorize(String msg) {
		// Replaces !@0 - !@f with the appropriate color code
		return(msg.replaceAll("\\!\\@([0-9a-f])", "§$1"));
	}

	public String join(String[] s, String glue) {
		int k=s.length;
		if (k==0) {
			return null;
		}
		
		StringBuilder out=new StringBuilder();
		out.append(s[0]);
		
		for (int x=1;x<k;++x) {
			out.append(glue).append(s[x]);
		}
		
		return out.toString();
	}
	
	public class PBListener extends PluginListener // start 
	{
		/**
	     * Called when either a lava block or a lighter tries to light something on fire.
	     * block status depends on the light source:
	     * 1 = lava.
	     * 2 = lighter (flint + steel).
	     * 3 = spread (dynamic spreading of fire).
	     * @param block block that the fire wants to spawn in.
	     * @param player player
	     * @return true if you don't want the fire to ignite.
	     */
	    public boolean onIgnite(Block block, Player player) {
	    	// Feature: PorkRoast
	    	if (block.getStatus() == 2 && !player.canUseCommand("/startfire")) {
	    		debug(player.getName() + " was prevented from using fire.");
	    		player.sendMessage(Colors.Rose + "You are not permitted to light fires.");
	    		return true;
	    	}
	        return false;
	    }
	    
	    /**
	     * Called when someone places a block. Return true to prevent the placement.
	     * 
	     * @param player
	     * @param blockPlaced
	     * @param blockClicked
	     * @param itemInHand
	     * @return true if you want to undo the block placement
	     */
	    public boolean onBlockPlace(Player player, Block blockPlaced, Block blockClicked, Item itemInHand) {
	    	
	    	// Feature: PorkRoast
	    	if (blockPlaced.getType() == 46 && !player.canUseCommand("/placetnt")) {
	    		debug(player.getName() + " was prevented from placing TNT.");
	    		player.sendMessage(Colors.Rose + "You are not permitted to place TNT.");
	    		return true;
	    	}
	        return false;
	    }
	    
	    /**
	     * Called when a player uses an item (rightclick with item in hand)
	     * @param player the player
	     * @param blockPlaced where a block would end up when the item was a bucket
	     * @param blockClicked
	     * @param item the item being used (in hand)
	     * @return true to prevent using the item.
	     */
	    public boolean onItemUse(Player player, Block blockPlaced, Block blockClicked, Item item) {
	    	
	    	// Feature: PorkRoast
	    	if ((item.itemType.getId() >= 325 && item.itemType.getId() <= 327) && !player.canUseCommand("/usebucket")) {
	    		debug(player.getName() + " was prevented from using a bucket.");
	    		player.sendMessage(Colors.Rose + "You are not permitted to use buckets.");
	    		return true;
	    	}
	    	
	        return false;
	    }
	    
	    /**
	     * @param mob Mob attempting to spawn.
	     * @return true if you don't want mob to spawn.
	     */
	    public boolean onMobSpawn(Mob mob) {
	    	// Feature: MobGrounder
	    	Location down = new Location(mob.getX(), mob.getY(), mob.getZ(), 0, 90);
	    	HitBlox blox = new HitBlox(down);
	    	Block block = blox.getTargetBlock();
	    	if (block != null && block.getType() == 18) {
	    		debug("Blocking " + mob.getName() + " from spawning on leaves.");
	    		return true;
	    	}
	        return false;
	    }
	    
	    /**
	     * Called before the command is parsed. Return true if you don't want the
	     * command to be parsed.
	     * 
	     * @param player
	     * @param split
	     * @return false if you want the command to be parsed.
	     */
	    public boolean onCommand(Player player, String[] split) {
			// The plug-in will get any command issued in the game,
			// not just the ones we specifically request, so we have to
			// check to see if we should be acting.
	    	
	    	if (!player.canUseCommand(split[0])) {
	    		return false;
	    	}
	    	
			if (split[0].equalsIgnoreCase("/gift")){
				// Feature: Server Gifts
				if (giftId == 0 || giftQty == 0) {
					player.sendMessage(Colors.Rose + "No gifts are available.");
					debug(player.getName() + " attempted to receive a gift.");
					return true;
				}
				
				boolean alreadyAuthed = giftRecipients.contains(player.getName());
				
				if (alreadyAuthed) {
					player.sendMessage(Colors.Rose + "You've already gotten a gift!");
					return true;
				} else {
					PropertiesFile giftees = new PropertiesFile(giftRecipientFile);
					Integer blockId = giftees.getInt(player.getName(), 0);
					
					if (blockId == 0 || blockId != giftId) {
						player.giveItem(giftId, giftQty);
						giftees.setInt(player.getName(), giftId);
						giftRecipients.add(player.getName());
						player.sendMessage(Colors.Gold + "Here you go!");
						return true;
					} else {
						giftRecipients.add(player.getName());
						player.sendMessage(Colors.Rose + "You've already gotten a gift!");
						return true;
					}
				}
			} else if (split[0].equalsIgnoreCase("/authme")){
				boolean alreadyAuthed = validPasswordList.contains(player.getName());

				// The group is something other than 'default', so this person should be a member
				// New users have one group in the array: an empty string.
				String currentGroup = "";
				if (player.getGroups().length > 0) {
					currentGroup = player.getGroups()[0];
				}
				if (!currentGroup.equalsIgnoreCase(preAuthGroupName) && currentGroup != "") {
					player.sendMessage(Colors.Rose + "You cannot auth.");
					return true;
				}

				
				if (alreadyAuthed) {
					if (split.length < 2 || !split[1].equals("agree")) {
						player.sendMessage(Colors.Rose + "Please agree to the Terms of Service.");
						player.sendMessage(Colors.Rose + "The TOS can be found in the MeFightClub.com Forums.");
						player.sendMessage(Colors.Rose + "Auth syntax: /authme agree");
						return true;
					}
					
					// Players who are members of multiple groups might have complicated permissions,
					// but they should already be "members". In theory, this could be changed to add
					// 'member' if it doesn't exist rather than just quitting here. 
					if (player.getGroups().length > 1) {
						player.sendMessage(Colors.Rose + "You are a member of multiple groups, cannot auth.");
						return true;
					}
					
					// Set the new group and save to users.txt or mysql
					player.setGroups(new String[]{postAuthGroupName});
					if (!etc.getDataSource().doesPlayerExist(player.getName())) {
						etc.getDataSource().addPlayer(player);
					} else {
						etc.getDataSource().modifyPlayer(player);
					}
					
					// Send notifications to issuer and target
					player.sendMessage(Colors.Green + "You are now a member.");
					log.info(player.getName() + " has agreed to the TOS.");
					validPasswordList.remove(player.getName());
					return true;
				} else {
					if (split.length < 2) {
						player.sendMessage(Colors.Rose + "Auth syntax: /authme <password>");
						return true;
					}
					
					if (split[1].equals(authPassword)) {
						player.sendMessage(Colors.Green + "Password accepted!");
						log.info(player.getName() + " has entered the correct password.");
						validPasswordList.add(player.getName());
						player.sendMessage(Colors.Rose + "To be authorized you must agree to the Terms of Service.");
						player.sendMessage(Colors.Rose + "The TOS can be found in the MeFightClub.com Forums.");
						player.sendMessage(Colors.Rose + "If you agree, issue command: /authme agree");
						return true;
					}
					
					player.sendMessage(Colors.Rose + "Invalid password.");
					log.info(player.getName() + " has entered an invalid password. (" + split[1] + ")");
					return true;
				}
			} else if (split[0].equalsIgnoreCase("/grouplist")) {
				player.sendMessage(Colors.LightGray + "Available Groups:");
				for (String g : sgGroupList) {
					player.sendMessage(Colors.LightGray + " - " + g);
				}
				return true;
			} else if (split[0].equalsIgnoreCase("/setgroup")) {
				if (split.length != 3) {
					player.sendMessage(Colors.Rose + "Syntax: /setgroup <player> <group>");
					return true;
				}			

				// Get a player if they're online
				Player targetPlayer = etc.getServer().getPlayer(split[1]);
				String targetGroup = split[2];
		
				// We can only work with online players right now
				if (targetPlayer == null) {
					player.sendMessage(Colors.Rose + split[1] + " is not online.");
					return true;
				}
				
				// Check that player is only in these groups
				String[] currentGroups = targetPlayer.getGroups();
				if (currentGroups.length > 0) {
					for (String g : currentGroups) {
						if (!sgGroupList.contains(g)) {
							player.sendMessage(Colors.Rose + player.getName() + " is a member of " + g + " and cannot be changed.");
							log.info(player.getName() + " failed to change " + targetPlayer.getName() + " to " + targetGroup);
							return true;
						}
					}
				}
				
				// Check that the assigned group is in the allowed groups
				if (!sgGroupList.contains(targetGroup)) {
					player.sendMessage(Colors.Rose + targetGroup + " is not a valid group.");
					log.info(player.getName() + " failed to change " + targetPlayer.getName() + " to " + targetGroup);
					return true;				
				}
				
				// Change the player groups
				targetPlayer.setGroups(new String[]{targetGroup});
				if (!etc.getDataSource().doesPlayerExist(targetPlayer.getName())) {
					etc.getDataSource().addPlayer(targetPlayer);
				} else {
					etc.getDataSource().modifyPlayer(targetPlayer);
				}
				
				player.sendMessage(Colors.Green + targetPlayer.getName() + " is now a member of the '" + targetGroup + "' group.");
				targetPlayer.sendMessage(Colors.Green + "You are now a member of the '" + targetGroup + "' group.");
				log.info(player.getName() + " set " + targetPlayer.getName() + "'s group to '" + targetGroup + "'.");
				return true;
			} else if (split[0].equalsIgnoreCase("/rawmsg") || split[0].equalsIgnoreCase("/rm")) {
				if (split.length < 3) {
					player.sendMessage(Colors.Rose + "Incorrect message syntax.");
					return true;
				}
				
				List<Player> targetList = new ArrayList<Player>();
				
				if (split[1].equalsIgnoreCase("*")) {
					targetList = etc.getServer().getPlayerList();
				} else {
					// Get a player if they're online
					Player targetPlayer = etc.getServer().getPlayer(split[1]);

					// We can only work with online players right now
					if (targetPlayer == null) {
						player.sendMessage(Colors.Rose + split[1] + " is not online.");
						return true;
					}
					
					targetList.add(targetPlayer);
				} 
				
				// Build the message
				int k = split.length;
				StringBuilder msg =  new StringBuilder();
				for (int i=2; i < k; i++) {
					msg.append(split[i]).append(' ');
				}
				String message = msg.toString();
				
				// Make colors work
				int sentCount = 0;
				message = colorize(message);
				for (Player p: targetList) {
					p.sendMessage(message);
					sentCount++;
				}
				
				if (sentCount == 1) {
					player.sendMessage(Colors.Green + "Message sent.");
				} else {
					player.sendMessage(Colors.Green + sentCount + " messages sent.");
				}
				
				return true;
			} else if (split[0].equalsIgnoreCase("/danger")) {
				// Check so this fails gracefully on updates:
				try {
					if (etc.getMCServer().e.k == 1) {
						player.sendMessage(Colors.Rose + "The world feels dangerous.");
					} else {
						player.sendMessage(Colors.LightGreen + "The world feels safe.");
					}
				} catch (Exception e) {
					PropertiesFile serverProps = new PropertiesFile("server.properties");
					if (serverProps.getBoolean("spawn-monsters", true)) {
						player.sendMessage(Colors.Rose + "You're not sure, but the world seems dangerous.");
					} else {
						player.sendMessage(Colors.LightGreen + "You're not sure, but the world seems safe.");
					}
					serverProps = null;
				}
				
				return true;
			} else if (split[0].equalsIgnoreCase("/repair")) {
				// Empty slot
				if (player.getInventory().getItemFromSlot(0) == null || player.getInventory().getItemFromSlot(1) == null) {
					player.sendMessage(Colors.Rose + "Repairable tool goes in slot one, repairing item goes in slot two.");
					return true;
				}
				
				// Get the info from the two slots
				Item toRepair = player.getInventory().getItemFromSlot(0);
				int toRepairId = toRepair.getItemId();
				
				Item repairWith = player.getInventory().getItemFromSlot(1);
				int repairWithId = repairWith.getItemId();
				
				// Check against the items that can be repaired
				if (!repairables.contains(toRepairId)) {
					player.sendMessage(Colors.Rose + "Your " + etc.getDataSource().getItem(toRepairId) + " is not repairable.");
					return true;
				}
				
				// Does the item actually need to be repaired?
				if (toRepair.getDamage() == 0) {
					player.sendMessage(Colors.Rose + "Your " + etc.getDataSource().getItem(toRepairId) + " does not need repairing.");
					return true;
				}
				
				// "like repairs like" means one of what the item was built of is enough to fully repair a tool
				// For example: one diamond fully repairs a diamond pick
				if (likeRepairsLike) {
					boolean doRepair = false;
					
					switch(toRepairId) {
						// Wooden Tools (Durability: 33)
						case 268: // Sword
						case 269: // Shovel
						case 270: // Pick
						case 271: // Axe
						case 290: // Hoe
							if (repairWithId == 5) { doRepair = true; }
							break;
	
							// Golden Tools (Durability: 33)
						case 283: // Sword
						case 284: // Shovel
						case 285: // Pick
						case 286: // Axe
						case 294: // Hoe
						case 314: // Helmet
						case 315: // Chestplate
						case 316: // Leggings
						case 317: // Boots
							
							if (repairWithId == 266) { doRepair = true; }
							break;
				
						// Rock Tools (Durability: 65)
						case 272: // Sword
						case 273: // Shovel
						case 274: // Pick
						case 275: // Axe
						case 291: // Hoe
							if (repairWithId == 4) { doRepair = true; }
							break;
						
						// Leather Armor
						case 298: // Helmet
						case 299: // Chestplate
						case 300: // Leggings
						case 301: // Boots
						case 302: // Chain Helmet
						case 303: // Chain Chestplate
						case 304: // Chain Leggings
						case 305: // Chain Boots
							if (repairWithId == 334) { doRepair = true; }
							break;

							
						// Iron Tools (Durability: 129)
						case 267: // Sword
						case 256: // Shovel
						case 257: // Pick
						case 258: // Axe
						case 292: // Hoe
						case 306: // Helmet
						case 307: // Chestplate
						case 308: // Leggings
						case 309: // Boots

							if (repairWithId == 265) { doRepair = true; }
							break;
				
						// Diamond Tools (Durability: 1025)
						case 276: // Sword
						case 277: // Shovel
						case 278: // Pick
						case 279: // Axe
						case 293: // Hoe
						case 310: // Helmet
						case 311: // Chestplate
						case 312: // Leggings
						case 313: // Boots
							if (repairWithId == 264) { doRepair = true; }
							break;
					}
					
					if (doRepair) {
						// Place the tool in inventory
						player.getInventory().setSlot(toRepairId, 1, 0, 0);
						
						// Update the item being repaired with
						if (repairWith.getAmount() > 1) {
							repairWith.setAmount(repairWith.getAmount()-1);
							player.getInventory().setSlot(repairWith, 1);
						} else {
							player.getInventory().removeItem(1);
						}
						
						player.sendMessage(Colors.LightGreen + "Your " + etc.getDataSource().getItem(toRepairId) + " is as good as new!");
						return true;
					}
				}
				
				boolean doLighterRepair = false;
				if (lighterRepairPerCharge > 0 && repairWithId == 259) {
					doLighterRepair = true;
				}
				
				
				// Check that the item in the repair-with slot is okay to repair with
				if (!doLighterRepair && !repairingItems.containsKey(repairWithId)) {
					player.sendMessage(Colors.Rose + "You cannot repair tools with " + etc.getDataSource().getItem(repairWithId) + ".");
					return true;
				}

				int curDamage = toRepair.getDamage();
				int repairPerItem = 0;
				int repairItemQty = repairWith.getAmount();
				int repairNumber = 1;
				
				if (doLighterRepair) {
					repairPerItem = lighterRepairPerCharge;
					repairItemQty = 65 - repairWith.getDamage();
				} else {
					repairPerItem = repairingItems.get(repairWithId);
				}

				// "/repair max" will use as many items from slot1 as is required to fully-repair an item
				if (split.length == 2 && split[1].equalsIgnoreCase("max")) {
					int requestedQty = curDamage / repairPerItem;
					
					// Integer division above discards remainders
					if (curDamage % repairPerItem != 0) { requestedQty++; }
					
					// Don't allow more than slot1 to be used.
					repairNumber = Math.min(repairItemQty, requestedQty);
				}
				
				// Repair the tool
				if (curDamage < (repairPerItem * repairNumber)) {
					curDamage = 0;
					player.sendMessage(Colors.LightGreen + "Your " + etc.getDataSource().getItem(toRepairId) + " is as good as new!");
				} else {
					curDamage = toRepair.getDamage() - (repairPerItem * repairNumber);
					player.sendMessage(Colors.LightGreen + "Your " + etc.getDataSource().getItem(toRepairId) + " has been patched up.");
				}
				player.getInventory().setSlot(toRepairId, 1, curDamage, 0);
				
				// Update slot1
				if (doLighterRepair && (repairWith.getDamage() + repairNumber < 65)) {
					player.getInventory().setSlot(259, 1, repairWith.getDamage() + repairNumber, 1);
				} else if (repairWith.getAmount() > repairNumber) {
					repairWith.setAmount(repairWith.getAmount() - repairNumber);
					player.getInventory().setSlot(repairWith, 1);
				} else {
					player.getInventory().removeItem(1);
				}
				return true;
			} else if (split[0].equalsIgnoreCase("/whatrepairs")) {
				if (allowArmorRepair) {
					player.sendMessage(Colors.LightBlue + "Tools and armor may both be repaired.");
				} else {
					player.sendMessage(Colors.LightBlue + "Only tools may be repaired.");
				}
				
				if (likeRepairsLike) {
					player.sendMessage(Colors.LightGray + "One of an item's material will fully repair it.");
				}
				
				if (lighterRepairPerCharge > 0) {
					player.sendMessage(Colors.LightGray + "One charge of a flint & steel repairs items for " + lighterRepairPerCharge + " durability.");
				}
				
				if (repairingItems.size() > 0) {
					player.sendMessage(Colors.LightGray + "The following items will also perform repairs:");
					for (int id : repairingItems.keySet()) {
						player.sendMessage(Colors.LightGray + " - " + etc.getDataSource().getItem(id) + ": " + repairingItems.get(id));
					}
				}
				return true;
			}
			
	    	return false;
	    } //onCommand
	    
	    /**
	     * Called before the console command is parsed. Return true if you don't
	     * want the server command to be parsed by the server.
	     * 
	     * @param split
	     * @return false if you want the command to be parsed.
	     */
	    public boolean onConsoleCommand(String[] split) {
			// The plug-in will get any command issued in the game,
			// not just the ones we specifically request, so we have to
			// check to see if we should be acting.
			if (split[0].equalsIgnoreCase("save-and-announce")){
				etc.getServer().useConsoleCommand("say Writing world to disk...");
				etc.getServer().useConsoleCommand("save-all");
				etc.getServer().useConsoleCommand("say Write complete.");
				return true;
			} else if (split[0].equalsIgnoreCase("rawmsg") || split[0].equalsIgnoreCase("rm")) {
				if (split.length < 3) {
					log.info("[RM] Incorrect message syntax.");
					return true;
				}
				
				List<Player> targetList = new ArrayList<Player>();
				
				if (split[1].equalsIgnoreCase("*")) {
					targetList = etc.getServer().getPlayerList();
				} else {
					// Get a player if they're online
					Player targetPlayer = etc.getServer().getPlayer(split[1]);

					// We can only work with online players right now
					if (targetPlayer == null) {
						log.info("[RM] " + split[1] + " is not online.");
						return true;
					}
					
					targetList.add(targetPlayer);
				}
				
				// Build the message
				int k = split.length;
				StringBuilder msg =  new StringBuilder();
				for (int i=2; i < k; i++) {
					msg.append(split[i]).append(' ');
				}
				String message = msg.toString();
				
				// Make colors work
				int sentCount = 0;
				message = colorize(message);
				for (Player p: targetList) {
					p.sendMessage(message);
					sentCount++;
				}
				
				if (sentCount == 1) {
					log.info("[RM] " + "Message sent.");
				} else {
					log.info("[RM] " + sentCount + " messages sent.");
				}
				
				return true;
			}
			return false;
	    } //onConsoleCommand

	    /**
	     * Called during the later login process
	     * 
	     * @param player
	     */
	    public void onLogin(Player player) {
	    	onCommand(player, new String[] {"/danger"});
	    }	    
	} //PBListener
} //PorkBarrel