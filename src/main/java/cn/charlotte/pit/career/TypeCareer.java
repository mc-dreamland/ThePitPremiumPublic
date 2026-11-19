package cn.charlotte.pit.career;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.career.impl.SwordsMan;

public enum TypeCareer {
    SWORDSMAN(SwordsMan.class);

    TypeCareer(Class<?> Clazz) {
        if (!AbstractCareer.class.isAssignableFrom(Clazz)) {
            ThePit.getInstance().getLogger().warning(String.format("该%s尚未继承AbstractCareer", Clazz.getName()));
            return;
        }
        Class<? extends AbstractCareer> career = Clazz.asSubclass(AbstractCareer.class);

    }
}
