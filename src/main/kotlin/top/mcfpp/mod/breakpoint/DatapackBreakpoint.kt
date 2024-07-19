package top.mcfpp.mod.breakpoint

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory
import top.mcfpp.mod.breakpoint.command.BreakPointCommand

object DatapackBreakpoint : ModInitializer {
    val logger = LoggerFactory.getLogger("datapack-breakpoint")

	override fun onInitialize() {
		BreakPointCommand.onInitialize()
	}
}