package top.mcfpp.mod.debugger.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.util.Identifier;
import top.mcfpp.mod.debugger.gui.EditScreen;


import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class EditCommand {
    public static final SuggestionProvider<FabricClientCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
        //CommandSource.suggestIdentifiers(commandFunctionManager.getFunctionTags(), builder, "#");
        return CommandSource.suggestIdentifiers(FunctionTextLoader.functionIds(), builder);
    };
    public static void onInitialize(){

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("edit")
                        .requires(source->source.hasPermissionLevel(2))
                        .executes(context -> {
                            EditScreen.setOpen(true);
                            return 1;
                        })
                    .then(argument("name", IdentifierArgumentType.identifier())
                        .suggests(SUGGESTION_PROVIDER)
                        .executes(context -> {
                            context.getSource().getClient().executeSync(()->{
                                Identifier identifier = context.getArgument("name",Identifier.class);
                                EditScreen.setFunction(identifier);
                                EditScreen.setOpen(true);
                            });
                            return 1;
                        }))
            );

        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(EditScreen.isOpen()){
                client.setScreen(new EditScreen());
                EditScreen.setOpen(false);
            }
        });
    }
}
