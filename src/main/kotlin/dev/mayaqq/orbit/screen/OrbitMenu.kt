package dev.mayaqq.orbit.screen

import dev.mayaqq.orbit.Orbit
import dev.mayaqq.orbit.config.OrbitConfig
import dev.mayaqq.orbit.data.OrbitButton
import dev.mayaqq.orbit.utils.McClient
import dev.mayaqq.orbit.utils.Text
import earth.terrarium.olympus.client.components.Widgets
import earth.terrarium.olympus.client.components.buttons.Button
import earth.terrarium.olympus.client.components.renderers.WidgetRenderers
import earth.terrarium.olympus.client.ui.UIConstants
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.*

class OrbitMenu : ControlsPassthroughScreen(Text.EMPTY) {

    var selectedButton: OrbitButton? = null

    val buttonWidgets: List<Button> = List(Orbit.buttons.size) { Widgets.button() }

    override fun init() {
        buttonWidgets.forEachIndexed { index, button ->
            button.setSize(40, 40)
            button.withTexture(UIConstants.BUTTON)
            button.withRenderer(
                WidgetRenderers.layered(
                    WidgetRenderers.sprite(UIConstants.BUTTON),
                    WidgetRenderers.center(40, 40) { gr, ctx, _ ->
                        val item = Orbit.buttons[index].item()
                        gr.renderItem(item, ctx.x + 12, ctx.y + 12)
                    }
                ))

            val angle = (index * (360.0 / buttonWidgets.size)) - 90.0
            val radius = 100
            val centerX = width / 2.0
            val centerY = height / 2.0
            val x = centerX + radius * cos(Math.toRadians(angle)) - 20.0
            val y = centerY + radius * sin(Math.toRadians(angle)) - 20.0
            button.setPosition(x.roundToInt(), y.roundToInt())

            button.withCallback {
                val button = Orbit.buttons[index]
                if (hasShiftDown()) {
                    McClient.tell { McClient.setScreen(ConfigurationScreen(button)) }
                } else {
                    button.execute()
                    onClose()
                }
            }


            button.withShape { mouseX, mouseY, width, height ->
                val dx = mouseX + x - centerX
                val dy = mouseY + y - centerY

                val distance = sqrt(dx * dx + dy * dy)
                val innerRadius = 10
                val outerRadius = 200

                if (distance.toInt() !in innerRadius..outerRadius) return@withShape false

                val angle = (atan2(dy, dx) + 2 * PI) % (2 * PI) + (PI / 2)

                val correctedAngle = (angle + (PI / buttonWidgets.size)) % (2 * PI)

                val segmentIndex = (correctedAngle / (2 * PI) * buttonWidgets.size).toInt()

                segmentIndex == index
            }


            button.visitWidgets(this::addRenderableWidget)
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, f: Float) {
        super.render(graphics, mouseX, mouseY, f)
        val centerX = width / 2
        val centerY = height / 2

        var anySelected = false
        buttonWidgets.forEachIndexed { index, button ->
            if (button.isHoveredOrFocused) {
                anySelected = true
                selectedButton = Orbit.buttons[index]
            }
        }
        if (!anySelected) selectedButton = null

        selectedButton?.let {
            graphics.drawCenteredString(McClient.font, Text.trans(it.actionString).string, centerX, centerY, 0xFFFFFF)
        }
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == Orbit.ORBIT.key.value) {
            buttonWidgets.forEachIndexed { index, button ->
                if (button.isHoveredOrFocused) {
                    Orbit.buttons[index].execute()
                }
            }
            onClose()
        }
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        McClient.options.keyHotbarSlots.mapIndexed { index, mapping ->
            if (mapping.key.value == keyCode) {
                Orbit.buttons[index].execute()
                onClose()
                return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun isPauseScreen(): Boolean = false

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}
}