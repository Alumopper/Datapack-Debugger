package top.mcfpp.mod.debugger.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import top.mcfpp.mod.debugger.utils.Debugger;

import java.util.Objects;

public class Continue extends Item {

    public Continue(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return TypedActionResult.pass(user.getStackInHand(hand));
        }
        Debugger.moveOn(user, Objects.requireNonNull(user.getServer()));
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
