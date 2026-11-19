package cn.charlotte.pit.impl;

import cn.charlotte.pit.bot.BotHandler;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.npc.CitizensNPCRegistry;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class BotHandlerImpl implements BotHandler {

    public static final BotHandlerImpl INSTANCE = new BotHandlerImpl();

    private static CitizensNPCRegistry npcRegistry;

    private BotHandlerImpl() {
    }

    public static CitizensNPCRegistry getNpcRegistry() {
        if (npcRegistry == null) {
            npcRegistry = new CitizensNPCRegistry(new MemoryNPCDataStore());
        }
        return npcRegistry;
    }

    @Override
    public void spawnBot(Location location, String name) {
        getNpcRegistry().createNPC(EntityType.PLAYER, name);
    }

    public static class BotConfig {

        private boolean enabled = false;
        private IntRange prestigeRange = new IntRange(40, 120);
        private IntRange levelRange = new IntRange(40, 120);
        private final HashMap<Integer, List<RandomObj>> armorRandoms = new HashMap<>();
        private final ArrayList<RandomObj> heldItems = new ArrayList<>();
        private double bountyChance = 0.08;
        private IntRange bountyValue = new IntRange(100, 500);

        public static void load(Configuration config) {
            BotConfig botConfig = new BotConfig();
            botConfig.enabled = config.getBoolean("enable");
            botConfig.prestigeRange = parseRange(config.getString("prestige"));
            botConfig.levelRange = parseRange(config.getString("prestige"));

            List<String> armorStrings = config.getStringList("armor");
            List<List<RandomObj>> armors = new ArrayList<>();
            for (String armorString : armorStrings) {
                List<RandomObj> randomObjs = new ArrayList<>();
                for (String randomString : armorString.split("/")) {
                    String[] split = randomString.split(":");
                    RandomObj randomObj = new RandomObj(split[0]);
                    randomObj.setRandomValue(Double.parseDouble(split[1]));
                    randomObjs.add(randomObj);
                }
                armors.add(randomObjs);
            }

            for (int index = 0; index < 4 && index < armors.size(); index++) {
                botConfig.armorRandoms.put(index, armors.get(index));
            }

            String held = config.getString("held");
            if (held != null) {
                for (String line : held.split("/")) {
                    String[] split = line.split(":");
                    RandomObj randomObj = new RandomObj(split[0]);
                    randomObj.setRandomValue(Double.parseDouble(split[1]));
                    botConfig.heldItems.add(randomObj);
                }
            }

            botConfig.bountyChance = config.getDouble("bounty-chance");
            botConfig.bountyValue = parseRange(config.getString("bounty-value"));
        }

        private static IntRange parseRange(String value) {
            if (value == null) {
                return new IntRange(0, 0);
            }
            String[] split = value.split("-");
            return new IntRange(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        }

        public boolean isEnabled() {
            return enabled;
        }

        public IntRange getPrestigeRange() {
            return prestigeRange;
        }

        public IntRange getLevelRange() {
            return levelRange;
        }

        public HashMap<Integer, List<RandomObj>> getArmorRandoms() {
            return armorRandoms;
        }

        public ArrayList<RandomObj> getHeldItems() {
            return heldItems;
        }

        public double getBountyChance() {
            return bountyChance;
        }

        public IntRange getBountyValue() {
            return bountyValue;
        }

        public static final class RandomObj {

            private final String content;
            private double randomValue;

            public RandomObj(String content) {
                this.content = content;
            }

            public String getContent() {
                return content;
            }

            public double getRandomValue() {
                return randomValue;
            }

            public void setRandomValue(double randomValue) {
                this.randomValue = randomValue;
            }
        }

        public static final class IntRange {

            private final int start;
            private final int endInclusive;

            public IntRange(int start, int endInclusive) {
                this.start = start;
                this.endInclusive = endInclusive;
            }

            public int getStart() {
                return start;
            }

            public int getEndInclusive() {
                return endInclusive;
            }
        }
    }
}
