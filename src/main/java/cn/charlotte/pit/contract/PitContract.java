package cn.charlotte.pit.contract;

import cn.charlotte.pit.contract.impl.SuperStreak;
import lombok.Getter;

@Getter
public enum PitContract {

    SUPERSTREAK(SuperStreak.class);

    private final Abstractcontract instance;

    PitContract(Class<?> clazz) {
        Class<? extends Abstractcontract> subclass = clazz.asSubclass(Abstractcontract.class);
        try {
            var ctor = subclass.getDeclaredConstructor();
            if (!ctor.canAccess(null)) ctor.setAccessible(true);
            this.instance = ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("合约实例化失败", e);
        }
    }
}