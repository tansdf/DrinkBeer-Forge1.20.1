package lekavar.lma.drinkbeer.blockentities;

import lekavar.lma.drinkbeer.gui.BeerBarrelContainer;
import lekavar.lma.drinkbeer.recipes.BrewingRecipe;
import lekavar.lma.drinkbeer.recipes.IBrewingInventory;
import lekavar.lma.drinkbeer.registries.BlockEntityRegistry;
import lekavar.lma.drinkbeer.registries.RecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BeerBarrelBlockEntity extends BlockEntity implements MenuProvider, IBrewingInventory {
    private static final int INGREDIENT_SLOT_START = 0;
    private static final int INGREDIENT_SLOT_END = 3;
    private static final int CUP_SLOT = 4;
    private static final int OUTPUT_SLOT = 5;
    private static final int SLOT_COUNT = 6;
    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot != OUTPUT_SLOT && (slot != CUP_SLOT || isEmptyCup(stack));
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot >= INGREDIENT_SLOT_START && slot <= INGREDIENT_SLOT_END) {
                return 1;
            }
            return super.getSlotLimit(slot);
        }
    };
    private final IItemHandler automationHandler = new AutomationItemHandler();
    private final LazyOptional<IItemHandler> automationHandlerOptional = LazyOptional.of(() -> automationHandler);
    // This int will not only indicate remainingBrewTime, but also represent Standard Brewing Time if valid in "waiting for ingredients" stage
    private int remainingBrewTime;
    // 0 - waiting for ingredient, 1 - brewing, 2 - waiting for pickup product
    private int statusCode;
    public final ContainerData syncData = new ContainerData() {
        @Override
        public int get(int p_221476_1_) {
            switch (p_221476_1_) {
                case 0:
                    return remainingBrewTime;
                case 1:
                    return statusCode;
                default:
                    return 0;
            }
        }

        @Override
        public void set(int p_221477_1_, int p_221477_2_) {
            switch (p_221477_1_) {
                case 0:
                    remainingBrewTime = p_221477_2_;
                    break;
                case 1:
                    statusCode = p_221477_2_;
                    break;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public BeerBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BEER_BARREL_TILEENTITY.get(), pos, state);
    }

    public void tickServer() {
        // waiting for ingredient
        if (statusCode == 0) {
            // ingredient slots must have no empty slot
            if (!getIngredients().contains(ItemStack.EMPTY)) {
                // Try match Recipe
                BrewingRecipe recipe = level.getRecipeManager().getRecipeFor(RecipeRegistry.BREWING_TYPE.get(), this, this.level).orElse(null);
                if (canBrew(recipe)) {
                    // Show Standard Brewing Time & Result
                    showPreview(recipe);
                    // Check Weather have enough cup.
                    if (hasEnoughEmptyCap(recipe)) {
                        startBrewing(recipe);

                    }
                }
                // Time remainingBrewTime will be reset since it also represents Standard Brewing Time if valid in this stage
                else {
                    clearPreview();
                }
            } else {
                clearPreview();
            }
        }
        // brewing
        else if (statusCode == 1) {
            if (remainingBrewTime > 0) {
                remainingBrewTime--;
            }
            // Enter "waiting for pickup"
            else {
                // Prevent wired glitch such as remainingTime been set to one;
                remainingBrewTime = 0;
                // Enter Next Stage
                statusCode = 2;
            }
            setChanged();
        }
        // waiting for pickup
        else if (statusCode == 2) {
            // Reset Stage to 0 (waiting for ingredients) after pickup Item
            if (items.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
                statusCode = 0;
                setChanged();
            }
        }
        // Error status reset
        else {
            remainingBrewTime = 0;
            statusCode = 0;
            setChanged();
        }
    }


    private boolean canBrew(@Nullable BrewingRecipe recipe) {
        if (recipe != null) {
            return recipe.matches(this, this.level);
        } else {
            return false;
        }
    }

    private boolean hasEnoughEmptyCap(BrewingRecipe recipe) {
        return recipe.isCupQualified(this);
    }

    private void startBrewing(BrewingRecipe recipe) {
        // Pre-set bear to output Slot
        // This Step must be done first
        items.setStackInSlot(OUTPUT_SLOT, recipe.assemble(this, level.registryAccess()));
        // Consume Ingredient & Cup;
        for (int i = INGREDIENT_SLOT_START; i <= INGREDIENT_SLOT_END; i++) {
            ItemStack ingred = items.getStackInSlot(i);
            if (isBucket(ingred)) items.setStackInSlot(i, Items.BUCKET.getDefaultInstance());
            else ingred.shrink(1);
        }
        items.getStackInSlot(CUP_SLOT).shrink(recipe.getRequiredCupCount());
        // Set Remaining Time;
        remainingBrewTime = recipe.getBrewingTime();
        // Change Status Code to 1 (brewing)
        statusCode = 1;

        setChanged();
    }

    private boolean isBucket(ItemStack itemStack) {
        return itemStack.getItem() instanceof BucketItem;
    }

    private void clearPreview() {
        items.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        remainingBrewTime = 0;
        setChanged();
    }

    private void showPreview(BrewingRecipe recipe) {
        items.setStackInSlot(OUTPUT_SLOT, recipe.assemble(this, level.registryAccess()));
        remainingBrewTime = recipe.getBrewingTime();
        setChanged();
    }


    @Nonnull
    @Override
    public List<ItemStack> getIngredients() {
        NonNullList<ItemStack> sample = NonNullList.withSize(4, ItemStack.EMPTY);
        for (int i = INGREDIENT_SLOT_START; i <= INGREDIENT_SLOT_END; i++) {
            sample.set(i, items.getStackInSlot(i).copy());
        }
        return sample;
    }

    @Nonnull
    @Override
    public ItemStack getCup() {
        return items.getStackInSlot(CUP_SLOT).copy();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", this.items.serializeNBT());
        tag.putShort("RemainingBrewTime", (short) this.remainingBrewTime);
        tag.putShort("statusCode", (short) this.statusCode);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            this.items.deserializeNBT(tag.getCompound("Inventory"));
        } else {
            NonNullList<ItemStack> legacyItems = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, legacyItems);
            for (int i = 0; i < legacyItems.size(); i++) {
                this.items.setStackInSlot(i, legacyItems.get(i));
            }
        }
        this.remainingBrewTime = tag.getShort("RemainingBrewTime");
        this.statusCode = tag.getShort("statusCode");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.drinkbeer.beer_barrel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BeerBarrelContainer(id, this, syncData, inventory, this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Inventory", this.items.serializeNBT());
        tag.putShort("RemainingBrewTime", (short) this.remainingBrewTime);
        tag.putShort("statusCode", (short) this.statusCode);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag.contains("Inventory")) {
            this.items.deserializeNBT(tag.getCompound("Inventory"));
        }
        this.remainingBrewTime = tag.getShort("RemainingBrewTime");
        this.statusCode = tag.getShort("statusCode");
    }

    @Override
    public int getContainerSize() {
        return items.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.items.getSlots(); i++) {
            if (!this.items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int p_70301_1_) {
        return p_70301_1_ >= 0 && p_70301_1_ < this.items.getSlots() ? this.items.getStackInSlot(p_70301_1_) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int p_70298_1_, int p_70298_2_) {
        if (p_70298_1_ < 0 || p_70298_1_ >= this.items.getSlots() || p_70298_2_ <= 0) {
            return ItemStack.EMPTY;
        }
        return this.items.extractItem(p_70298_1_, p_70298_2_, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_70304_1_) {
        if (p_70304_1_ < 0 || p_70304_1_ >= this.items.getSlots()) {
            return ItemStack.EMPTY;
        }
        ItemStack itemStack = this.items.getStackInSlot(p_70304_1_);
        this.items.setStackInSlot(p_70304_1_, ItemStack.EMPTY);
        return itemStack;
    }

    @Override
    public void setItem(int p_70299_1_, ItemStack p_70299_2_) {
        if (p_70299_1_ >= 0 && p_70299_1_ < this.items.getSlots()) {
            if (p_70299_1_ >= INGREDIENT_SLOT_START && p_70299_1_ <= INGREDIENT_SLOT_END) {
                p_70299_2_.setCount(Math.min(p_70299_2_.getCount(), 1));
            }
            this.items.setStackInSlot(p_70299_1_, p_70299_2_);
        }
    }

    @Override
    public int getMaxStackSize() {
        return IBrewingInventory.super.getMaxStackSize();
    }

    @Override
    public boolean stillValid(Player p_70300_1_) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return !(p_70300_1_.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) > 64.0D);
        }
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.items.getSlots(); i++) {
            this.items.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public boolean isOutputReady() {
        return statusCode == 2;
    }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int i = 0; i < this.items.getSlots(); i++) {
            if (i == OUTPUT_SLOT && !isOutputReady()) {
                continue;
            }
            ItemStack itemStack = this.items.getStackInSlot(i);
            if (!itemStack.isEmpty()) {
                drops.add(itemStack.copy());
            }
        }
        return drops;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return automationHandlerOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        automationHandlerOptional.invalidate();
    }

    private boolean isEmptyCup(ItemStack stack) {
        return stack.is(lekavar.lma.drinkbeer.registries.ItemRegistry.EMPTY_BEER_MUG.get());
    }

    private boolean canAutomationInsert(int slot, ItemStack stack) {
        if (statusCode != 0 || stack.isEmpty()) {
            return false;
        }
        if (slot >= INGREDIENT_SLOT_START && slot <= INGREDIENT_SLOT_END) {
            return true;
        }
        return slot == CUP_SLOT && isEmptyCup(stack);
    }

    private boolean canAutomationExtract(int slot) {
        if (slot >= INGREDIENT_SLOT_START && slot <= INGREDIENT_SLOT_END) {
            return this.items.getStackInSlot(slot).is(Items.BUCKET);
        }
        return slot == OUTPUT_SLOT && isOutputReady();
    }

    private class AutomationItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (!canAutomationInsert(slot, stack)) {
                return stack;
            }
            return items.insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canAutomationExtract(slot)) {
                return ItemStack.EMPTY;
            }
            return items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return canAutomationInsert(slot, stack);
        }
    }
}
