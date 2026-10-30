import java.io.File;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
public class TestRtpConfig {
 public static void main(String[] args)throws Exception{
  var c=new YamlConfiguration();c.load(new File(args[0]));
  if(c.getInt("countdown-seconds")!=5||c.getInt("cooldown-seconds")!=60)throw new AssertionError("Requested timings");
  String[] worlds={"world","world_nether","world_the_end"};int[] widths={1250,156,2500},slots={11,13,15};String[] ids={"overworld","nether","end"};
  for(int i=0;i<3;i++){String p="destinations."+ids[i]+".";if(!c.getString(p+"world").equals(worlds[i])||c.getInt(p+"size-x")!=widths[i]||c.getInt(p+"size-z")!=widths[i]||c.getInt(p+"slot")!=slots[i])throw new AssertionError("Dimension configuration "+ids[i]);}
  for(String key:List.of("no-permission","usage","already-pending","cooldown","countdown","searching","world-unavailable","not-found","teleport-failed","success","cancelled","reloaded","config-failed"))if(c.getString("messages."+key,"").isBlank())throw new AssertionError("Missing actionbar "+key);
  System.out.println("PASS: three world names, GUI slots 11/13/15, dimension sizes, 5s countdown, 60s cooldown and Actionbar templates");
 }
}
