import java.util.Random;
public final class TestRtpRules {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  var random=new Random(42);
  for(int size:new int[]{2500,312,1,5}){
   for(int center:new int[]{0,-700,100})for(int i=0;i<10000;i++)check(RtpRules.inRange(RtpRules.sample(random,center,size),center,size),"Sample outside full side length");
   check(RtpRules.inRange(-size/2,0,size),"Lower boundary excluded");check(RtpRules.inRange(-size/2+size-1,0,size),"Upper valid coordinate excluded");check(!RtpRules.inRange(-size/2+size,0,size),"Upper exclusive boundary included");
  }
  check(RtpRules.remainingSeconds(60000,0)==60,"One-minute cooldown");check(RtpRules.remainingSeconds(60000,59001)==1,"Remaining time must round up");check(RtpRules.remainingSeconds(60000,60000)==0,"Expiry boundary");
  for(String type:new String[]{"LAVA","WATER","MAGMA_BLOCK","POWDER_SNOW","BEDROCK","CACTUS","CAMPFIRE"})check(RtpRules.hazardous(type),"Unsafe landing type accepted");
  for(String type:new String[]{"GRASS_BLOCK","STONE","NETHERRACK","END_STONE"})check(!RtpRules.hazardous(type),"Safe floor rejected");
  try{RtpRules.validate(0,0);throw new AssertionError("Empty range accepted");}catch(IllegalArgumentException expected){}
  System.out.println("PASS: 120,000 range samples, full-width boundaries, one-minute cooldown rounding, safe/hazard floor types and invalid range rejection");
 }
}
