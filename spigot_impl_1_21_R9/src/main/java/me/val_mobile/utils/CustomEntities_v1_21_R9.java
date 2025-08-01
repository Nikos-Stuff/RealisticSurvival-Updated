/*
    Copyright (C) 2025  Val_Mobile

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.val_mobile.utils;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Guardian;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;

public enum CustomEntities_v1_21_R9 {

    ENDERMAN_ALLY("enderman_ally", -1, EntityType.ENDERMAN, EnderMan.class, EndermanAlly_v1_21_R9.class),
    FIRE_DRAGON("fire_dragon", -1, EntityType.ENDER_DRAGON, EnderDragon.class, FireDragon_v1_21_R9.class),
    ICE_DRAGON("ice_dragon", -1, EntityType.ENDER_DRAGON, EnderDragon.class, IceDragon_v1_21_R9.class),
    LIGHTNING_DRAGON("lightning_dragon", -1, EntityType.ENDER_DRAGON, EnderDragon.class, LightningDragon_v1_21_R9.class),
    SEA_SERPENT("sea_serpent", -1, EntityType.ELDER_GUARDIAN, ElderGuardian.class, SeaSerpent_v1_21_R9.class),
    SIREN("siren", -1, EntityType.GUARDIAN, Guardian.class, Siren_v1_21_R9.class);

    private final String name;
    private final int id;
    private final EntityType<?> entityType;
    private final ResourceLocation minecraftKey;
    private final Class<? extends Mob> nmsClass;
    private final Class<? extends Entity> customClass;

    CustomEntities_v1_21_R9(String name, int id, EntityType<?> entityType, Class<? extends Mob> nmsClass,
                            Class<? extends Entity> customClass) {
        this.name = name;
        this.id = id;
        this.entityType = entityType;
        this.minecraftKey = ResourceLocation.tryParse(name);
        this.nmsClass = nmsClass;
        this.customClass = customClass;

    }

        public static void registerEntities() {
            Map<String, Type<?>> types = (Map<String, Type<?>>) DataFixers.getDataFixer() // Testing the lat datafixer
    .getSchema(DataFixUtils.makeKey(SharedConstants.getCurrentVersion().dataVersion().version()))
    .findChoiceType(References.ENTITY)
    .types();

            unfreezeRegistry();
            registerEntity("fire_dragon", FireDragon_v1_21_R9::new, types);
            registerEntity("ice_dragon", IceDragon_v1_21_R9::new, types);
            registerEntity("lightning_dragon", LightningDragon_v1_21_R9::new, types);
            registerEntity("enderman_ally", EndermanAlly_v1_21_R9::new, types);
            registerEntity("sea_serpent", SeaSerpent_v1_21_R9::new, types);
            registerEntity("siren", Siren_v1_21_R9::new, types);
            BuiltInRegistries.ENTITY_TYPE.freeze();
        }

    private static void registerEntity(String type, EntityType.EntityFactory customMob, Map<String, Type<?>> types) {
        if (BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(type)).isEmpty()) {
            String customName = "minecraft:realisticsurvival_" + type;
            types.put(customName, types.get("minecraft:" + type));
            EntityType.Builder<Entity> a = EntityType.Builder.of(customMob, MobCategory.MONSTER);
            ResourceLocation resourceLoc = ResourceLocation.parse(customName);
            Registry.register(BuiltInRegistries.ENTITY_TYPE, customName, a.build(ResourceKey.create(Registries.ENTITY_TYPE, resourceLoc)));
        }
    }

    private static void unfreezeRegistry() {
        Class<MappedRegistry> registryClass = MappedRegistry.class;
        try {
            Field intrusiveHolderCache = registryClass.getDeclaredField(ObfuscatedFields_v1_21_R9.INTRUSIVE_HOLDER_CACHE_SYMBOL);
            intrusiveHolderCache.setAccessible(true);
            intrusiveHolderCache.set(BuiltInRegistries.ENTITY_TYPE, new IdentityHashMap<EntityType<?>, Holder.Reference<EntityType<?>>>());
            Field frozen = registryClass.getDeclaredField(ObfuscatedFields_v1_21_R9.FROZEN_SYMBOL);
            frozen.setAccessible(true);
            frozen.set(BuiltInRegistries.ENTITY_TYPE, false);

            // Dynamically locate the TagSet interface within MappedRegistry
            Class<?> tagSetInterface = null;
            for (Class<?> nestedClass : MappedRegistry.class.getDeclaredClasses()) {
                if (nestedClass.getName().equals(ObfuscatedFields_v1_21_R9.TAGSET_SYMBOL_OBFUSCATED) || nestedClass.getName().equals(ObfuscatedFields_v1_21_R9.TAGSET_SYMBOL_UNOBFUSCATED)) {
                    tagSetInterface = nestedClass;
                    break;
                }
            }

            // Locate the 'unbound' method within TagSet
            Method unboundMethod = null;
            for (Method method : tagSetInterface.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
                    unboundMethod = method;
                    unboundMethod.setAccessible(true);
                    break;
                }
            }

            // Retrieve the unbound TagSet instance
            Object unboundTagSet = unboundMethod.invoke(null);

            Field allTags = registryClass.getDeclaredField(ObfuscatedFields_v1_21_R9.ALL_TAGS_SYMBOL);
            allTags.setAccessible(true);
            // Set the unbound TagSet to the registry's allTags field
            allTags.set(BuiltInRegistries.ENTITY_TYPE, unboundTagSet);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException |
        InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void unregisterEntities() {}

    public static Object getPrivateField(Class<?> clazz, Object handle, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(handle);
    }

    public String getName() {
        return name;
    }

    public int getID() {
        return id;
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public ResourceLocation getMinecraftKey() {
        return this.minecraftKey;
    }

    public Class<? extends Mob> getNMSClass() {
        return nmsClass;
    }

    public Class<? extends Entity> getCustomClass() {
        return customClass;
    }

}
