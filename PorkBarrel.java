import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.*;

public class PorkBarrel extends Plugin 
{
	// Base Plugin Variables
	private static String name = "PorkBarrel";
	private static int version = 2;
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
		
		// SetGroup
		Collections.addAll(sgGroupList, properties.getString("setgroup-list", "default,vip").split(","));
		
		
		// TODO: Add command help
		// etc.getInstance().addCommand("/lb", " - LogBlock display command.");
		log.info(name + " v" + version + " enabled.");
	}

	public void disable() {
		// etc.getInstance().removeCommand("/lb");
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

	String join(String[] s, String glue) {
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