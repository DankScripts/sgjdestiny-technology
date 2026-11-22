package com.dankscripts.sgjdestiny_dhd.item;

import java.util.Map;
import java.util.Optional;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public final class KinoBiomeScanTest {
    public static void main(String[] args) {
        Language original = Language.getInstance();
        try {
            Language.inject(language(Map.of("biome.minecraft.plains", "Plains",
                    "biome.example.highlands.meadow", "Highland Meadow")));
            var plains = KinoBiomeScan.message(Optional.of(ResourceLocation.tryParse("minecraft:plains")));
            check(plains.getString().equals("BIOME: Plains"), "vanilla biome language entry");
            check(KinoBiomeScan.message(Optional.of(ResourceLocation.tryParse("example:highlands/meadow")))
                    .getString().equals("BIOME: Highland Meadow"), "namespaced nested biome language entry");
            Language.inject(language(Map.of("biome.minecraft.plains", "Ebenen")));
            check(plains.getString().equals("BIOME: Ebenen"), "same message resolves in the client's language");
            check(KinoBiomeScan.message(Optional.empty()).getString().equals("BIOME: unknown"),
                    "unregistered biome preserves unknown label");
        } finally {
            Language.inject(original);
        }
        System.out.println("PASS: Kino biome labels use client language entries");
    }

    private static Language language(Map<String, String> entries) {
        return new Language() {
            @Override public String getOrDefault(String key, String fallback) { return entries.getOrDefault(key, fallback); }
            @Override public boolean has(String key) { return entries.containsKey(key); }
            @Override public boolean isDefaultRightToLeft() { return false; }
            @Override public FormattedCharSequence getVisualOrder(FormattedText text) { return FormattedCharSequence.EMPTY; }
        };
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
