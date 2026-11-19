package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.npc.AbstractPitNPC;
import org.bukkit.entity.EntityType;

/**
 * 专用于设置实体类型的NPC基类
 */
public abstract class TypeNPC extends AbstractPitNPC {
    @Override
    public abstract EntityType getEntityType();
}