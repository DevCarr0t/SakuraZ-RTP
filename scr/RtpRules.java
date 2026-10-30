import java.util.random.RandomGenerator;
final class RtpRules {
 static int sample(RandomGenerator random,int center,int size){validate(center,size);return center-size/2+random.nextInt(size);}
 static void validate(int center,int size){if(size<1||size>1000000||Math.abs((long)center)+size>29999984)throw new IllegalArgumentException("Invalid RTP range");}
 static boolean inRange(int coordinate,int center,int size){long first=(long)center-size/2;return coordinate>=first&&coordinate<first+size;}
 static long remainingSeconds(long expires,long now){return expires<=now?0:(expires-now+999)/1000;}
 static boolean hazardous(String name){return switch(name){case "LAVA","WATER","MAGMA_BLOCK","CACTUS","FIRE","SOUL_FIRE","CAMPFIRE","SOUL_CAMPFIRE","POWDER_SNOW","SWEET_BERRY_BUSH","WITHER_ROSE","POINTED_DRIPSTONE","BEDROCK"->true;default->false;};}
}
