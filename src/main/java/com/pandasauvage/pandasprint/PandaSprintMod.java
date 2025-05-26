package com.pandasauvage.pandasprint;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

@Mod(
  modid = "pandasprint",
  name = "PandaSprint",
  version = "1.0",
  clientSideOnly = true
)
public class PandaSprintMod {

  private static final Minecraft mc = Minecraft.getMinecraft();
  private static KeyBinding toggleSprintKey;
  private static KeyBinding editModeKey;
  private static boolean toggled = false;
  private static boolean editMode = false;
  private static boolean dragging = false;
  private static int dragOffsetX = 0;
  private static int dragOffsetY = 0;
  private static Configuration config;

  private static int hudX;
  private static int hudY;
  private static float hudScale;

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    toggleSprintKey =
      new KeyBinding(
        "key.pandasprint.toggle",
        Keyboard.KEY_R,
        "key.categories.pandasprint"
      );
    editModeKey =
      new KeyBinding(
        "key.pandasprint.edit",
        Keyboard.KEY_P,
        "key.categories.pandasprint"
      );
    ClientRegistry.registerKeyBinding(toggleSprintKey);
    ClientRegistry.registerKeyBinding(editModeKey);

    File configFile = new File(mc.mcDataDir, "pandasprint.cfg");
    config = new Configuration(configFile);
    config.load();
    toggled = config.get("general", "sprintToggled", false).getBoolean();
    hudX = config.get("hud", "x", 5).getInt();
    hudY = config.get("hud", "y", 5).getInt();
    hudScale = (float) config.get("hud", "scale", 1.0).getDouble();
    config.save();

    MinecraftForge.EVENT_BUS.register(this);
  }

  @SubscribeEvent
  public void onKeyInput(KeyInputEvent event) {
    if (toggleSprintKey.isPressed()) {
      toggled = !toggled;
      config.get("general", "sprintToggled", false).set(toggled);
      config.save();
    }

    if (editModeKey.isPressed()) {
      editMode = !editMode;
    }
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (mc.thePlayer != null && mc.theWorld != null && toggled) {
      // Vérifier si le joueur se déplace horizontalement
      boolean isMoving = mc.thePlayer.moveForward > 0;

      // N'activer le sprint que si le joueur se déplace
      if (isMoving) {
        mc.thePlayer.setSprinting(true);
      }
    }
  }

  @SubscribeEvent
  public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
    ScaledResolution scaled = new ScaledResolution(mc);
    String display = toggled ? "§aSprinting (Toggled)" : "§cSprinting (Off)";
    int textWidth = mc.fontRendererObj.getStringWidth(display);
    int textHeight = mc.fontRendererObj.FONT_HEIGHT;

    GL11.glPushMatrix();
    GL11.glScalef(hudScale, hudScale, 1.0F);
    mc.fontRendererObj.drawStringWithShadow(
      display,
      hudX / hudScale,
      hudY / hudScale,
      0xFFFFFF
    );
    GL11.glPopMatrix();

    if (editMode) {
      int dWheel = Mouse.getDWheel();
      if (dWheel != 0) {
        hudScale = Math.max(0.5F, hudScale + (dWheel > 0 ? 0.1F : -0.1F));
        config.get("hud", "scale", 1.0).set(hudScale);
        config.save();
      }
    }

    int mouseX = Mouse.getX() * scaled.getScaledWidth() / mc.displayWidth;
    int mouseY =
      scaled.getScaledHeight() -
      Mouse.getY() *
      scaled.getScaledHeight() /
      mc.displayHeight;

    if (editMode) {
      if (Mouse.isButtonDown(0)) {
        if (
          !dragging &&
          mouseX >= hudX &&
          mouseX <= hudX + textWidth &&
          mouseY >= hudY &&
          mouseY <= hudY + textHeight
        ) {
          dragging = true;
          dragOffsetX = mouseX - hudX;
          dragOffsetY = mouseY - hudY;
        }
        if (dragging) {
          hudX = mouseX - dragOffsetX;
          hudY = mouseY - dragOffsetY;
        }
      } else if (dragging) {
        dragging = false;
        config.get("hud", "x", 5).set(hudX);
        config.get("hud", "y", 5).set(hudY);
        config.save();
      }

      drawRect(
        hudX - 2,
        hudY - 2,
        hudX + textWidth + 2,
        hudY + textHeight + 2,
        0x90000000
      );
    }
  }

  private void drawRect(int left, int top, int right, int bottom, int color) {
    int j1;

    if (left < right) {
      j1 = left;
      left = right;
      right = j1;
    }

    if (top < bottom) {
      j1 = top;
      top = bottom;
      bottom = j1;
    }

    float a = (float) (color >> 24 & 255) / 255.0F;
    float r = (float) (color >> 16 & 255) / 255.0F;
    float g = (float) (color >> 8 & 255) / 255.0F;
    float b = (float) (color & 255) / 255.0F;

    GL11.glEnable(GL11.GL_BLEND);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glColor4f(r, g, b, a);
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2f(right, top);
    GL11.glVertex2f(left, top);
    GL11.glVertex2f(left, bottom);
    GL11.glVertex2f(right, bottom);
    GL11.glEnd();
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }
}
