package cn.charlotte.pit.perk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A reviewed PlayerOnly combat hook which can also safely receive a managed Bot. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BotCombatEffect { }
