package lekavar.lma.drinkbeer.utils;

import lekavar.lma.drinkbeer.blocks.BeerBarrelBlock;
import lekavar.lma.drinkbeer.blocks.BeerMugBlock;
import lekavar.lma.drinkbeer.blocks.CallBellBlock;
import lekavar.lma.drinkbeer.blocks.RecipeBoardBlock;
import lekavar.lma.drinkbeer.blocks.RecipeBoardPackageBlock;
import lekavar.lma.drinkbeer.registries.BlockRegistry;
import lekavar.lma.drinkbeer.registries.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "drinkbeer");

    public static final RegistryObject<CreativeModeTab> BEER = CREATIVE_MODE_TABS.register("beer", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.drinkbeer.beer"))
            .icon(() -> new ItemStack(ItemRegistry.BEER_MUG.get()))
            .displayItems((parameters, output) -> beerItems().forEach(output::accept))
            .build());

    public static final RegistryObject<CreativeModeTab> GENERAL = CREATIVE_MODE_TABS.register("general", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.drinkbeer.general"))
            .icon(() -> new ItemStack(ItemRegistry.BEER_BARREL.get()))
            .displayItems((parameters, output) -> generalItems()
                    .sorted(Comparator.comparingInt(ModCreativeTab::getIndexNumber))
                    .forEach(output::accept))
            .build());

    private static Stream<ItemStack> beerItems() {
        return Stream.of(
                ItemRegistry.BEER_MUG,
                ItemRegistry.BEER_MUG_BLAZE_STOUT,
                ItemRegistry.BEER_MUG_BLAZE_MILK_STOUT,
                ItemRegistry.BEER_MUG_APPLE_LAMBIC,
                ItemRegistry.BEER_MUG_SWEET_BERRY_KRIEK,
                ItemRegistry.BEER_MUG_HAARS_ICEY_PALE_LAGER,
                ItemRegistry.BEER_MUG_PUMPKIN_KVASS,
                ItemRegistry.BEER_MUG_NIGHT_HOWL_KVASS
        ).map(item -> new ItemStack(item.get()));
    }

    private static Stream<ItemStack> generalItems() {
        List<RegistryObject<? extends net.minecraft.world.item.Item>> items = List.of(
                ItemRegistry.BEER_BARREL,
                ItemRegistry.EMPTY_BEER_MUG,
                ItemRegistry.IRON_CALL_BELL,
                ItemRegistry.GOLDEN_CALL_BELL,
                ItemRegistry.RECIPE_BOARD_PACKAGE,
                ItemRegistry.RECIPE_BOARD_BEER_MUG,
                ItemRegistry.RECIPE_BOARD_BEER_MUG_BLAZE_STOUT,
                ItemRegistry.RECIPE_BOARD_BEER_MUG_BLAZE_MILK_STOUT,
                ItemRegistry.RECIPE_BOARD_BEER_MUG_APPLE_LAMBIC,
                ItemRegistry.RECIPE_BOARD_BEER_MUG_SWEET_BERRY_KRIEK,
                ItemRegistry.RECIPE_BOARD_BEER_MUG_HAARS_ICEY_PALE_LAGER,
                ItemRegistry.RECIPE_BOARD_BEER_MUG_PUMPKIN_KVASS,
                ItemRegistry.RECIPE_BOARD_BEER_MUG_NIGHT_HOWL_KVASS
        );
        return items.stream().map(item -> new ItemStack(item.get()));
    }

    private static int getIndexNumber(ItemStack itemStack) {
        if (itemStack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof BeerBarrelBlock) return 1;
            else if (block instanceof BeerMugBlock) return 2;
            else if (block instanceof CallBellBlock) return 3;
            else if (block instanceof RecipeBoardPackageBlock) return 4;
            else if (block instanceof RecipeBoardBlock) return 5;
        }
        return 9999;
    }
}
