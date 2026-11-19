package cn.charlotte.pit.util.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An Adventure‑based replacement for the old Bungee {@code ComponentBuilder}.
 * <p>
 * Supports legacy colour codes (both § and &), click / hover events and basic
 * per‑part manipulation, while avoiding reflection and the deprecated Bungee
 * classes. Works natively on 1.16+ servers such as Paper‑1.20.6.
 */
public class ChatComponentBuilder {

    private static final Pattern LEGACY_PATTERN = Pattern.compile("[§&][0-9a-fk-orA-FK-OR]");

    private final List<Component> parts = new ArrayList<>();
    private TextComponent.Builder current = Component.text();

    public ChatComponentBuilder(String text) {
        parse(text);
    }

    /**
     * 当前正在编辑的组件片段。
     */
    public TextComponent.Builder current() {
        return current;
    }

    public void current(TextComponent.Builder builder) {
        this.current = builder;
    }

    public List<Component> parts() {
        return parts;
    }

    /* --------------------------------------------------- */
    /*               事件（Click / Hover）支持               */
    /* --------------------------------------------------- */

    public ChatComponentBuilder setCurrentHoverEvent(HoverEvent<?> hoverEvent) {
        this.current.hoverEvent(hoverEvent);
        return this;
    }

    public ChatComponentBuilder setCurrentClickEvent(ClickEvent clickEvent) {
        this.current.clickEvent(clickEvent);
        return this;
    }

    public ChatComponentBuilder attachToEachPart(HoverEvent<?> hoverEvent) {
        for (int i = 0; i < parts.size(); i++) {
            Component c = parts.get(i);
            if (c.hoverEvent() == null) {
                parts.set(i, c.hoverEvent(hoverEvent));
            }
        }
        setCurrentHoverEvent(hoverEvent);
        return this;
    }

    public ChatComponentBuilder attachToEachPart(ClickEvent clickEvent) {
        for (int i = 0; i < parts.size(); i++) {
            Component c = parts.get(i);
            if (c.clickEvent() == null) {
                parts.set(i, c.clickEvent(clickEvent));
            }
        }
        setCurrentClickEvent(clickEvent);
        return this;
    }

    /* --------------------------------------------------- */
    /*                        解析                         */
    /* --------------------------------------------------- */

    public ChatComponentBuilder parse(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }

        // 将 § 统一替换为 &，便于统一使用 Adventure 的 legacyAmpersand() 解析器
        String unified = text.replace('§', '&');

        // 如果字符串里没有颜色代码，直接把内容写进 current
        if (!LEGACY_PATTERN.matcher(unified).find()) {
            this.current.content(unified);
            return this;
        }

        // 使用 Adventure 的反序列化器一次性分解所有格式代码
        Component deserialized = LegacyComponentSerializer.legacyAmpersand().deserialize(unified);

        List<Component> flat = new ArrayList<>();
        flatten(deserialized, flat);

        if (flat.isEmpty()) {
            return this;
        }

        // 把最后一段留给 current，其余段加入 parts，保持和旧实现相同的语义
        if (flat.size() == 1) {
            this.current = ((TextComponent) flat.get(0)).toBuilder();
        } else {
            for (int i = 0; i < flat.size() - 1; i++) {
                parts.add(flat.get(i));
            }
            this.current = ((TextComponent) flat.get(flat.size() - 1)).toBuilder();
        }
        return this;
    }

    /**
     * 深度遍历，把所有 {@link TextComponent} 叶节点扁平化收集到列表中。
     */
    private void flatten(Component component, List<Component> collector) {
        if (component instanceof TextComponent tc && !tc.content().isEmpty()) {
            collector.add(component);
        }
        for (Component child : component.children()) {
            flatten(child, collector);
        }
    }

    /* --------------------------------------------------- */
    /*                        追加                         */
    /* --------------------------------------------------- */

    public ChatComponentBuilder append(Component component) {
        if (component == null) return this;
        parts.add(component);
        return this;
    }

    public ChatComponentBuilder append(String component) {
        if (component == null) return this;
        parts.add(Component.text(component));
        return this;
    }

    public ChatComponentBuilder append(TextComponent.Builder builder) {
        if (builder == null) return this;
        parts.add(builder.build());
        return this;
    }

    /* --------------------------------------------------- */
    /*                        构建                         */
    /* --------------------------------------------------- */

    /**
     * 构建最终的 {@link Component}，可直接用 {@code Audience#sendMessage(Component)} 发送。
     */
    public Component build() {
        List<Component> all = new ArrayList<>(parts);
        all.add(current.build());
        return Component.join(Component.empty(), all);
    }
}
