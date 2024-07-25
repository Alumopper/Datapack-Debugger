package top.mcfpp.mod.debugger.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import top.mcfpp.mod.debugger.utils.Debugger;

import java.util.Objects;

public class Stack extends Item {

    public Stack(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return TypedActionResult.pass(user.getStackInHand(hand));
        }
        Debugger.printStack(user, Objects.requireNonNull(user.getServer()));
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
