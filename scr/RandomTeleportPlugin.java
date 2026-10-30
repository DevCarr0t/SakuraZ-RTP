import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class RandomTeleportPlugin extends JavaPlugin implements Listener,CommandExecutor,TabCompleter {
 record Destination(String id,String world,String name,Material icon,int slot,int sizeX,int sizeZ,int centerX,int centerZ,int minY,int maxY){}
 static final class Menu implements InventoryHolder {
  final UUID owner;final List<Destination> options;Inventory inventory;
  Menu(UUID owner,List<Destination> options){this.owner=owner;this.options=options;}
  public Inventory getInventory(){return inventory;}
 }
 static final class Request {
  final UUID player;final Destination destination;final long deadline;
  BukkitTask countdown;boolean teleporting,cancelled,arrived;Location landing;int attempts;
  Request(UUID player,Destination destination,long deadline){this.player=player;this.destination=destination;this.deadline=deadline;}
 }
 private List<Destination> destinations=List.of();
 private final Map<UUID,Request> pending=new HashMap<>();
 private final Map<UUID,Long> cooldowns=new HashMap<>();
 private boolean combatCheckQueued;
 private int countdownSeconds,cooldownSeconds,maxAttempts,timeoutSeconds;
 public void onEnable(){
  saveDefaultConfig();load();loadCooldowns();getServer().getPluginManager().registerEvents(this,this);getCommand("rtp").setExecutor(this);getCommand("rtp").setTabCompleter(this);
 }
 private void load(){
  int countdown=getConfig().getInt("countdown-seconds",5),cooldown=getConfig().getInt("cooldown-seconds",60),attempts=getConfig().getInt("search.max-attempts",80),timeout=getConfig().getInt("search.timeout-seconds",30);
  if(countdown<1||countdown>60||cooldown<1||cooldown>86400||attempts<1||attempts>500||timeout<5||timeout>120)throw new IllegalArgumentException("Invalid RTP timing/search settings");
  var worlds=getConfig().getConfigurationSection("destinations");if(worlds==null)throw new IllegalArgumentException("Missing destinations");
  List<Destination> options=new ArrayList<>();Set<Integer> slots=new HashSet<>();
  for(String id:worlds.getKeys(false)){
   var c=worlds.getConfigurationSection(id);int slot=c.getInt("slot"),sizeX=c.getInt("size-x"),sizeZ=c.getInt("size-z"),cx=c.getInt("center-x",0),cz=c.getInt("center-z",0);
   if(slot<0||slot>=27||!slots.add(slot))throw new IllegalArgumentException("Invalid/duplicate RTP slot");RtpRules.validate(cx,sizeX);RtpRules.validate(cz,sizeZ);
   int min=c.getInt("nether-min-y",32),max=c.getInt("nether-max-y",110);if(min<1||max>=126||min>max)throw new IllegalArgumentException("Invalid Nether floor range");
   Material icon=Material.valueOf(c.getString("icon"));if(icon.isAir()||!icon.isItem())throw new IllegalArgumentException("Invalid RTP icon");
   options.add(new Destination(id,Objects.requireNonNull(c.getString("world")),c.getString("name",id),icon,slot,sizeX,sizeZ,cx,cz,min,max));
  }
  if(options.isEmpty())throw new IllegalArgumentException("At least one RTP destination is required");
  destinations=List.copyOf(options);countdownSeconds=countdown;cooldownSeconds=cooldown;maxAttempts=attempts;timeoutSeconds=timeout;
 }
 private String color(String text){return ChatColor.translateAlternateColorCodes('&',text);}
 private void teleportSound(Player player){playRtpSound(player,"teleport-success","entity.enderman.teleport");}
 private void countdownSound(Player player){playRtpSound(player,"countdown","block.note_block.hat");}
 private void playRtpSound(Player player,String key,String fallback){
  String path="sounds."+key;
  if(!getConfig().getBoolean(path+".enabled",true)||!player.isOnline())return;
  String sound=getConfig().getString(path+".sound",fallback);
  if(sound.isBlank())return;
  float volume=(float)getConfig().getDouble(path+".volume",1.0),pitch=(float)getConfig().getDouble(path+".pitch",1.0);
  if(!Float.isFinite(volume)||volume<0||!Float.isFinite(pitch)||pitch<=0){getLogger().warning("Invalid RTP sound volume/pitch");return;}
  try{player.playSound(player.getLocation(),sound,volume,pitch);}catch(RuntimeException error){getLogger().warning("Could not play RTP sound: "+error.getMessage());}
 }
 private void message(Player player,String key,String... tokens){
  String text=getConfig().getString("messages."+key,"");for(int i=0;i+1<tokens.length;i+=2)text=text.replace("{"+tokens[i]+"}",tokens[i+1]);
  if(!text.isBlank()&&player.isOnline())player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
 }
 public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
  if(!(sender instanceof Player player)){getLogger().info("RTP uses an in-game GUI and Actionbar messages.");return true;}
  if(combatBlocked(player))return true;
  if(args.length==1&&args[0].equalsIgnoreCase("reload")){
   if(!player.hasPermission("randomteleportplugin.admin")){message(player,"no-permission");return true;}
   try{var checked=new YamlConfiguration();checked.load(new File(getDataFolder(),"config.yml"));reloadConfig();load();message(player,"reloaded");}catch(Exception error){message(player,"config-failed");getLogger().log(java.util.logging.Level.WARNING,"RTP config reload failed",error);}return true;
  }
  if(args.length!=0){message(player,"usage");return true;}
  if(!player.hasPermission("randomteleportplugin.use")){message(player,"no-permission");return true;}
  if(!available(player))return true;
  Menu menu=new Menu(player.getUniqueId(),destinations);menu.inventory=Bukkit.createInventory(menu,27,color(getConfig().getString("menu.title","Random Teleport")));
  for(Destination target:menu.options){
   ItemStack item=new ItemStack(target.icon);ItemMeta meta=item.getItemMeta();meta.setDisplayName(color(target.name));
   meta.setLore(getConfig().getStringList("menu.item-lore").stream().map(line->color(line.replace("{size_x}",String.valueOf(target.sizeX)).replace("{size_z}",String.valueOf(target.sizeZ)))).toList());item.setItemMeta(meta);menu.inventory.setItem(target.slot,item);
  }
  player.openInventory(menu.inventory);return true;
 }
 public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){return List.of();}
 private boolean inCombat(Player player){
  long now=System.currentTimeMillis();for(var value:player.getMetadata("icesmp-combat-until"))if(value.getOwningPlugin()!=null&&value.getOwningPlugin().getName().equals("IceCurrencies")&&value.getOwningPlugin().isEnabled()&&value.asLong()>now)return true;return false;
 }
 private boolean combatBlocked(Player player){if(!inCombat(player))return false;player.sendMessage(ChatColor.RED+"You cannot use /rtp while in combat.");return true;}
 private boolean available(Player player){
  if(combatBlocked(player))return false;
  if(pending.containsKey(player.getUniqueId())){message(player,"already-pending");return false;}
  long seconds=RtpRules.remainingSeconds(cooldowns.getOrDefault(player.getUniqueId(),0L),System.currentTimeMillis());
  if(seconds>0){message(player,"cooldown","seconds",String.valueOf(seconds));return false;}return true;
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void click(InventoryClickEvent event){
  if(!(event.getView().getTopInventory().getHolder() instanceof Menu menu))return;boolean cancelled=event.isCancelled();event.setCancelled(true);
  if(cancelled||!(event.getWhoClicked() instanceof Player player)||!menu.owner.equals(player.getUniqueId())||(event.getClick()!=ClickType.LEFT&&event.getClick()!=ClickType.RIGHT))return;
  for(Destination target:menu.options)if(event.getRawSlot()==target.slot){getServer().getScheduler().runTask(this,()->{
   if(player.isOnline()&&player.getOpenInventory().getTopInventory()==menu.inventory){player.closeInventory();if(player.hasPermission("randomteleportplugin.use"))start(player,target);else message(player,"no-permission");}
  });break;}
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder() instanceof Menu)event.setCancelled(true);}
 private void start(Player player,Destination target){
  if(!available(player))return;World world=getServer().getWorld(target.world);if(world==null){message(player,"world-unavailable");return;}
  Request request=new Request(player.getUniqueId(),target,System.currentTimeMillis()+(countdownSeconds+timeoutSeconds)*1000L);pending.put(request.player,request);
  message(player,"countdown","seconds",String.valueOf(countdownSeconds),"world",ChatColor.stripColor(color(target.name)));countdownSound(player);
  int[] remaining={countdownSeconds};request.countdown=getServer().getScheduler().runTaskTimer(this,()->{
   if(!current(request)){cancel(request);return;}
   if(--remaining[0]>0){message(player,"countdown","seconds",String.valueOf(remaining[0]),"world",ChatColor.stripColor(color(target.name)));countdownSound(player);}
   else{request.countdown.cancel();message(player,"searching");search(request);}
  },20,20);
  getServer().getScheduler().runTaskLater(this,()->{if(current(request)&&System.currentTimeMillis()>=request.deadline&&!request.teleporting)fail(request,"not-found");},(countdownSeconds+timeoutSeconds)*20L+1);
 }
 private boolean current(Request request){Player player=getServer().getPlayer(request.player);if(request.cancelled||!isEnabled()||pending.get(request.player)!=request||player==null||!player.isOnline())return false;if(inCombat(player)){cancel(request);return false;}return true;}
 private void cancel(Request request){request.cancelled=true;if(!request.teleporting)pending.remove(request.player,request);if(request.countdown!=null)request.countdown.cancel();}
 private void fail(Request request,String key){if(!current(request))return;Player player=getServer().getPlayer(request.player);cancel(request);message(player,key);}
 private void search(Request request){
  if(!current(request))return;
  if(++request.attempts>maxAttempts||System.currentTimeMillis()>request.deadline){fail(request,"not-found");return;}
  World world=getServer().getWorld(request.destination.world);if(world==null){fail(request,"world-unavailable");return;}
  int x=RtpRules.sample(ThreadLocalRandom.current(),request.destination.centerX,request.destination.sizeX),z=RtpRules.sample(ThreadLocalRandom.current(),request.destination.centerZ,request.destination.sizeZ);
  // No off-thread Bukkit block reads. Chunk generation is asynchronous; inspection returns to the main thread.
  world.getChunkAtAsync(x>>4,z>>4,true).whenComplete((chunk,error)->{
   if(!isEnabled())return;getServer().getScheduler().runTask(this,()->{
    if(!current(request))return;if(error!=null){fail(request,"not-found");getLogger().log(java.util.logging.Level.WARNING,"RTP chunk loading failed",error);return;}
    Location landing=find(world,x,z,request.destination);
    if(landing==null){search(request);return;}
    Player player=getServer().getPlayer(request.player);landing.setYaw(player.getLocation().getYaw());request.teleporting=true;request.landing=landing.clone();
    player.teleportAsync(landing,PlayerTeleportEvent.TeleportCause.PLUGIN).whenComplete((success,teleportError)->{
     if(!isEnabled())return;getServer().getScheduler().runTask(this,()->{
      request.teleporting=false;
      if(!current(request)){cancel(request);return;}
      if(teleportError!=null||!Boolean.TRUE.equals(success)){fail(request,"teleport-failed");return;}
      cooldowns.put(request.player,System.currentTimeMillis()+cooldownSeconds*1000L);cancel(request);player.setFallDistance(0);saveCooldowns();teleportSound(player);
      message(player,"success","world",ChatColor.stripColor(color(request.destination.name)),"x",String.valueOf(landing.getBlockX()),"y",String.valueOf(landing.getBlockY()),"z",String.valueOf(landing.getBlockZ()));
     });
    });
   });
  });
 }
 private Location find(World world,int x,int z,Destination target){
  if(world.getEnvironment()==World.Environment.NETHER){
   for(int y=Math.min(target.maxY,world.getMaxHeight()-3);y>=Math.max(target.minY,world.getMinHeight()+1);y--)if(safe(world,x,y,z))return insideBorder(new Location(world,x+0.5,y+1,z+0.5));
   return null;
  }
  // Highest surface only: never scan down into Overworld caves or underwater floors.
  int y=world.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);
  if(y<=world.getMinHeight()||y+2>=world.getMaxHeight()||!safe(world,x,y,z))return null;
  return insideBorder(new Location(world,x+0.5,y+1,z+0.5));
 }
 private Location insideBorder(Location location){
  WorldBorder border=location.getWorld().getWorldBorder();
  if(!border.isInside(location)||!border.isInside(location.clone().add(1,0,1))||!border.isInside(location.clone().add(-1,0,-1)))return null;return location;
 }
 private boolean safe(World world,int x,int y,int z){
  Block floor=world.getBlockAt(x,y,z);Material type=floor.getType();
  if(!type.isOccluding()||RtpRules.hazardous(type.name())||Tag.LEAVES.isTagged(type)||!world.getBlockAt(x,y+1,z).getType().isAir()||!world.getBlockAt(x,y+2,z).getType().isAir())return false;
  // Only inspect within the already loaded chunk; reject edge candidates rather than synchronously loading neighbors.
  if((x&15)==0||(x&15)==15||(z&15)==0||(z&15)==15)return false;
  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=0;dy<=2;dy++)if(RtpRules.hazardous(world.getBlockAt(x+dx,y+dy,z+dz).getType().name()))return false;
  return true;
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void combatDamage(EntityDamageByEntityEvent event){
  if(!(event.getEntity() instanceof Player)||pending.isEmpty()||combatCheckQueued)return;
  // Combat tagging may run later at the same event priority; inspect metadata next tick.
  combatCheckQueued=true;getServer().getScheduler().runTask(this,()->{combatCheckQueued=false;for(Request request:new ArrayList<>(pending.values())){Player player=getServer().getPlayer(request.player);if(player!=null&&inCombat(player))cancel(request);}});
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void move(PlayerMoveEvent event){
  if(event instanceof PlayerTeleportEvent||event.getTo()==null)return;
  Location from=event.getFrom(),to=event.getTo();
  if(from.getWorld()==to.getWorld()&&from.getX()==to.getX()&&from.getY()==to.getY()&&from.getZ()==to.getZ())return;
  Request request=pending.get(event.getPlayer().getUniqueId());
  if(request!=null&&!request.cancelled&&!request.arrived){cancel(request);message(event.getPlayer(),"moved");}
 }
 @EventHandler public void quit(PlayerQuitEvent event){Request request=pending.get(event.getPlayer().getUniqueId());if(request!=null)cancel(request);}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void teleport(PlayerTeleportEvent event){
  Request request=pending.get(event.getPlayer().getUniqueId());if(request==null||request.cancelled)return;
  boolean own=request.teleporting&&event.getCause()==PlayerTeleportEvent.TeleportCause.PLUGIN&&request.landing.equals(event.getTo());
  if(own)request.arrived=true;else{cancel(request);message(event.getPlayer(),"cancelled");}
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void combatTeleport(PlayerTeleportEvent event){
  Request request=pending.get(event.getPlayer().getUniqueId());if(request!=null&&request.teleporting&&request.cancelled&&event.getCause()==PlayerTeleportEvent.TeleportCause.PLUGIN&&request.landing.equals(event.getTo())){event.setCancelled(true);return;}if(request!=null&&request.teleporting&&inCombat(event.getPlayer())){event.setCancelled(true);cancel(request);combatBlocked(event.getPlayer());}
 }
 private void loadCooldowns(){
  File file=new File(getDataFolder(),"cooldowns.yml");if(!file.exists())return;
  try{var data=new YamlConfiguration();data.load(file);long now=System.currentTimeMillis();for(String key:data.getKeys(false)){long expires=data.getLong(key);if(expires>now)cooldowns.put(UUID.fromString(key),expires);}}
  catch(Exception error){throw new IllegalStateException("Invalid RTP cooldown data",error);}
 }
 private void saveCooldowns(){
  try{
   Files.createDirectories(getDataFolder().toPath());var data=new YamlConfiguration();long now=System.currentTimeMillis();cooldowns.forEach((id,expires)->{if(expires>now)data.set(id.toString(),expires);});
   Path temp=Files.createTempFile(getDataFolder().toPath(),"cooldowns-",".tmp"),target=getDataFolder().toPath().resolve("cooldowns.yml");
   try{data.save(temp.toFile());try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException error){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
  }catch(IOException error){getLogger().log(java.util.logging.Level.SEVERE,"Cannot save RTP cooldowns",error);}
 }
 public void onDisable(){for(Request request:new ArrayList<>(pending.values())){Player player=getServer().getPlayer(request.player);if(player!=null)message(player,"cancelled");cancel(request);}pending.clear();saveCooldowns();}
}
