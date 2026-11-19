package cn.charlotte.pit.activity;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.genes.GenesListener;
import cn.charlotte.pit.activity.genes.menu.GenesMenu;
import cn.charlotte.pit.activity.newyear.NewYearMain;
import cn.charlotte.pit.util.menu.Menu;
import lombok.Getter;

@Getter
public enum ActivityType {
    NULL(0.0, null, null,null,null),
    NEW_YEAR(1.7, "&e新年", NewYearMain.class,null,null),
    GENES(1.8,"&b天使&c恶魔",GenesListener.class,1774936800000L,new GenesMenu());

    private final Double decimal;
    private final String name;
    private final Class<?> clazz;
    private final Long endTime;
    private final Menu menu;

    ActivityType(Double decimal, String name, Class<?> clazz, Long endTime,Menu menu) {
        this.decimal = decimal;
        this.name = name;
        this.clazz = clazz;
        this.endTime = endTime;
        this.menu = menu;
    }

    public boolean isActive() {
        ActivityType type = ThePit.getInstance().getActivityFactory().getFactoryType();
        if (type == null) return false;
        return this.equals(type);
    }

//    public boolean isActive() {
//        return this.clazz == ThePit.getInstance().getActivityFactory().getFactoryType().getClazz();
//    }
}
