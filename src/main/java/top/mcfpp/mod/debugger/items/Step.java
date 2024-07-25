package top.mcfpp.mod.debugger.items;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import top.mcfpp.mod.debugger.utils.Debugger;

import java.util.Objects;

public class Step extends Item {

    public Step(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return TypedActionResult.pass(user.getStackInHand(hand));
        }
        Debugger.step(user.isSneaking() ? 10 : 1, user, Objects.requireNonNull(user.getServer()));
        if (Debugger.isDebugging) {
            // switch
        }
        return TypedActionResult.success(user.getStackInHand(hand));

    }
}
