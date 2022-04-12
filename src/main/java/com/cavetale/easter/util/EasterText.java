package com.cavetale.easter.util;

import com.cavetale.mytems.item.easter.EasterEggColor;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;

public final class EasterText {
    private EasterText() { }

    public static Component easterify(String in) {
        EasterEggColor[] colors = EasterEggColor.values();
        List<Component> components = new ArrayList<>();
        for (int i = 0; i < in.length(); i += 1) {
            char c = in.charAt(i);
            if (c == ' ') {
                components.add(space());
            } else {
                components.add(text(c, colors[i % colors.length].textColor));
            }
        }
        return join(noSeparators(), components);
    }
}
