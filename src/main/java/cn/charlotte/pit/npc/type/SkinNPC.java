package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.npc.AbstractPitNPC;
import cn.charlotte.pit.npc.Skin;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;

public abstract class SkinNPC extends AbstractPitNPC {

    @Override
    public void initSkin(NPC npc) {
        if (!npc.hasTrait(SkinTrait.class)) {
            npc.addTrait(SkinTrait.class);
        }
        SkinTrait skinTrait = npc.getTrait(SkinTrait.class);
        Skin npcSkin = getNpcSkin();

        if (npcSkin == null || npcSkin.value() == null || npcSkin.signature() == null) {
            ThePit.getInstance().info(String.format("Npc %s,未设置皮肤！", this.getNpcInternalName()));

            return;
        }
        skinTrait.setSkinPersistent("custom_skin_name", npcSkin.signature(), npcSkin.value());
    }
}
