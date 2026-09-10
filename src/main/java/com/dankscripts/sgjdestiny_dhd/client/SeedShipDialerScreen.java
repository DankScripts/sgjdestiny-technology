package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.network.DestinyDialerNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.sgjourney.common.sgjourney.Address;
import java.util.List;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.render.GuiPortalRendering;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

/** Destiny's seed-ship database, drawn directly into the console monitor artwork. */
public final class SeedShipDialerScreen extends Screen {
    private static final ResourceLocation FRAME = new ResourceLocation(DestinyDHD.MOD_ID, "textures/gui/seed_ship_dialer.png");
    private static final ResourceLocation HANDHELD_FRAME = new ResourceLocation(DestinyDHD.MOD_ID, "textures/gui/kino_remote_dialer.png");
    private static final int TEXTURE_WIDTH = 1536, TEXTURE_HEIGHT = 1024, PAGE_SIZE = 7;
    private final BlockPos console;
    private final boolean directGate;
    private final boolean handheld;
    private final List<DestinyDialerNetwork.ClientEntry> entries;
    private final int gateState;
    private int page, selected = -1;
    private boolean kinoSection;
    private boolean kinoDeployed;
    private int flightInputTick;
    private String videoDimension = "";
    private Vec3 videoCameraPos;
    private Vec3 videoCameraFrom;
    private Vec3 videoCameraTarget;
    private long videoCameraMoveStarted;
    private float videoYaw, videoPitch;
    private RenderTarget videoTarget;
    private boolean videoMode;
    private boolean videoCursorCaptured;
    private boolean ignoreNextMouseMove;
    private double lastVideoMouseX, lastVideoMouseY;
    private double pendingLookX, pendingLookY;

    public SeedShipDialerScreen(BlockPos console, boolean directGate, boolean handheld,
                                List<DestinyDialerNetwork.ClientEntry> entries, int gateState,
                                boolean kinoInitially, boolean kinoDeployed) {
        super(Component.translatable("screen.sgjdestiny_dhd.seed_ship_database"));
        this.console = console;
        this.directGate = directGate;
        this.handheld = handheld;
        this.entries = handheld ? entries.stream().filter(entry -> !entry.earth()).toList() : entries;
        this.gateState = gateState;
        this.kinoSection = kinoInitially;
        this.kinoDeployed = kinoDeployed;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout l = layout();
        if (handheld) {
            graphics.blit(HANDHELD_FRAME, l.left, l.top, l.width, l.height, 0, 0,
                    768, 512, 768, 512);
        } else {
            graphics.blit(FRAME, l.left, l.top, l.width, l.height, 0, 0,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        int center = l.screenLeft + l.screenWidth / 2;
        if (handheld) {
            int tabX = l.left + l.width * 81 / 100;
            graphics.drawCenteredString(font, "GATE", tabX + l.width * 5 / 100,
                    l.top + l.height * 27 / 100, kinoSection ? 0x718579 : 0xE5D29A);
            graphics.drawCenteredString(font, "KINO", tabX + l.width * 5 / 100,
                    l.top + l.height * 38 / 100, kinoSection ? 0xE5D29A : 0x718579);
        }
        if (handheld && kinoSection) {
            graphics.drawCenteredString(font, Component.translatable("screen.sgjdestiny_dhd.kino_control"),
                    center, l.screenTop + 15, 0xB9E1CC);
            graphics.drawCenteredString(font, Component.translatable("screen.sgjdestiny_dhd.kino_link_standby"),
                    center, l.screenTop + 38, 0x70A894);
            int panelLeft = l.rowsLeft + 12, panelRight = l.rowsLeft + l.rowsWidth - 12;
            int panelTop = l.rowsTop + 32, panelBottom = l.screenTop + l.screenHeight - 48;
            graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0x88203831);
            if (videoMode && videoCameraPos != null) {
                int vx = panelLeft + 5, vy = panelTop + 5;
                var worldKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(videoDimension));
                var remoteWorld = ClientWorldLoader.getOptionalWorld(worldKey);
                if (remoteWorld != null) {
                    int frameWidth = minecraft.getWindow().getWidth(), frameHeight = minecraft.getWindow().getHeight();
                    if (videoTarget == null) videoTarget = new TextureTarget(frameWidth, frameHeight, true, Minecraft.ON_OSX);
                    else if (videoTarget.width != frameWidth || videoTarget.height != frameHeight)
                        videoTarget.resize(frameWidth, frameHeight, Minecraft.ON_OSX);
                    Matrix4f camera = new Matrix4f().rotationX((float)Math.toRadians(-videoPitch))
                            .rotateY((float)Math.toRadians(videoYaw + 180.0F));
                    WorldRenderInfo info = new WorldRenderInfo.Builder().setWorld(remoteWorld)
                            .setCameraPos(interpolatedVideoCameraPos()).setCameraTransformation(camera)
                            .setOverwriteCameraTransformation(true).setDescription(null)
                            .setRenderDistance(Math.min(12, minecraft.options.getEffectiveRenderDistance()))
                            .setDoRenderHand(false).setEnableViewBobbing(false).setDoRenderSky(true).setHasFog(true).build();
                    double scale = minecraft.getWindow().getGuiScale();
                    MyRenderHelper.drawFramebuffer(videoTarget, false, false,
                            (float)(vx * scale), (panelRight - 5) * scale,
                            (float)((height - panelBottom + 5) * scale), (height - vy) * scale);
                    videoTarget.setClearColor(0, 0, 0, 1);
                    videoTarget.clear(Minecraft.ON_OSX);
                    GuiPortalRendering.submitNextFrameRendering(info, videoTarget);
                }
                graphics.fill(vx, vy, panelRight - 5, vy + 13, 0x99000000);
                graphics.drawString(font, "LIVE  " + videoDimension, vx + 4, vy + 3, 0xFFB8E4D2, false);
            }
            graphics.fill(panelLeft, panelTop, panelLeft + 2, panelBottom, 0xFF668A75);
            if (videoMode) {
                if (videoCameraPos == null) {
                    graphics.drawCenteredString(font, "ESTABLISHING KINO VIDEO LINK...",
                            center, panelTop + (panelBottom - panelTop) / 2, 0xB9E1CC);
                }
                super.render(graphics, mouseX, mouseY, partialTick);
                return;
            }
            graphics.drawCenteredString(font, Component.translatable("screen.sgjdestiny_dhd.kino_ready"),
                    center, panelTop + 24, 0xD7CFB1);
            drawKinoButton(graphics, center - 112, panelTop + 50, 68,
                    kinoDeployed ? "FOLLOW" : "DEPLOY", mouseX, mouseY);
            drawKinoButton(graphics, center - 34, panelTop + 50, 68, "HOLD", mouseX, mouseY);
            drawKinoButton(graphics, center + 44, panelTop + 50, 68, "RECALL", mouseX, mouseY);
            drawKinoButton(graphics, center - 88, panelTop + 84, 52, "LEFT", mouseX, mouseY);
            drawKinoButton(graphics, center - 26, panelTop + 84, 52, "FWD", mouseX, mouseY);
            drawKinoButton(graphics, center + 36, panelTop + 84, 52, "RIGHT", mouseX, mouseY);
            drawKinoButton(graphics, center - 88, panelTop + 112, 52, "UP", mouseX, mouseY);
            drawKinoButton(graphics, center - 26, panelTop + 112, 52, "BACK", mouseX, mouseY);
            drawKinoButton(graphics, center + 36, panelTop + 112, 52, "DOWN", mouseX, mouseY);
            drawKinoButton(graphics, center - 84, panelTop + 144, 78, "SCAN", mouseX, mouseY);
            drawKinoButton(graphics, center + 6, panelTop + 144, 78, "VIDEO", mouseX, mouseY);
            graphics.drawCenteredString(font, "REMOTE SENSOR CHANNEL", center, panelBottom - 14, 0x708F80);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        Component heading = handheld ? Component.translatable("screen.sgjdestiny_dhd.remote_network") : title;
        graphics.drawCenteredString(font, heading, center, l.screenTop + 9, handheld ? 0xB9E1CC : 0xDCC891);
        long universeCount = entries.stream().filter(entry -> !entry.earth()).count();
        graphics.drawCenteredString(font, Component.translatable(handheld
                ? "screen.sgjdestiny_dhd.remote_subtitle" : "screen.sgjdestiny_dhd.seed_ship_subtitle", universeCount),
                center, l.screenTop + 22, handheld ? 0x70A894 : 0x6F9B80);
        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.sgjdestiny_dhd.no_destinations"), center, l.screenTop + l.screenHeight / 2 - 6, 0xC4AD7A);
            graphics.drawCenteredString(font, Component.translatable("screen.sgjdestiny_dhd.awaiting_seed_data"), center, l.screenTop + l.screenHeight / 2 + 9, 0x668877);
        } else {
            int first = page * PAGE_SIZE;
            for (int row = 0; row < PAGE_SIZE && first + row < entries.size(); row++) {
                int index = first + row, y = l.rowsTop + row * l.rowHeight;
                boolean hover = inside(mouseX, mouseY, l.rowsLeft, y, l.rowsWidth, l.rowHeight - 2);
                graphics.fill(l.rowsLeft, y, l.rowsLeft + l.rowsWidth, y + l.rowHeight - 2,
                        index == selected ? (handheld ? 0xB055806D : 0xB04C6D59)
                                : hover ? (handheld ? 0xA03B5E50 : 0xA0395547)
                                : (handheld ? 0x70203831 : 0x7623342C));
                graphics.fill(l.rowsLeft, y, l.rowsLeft + 2, y + l.rowHeight - 2,
                        entryColor(entries.get(index), index == selected));
                DestinyDialerNetwork.ClientEntry entry = entries.get(index);
                graphics.drawString(font, entry.name(), l.rowsLeft + 8, y + 5, 0xD7CFB1, false);
                drawUniverseAddress(graphics, entry, l.rowsLeft + 8, y + 15);
            }
        }
        if (page > 0) graphics.drawString(font, "< PREVIOUS", l.rowsLeft, l.screenTop + l.screenHeight - 14, 0xA8C2AE, false);
        if ((page + 1) * PAGE_SIZE < entries.size()) graphics.drawString(font, "NEXT >", l.rowsLeft + l.rowsWidth - font.width("NEXT >"), l.screenTop + l.screenHeight - 14, 0xA8C2AE, false);
        Component gearText = gateState == 2 ? Component.translatable("screen.sgjdestiny_dhd.disconnect")
                : gateState == 1 ? Component.translatable("screen.sgjdestiny_dhd.cancel_dial")
                : selected >= 0 ? Component.translatable("screen.sgjdestiny_dhd.select") : Component.translatable("screen.sgjdestiny_dhd.select_destination");
        graphics.drawCenteredString(font, gearText, center, l.screenTop + l.screenHeight - 15,
                gateState > 0 || selected >= 0 ? 0xE3C46E : 0x6E766D);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        Layout l = layout();
        if (handheld) {
            int homeLeft = l.left + l.width * 81 / 100;
            int homeTop = l.top + l.height * 72 / 100;
            if (inside(mouseX, mouseY, homeLeft, homeTop, l.width * 10 / 100, l.height * 13 / 100)) {
                if (videoMode) {
                    exitVideoMode();
                    kinoSection = true;
                } else {
                    kinoSection = false;
                    selected = -1;
                }
                return true;
            }
            int tabLeft = l.left + l.width * 81 / 100;
            int tabWidth = l.width * 10 / 100;
            if (inside(mouseX, mouseY, tabLeft, l.top + l.height * 22 / 100,
                    tabWidth, l.height * 10 / 100)) {
                exitVideoMode();
                kinoSection = false;
                return true;
            }
            if (inside(mouseX, mouseY, tabLeft, l.top + l.height * 33 / 100,
                    tabWidth, l.height * 10 / 100)) {
                exitVideoMode();
                kinoSection = true;
                selected = -1;
                return true;
            }
            if (kinoSection) {
                int panelTop = l.rowsTop + 32;
                int panelRight = l.rowsLeft + l.rowsWidth - 12;
                int panelBottom = l.screenTop + l.screenHeight - 48;
                int center = l.screenLeft + l.screenWidth / 2;
                if (videoMode) {
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                if (inside(mouseX, mouseY, center - 112, panelTop + 50, 68, 24)) {
                    DestinyDialerNetwork.kinoCommand(0); return true;
                }
                if (inside(mouseX, mouseY, center - 34, panelTop + 50, 68, 24)) {
                    DestinyDialerNetwork.kinoCommand(1); return true;
                }
                if (inside(mouseX, mouseY, center + 44, panelTop + 50, 68, 24)) {
                    DestinyDialerNetwork.kinoCommand(2); return true;
                }
                if (inside(mouseX, mouseY, center - 88, panelTop + 84, 52, 24)) { DestinyDialerNetwork.kinoCommand(5); return true; }
                if (inside(mouseX, mouseY, center - 26, panelTop + 84, 52, 24)) { DestinyDialerNetwork.kinoCommand(3); return true; }
                if (inside(mouseX, mouseY, center + 36, panelTop + 84, 52, 24)) { DestinyDialerNetwork.kinoCommand(6); return true; }
                if (inside(mouseX, mouseY, center - 88, panelTop + 112, 52, 24)) { DestinyDialerNetwork.kinoCommand(7); return true; }
                if (inside(mouseX, mouseY, center - 26, panelTop + 112, 52, 24)) { DestinyDialerNetwork.kinoCommand(4); return true; }
                if (inside(mouseX, mouseY, center + 36, panelTop + 112, 52, 24)) { DestinyDialerNetwork.kinoCommand(8); return true; }
                if (inside(mouseX, mouseY, center - 84, panelTop + 144, 78, 24)) { DestinyDialerNetwork.kinoCommand(9); return true; }
                if (inside(mouseX, mouseY, center + 6, panelTop + 144, 78, 24) && kinoDeployed) {
                    videoCameraPos = null;
                    videoMode = true;
                    captureVideoCursor();
                    DestinyDialerNetwork.kinoLook(0, 0, true);
                    DestinyDialerNetwork.kinoCommand(10);
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
        int selectWidth = 120;
        int selectLeft = l.screenLeft + (l.screenWidth - selectWidth) / 2;
        int selectTop = l.screenTop + l.screenHeight - 26;
        if (gateState > 0 && inside(mouseX, mouseY, selectLeft, selectTop, selectWidth, 24)) {
            DestinyDialerNetwork.disconnect(console, directGate);
            onClose();
            return true;
        }
        if (inside(mouseX, mouseY, selectLeft, selectTop, selectWidth, 24) && selected >= 0) {
            DestinyDialerNetwork.ClientEntry entry = entries.get(selected);
            DestinyDialerNetwork.dial(console, directGate, new Address.Immutable(entry.symbols()));
            onClose();
            return true;
        }
        int first = page * PAGE_SIZE;
        for (int row = 0; row < PAGE_SIZE && first + row < entries.size(); row++) {
            int y = l.rowsTop + row * l.rowHeight;
            if (inside(mouseX, mouseY, l.rowsLeft, y, l.rowsWidth, l.rowHeight - 2)) { selected = first + row; return true; }
        }
        int navY = l.screenTop + l.screenHeight - 22;
        if (page > 0 && inside(mouseX, mouseY, l.rowsLeft, navY, 80, 22)) { page--; selected = -1; return true; }
        if ((page + 1) * PAGE_SIZE < entries.size() && inside(mouseX, mouseY, l.rowsLeft + l.rowsWidth - 80, navY, 80, 22)) { page++; selected = -1; return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void captureVideoCursor() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        lastVideoMouseX = width / 2.0;
        lastVideoMouseY = height / 2.0;
        ignoreNextMouseMove = true;
        GLFW.glfwSetCursorPos(window, lastVideoMouseX, lastVideoMouseY);
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        videoCursorCaptured = true;
    }

    private void releaseVideoCursor() {
        if (!videoCursorCaptured) return;
        GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(),
                GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        videoCursorCaptured = false;
    }

    private void exitVideoMode() {
        if (!videoMode) return;
        videoMode = false;
        pendingLookX = pendingLookY = 0;
        releaseVideoCursor();
        DestinyDialerNetwork.kinoCommand(11);
        DestinyDialerNetwork.kinoLook(0, 0, true);
    }

    @Override public void mouseMoved(double mouseX, double mouseY) {
        if (videoMode && videoCursorCaptured) {
            if (ignoreNextMouseMove) {
                ignoreNextMouseMove = false;
            } else {
                double dx = mouseX - lastVideoMouseX, dy = mouseY - lastVideoMouseY;
                pendingLookX += dx;
                pendingLookY += dy;
            }
            lastVideoMouseX = mouseX;
            lastVideoMouseY = mouseY;
            return;
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (videoMode && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            exitVideoMode();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void onClose() {
        exitVideoMode();
        super.onClose();
    }

    @Override public void removed() {
        exitVideoMode();
        if (videoTarget != null) {
            videoTarget.destroyBuffers();
            videoTarget = null;
        }
        super.removed();
    }

    @Override
    public void tick() {
        super.tick();
        if (!handheld || !kinoSection) return;
        flightInputTick++;
        if (videoMode && (pendingLookX != 0 || pendingLookY != 0)) {
            DestinyDialerNetwork.kinoLook((float)(pendingLookX * .18), (float)(pendingLookY * .18), false);
            pendingLookX = pendingLookY = 0;
        }
        if (kinoDeployed && videoMode && flightInputTick % 2 == 0) DestinyDialerNetwork.kinoCommand(10);
        if (flightInputTick % 2 != 0) return;
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_UP)) DestinyDialerNetwork.kinoCommand(3);
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DOWN)) DestinyDialerNetwork.kinoCommand(4);
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT)) DestinyDialerNetwork.kinoCommand(5);
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT)) DestinyDialerNetwork.kinoCommand(6);
    }

    private Layout layout() {
        if (handheld) {
            int maxWidth = Math.min(630, width * 88 / 100);
            int maxHeight = Math.min(420, height * 88 / 100);
            int w = maxWidth, h = w * 2 / 3;
            if (h > maxHeight) { h = maxHeight; w = h * 3 / 2; }
            int left = (width - w) / 2, top = (height - h) / 2;
            int sl = left + w * 12 / 100, st = top + h * 16 / 100;
            int sw = w * 67 / 100, sh = h * 70 / 100;
            int rl = sl + 17, rt = st + 38, rw = sw - 34;
            int rh = Math.max(27, (sh - 60) / PAGE_SIZE);
            return new Layout(left, top, w, h, sl, st, sw, sh, rl, rt, rw, rh,
                    left + w * 82 / 100, top + h * 20 / 100, w * 8 / 100,
                    left + w * 86 / 100, top + h * 78 / 100);
        }
        int maxWidth = Math.min(720, width * 82 / 100);
        int maxHeight = Math.min(480, height * 82 / 100);
        int w = maxWidth, h = w * 2 / 3;
        if (h > maxHeight) { h = maxHeight; w = h * 3 / 2; }
        int left = (width - w) / 2, top = (height - h) / 2;
        int sl = left + w * 19 / 100, st = top + h * 20 / 100, sw = w * 72 / 100, sh = h * 67 / 100;
        int rl = sl + 12, rt = st + 38, rw = sw - 24, rh = Math.max(27, (sh - 58) / PAGE_SIZE), gs = w * 15 / 100;
        return new Layout(left, top, w, h, sl, st, sw, sh, rl, rt, rw, rh, left + w / 50, top + h / 25, gs, left + w * 19 / 200, top + h / 25 + gs);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private void drawKinoButton(GuiGraphics graphics, int x, int y, int width, String text,
                                double mouseX, double mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, width, 24);
        graphics.fill(x, y, x + width, y + 24, hover ? 0xCC496A5A : 0xAA2C493D);
        graphics.fill(x, y, x + 2, y + 24, 0xFFB49A68);
        graphics.drawCenteredString(font, text, x + width / 2, y + 8, hover ? 0xFFF0C8 : 0xD7CFB1);
    }

    public void setKinoDeployed(boolean deployed) {
        this.kinoDeployed = deployed;
    }

    public void setKinoCamera(String dimension, double x, double y, double z, float yaw, float pitch) {
        Vec3 next = new Vec3(x, y, z);
        if (videoCameraPos == null || !this.videoDimension.equals(dimension)) {
            this.videoCameraPos = next;
            this.videoCameraFrom = next;
        } else {
            this.videoCameraFrom = interpolatedVideoCameraPos();
        }
        this.videoDimension = dimension;
        this.videoCameraTarget = next;
        this.videoCameraMoveStarted = System.nanoTime();
        this.videoYaw = yaw;
        this.videoPitch = pitch;
    }

    private Vec3 interpolatedVideoCameraPos() {
        if (videoCameraTarget == null || videoCameraFrom == null) return videoCameraPos;
        double progress = Math.min(1.0, (System.nanoTime() - videoCameraMoveStarted) / 100_000_000.0);
        videoCameraPos = videoCameraFrom.lerp(videoCameraTarget, progress);
        return videoCameraPos;
    }

    public boolean shouldHideKinoFromVideo(Entity entity) {
        return videoMode && videoCameraPos != null && entity instanceof ArmorStand stand
                && stand.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.KINO.get())
                && entity.position().distanceToSqr(videoCameraPos.x, videoCameraPos.y - 1.45, videoCameraPos.z) < 4.0;
    }

    private static int entryColor(DestinyDialerNetwork.ClientEntry entry, boolean selected) {
        if (selected) return 0xFFD8C584;
        return entry.earth() ? 0xFFC69D4B : 0xFF5D806C;
    }

    private void drawUniverseAddress(GuiGraphics graphics, DestinyDialerNetwork.ClientEntry entry, int x, int y) {
        int glyphX = x;
        for (int symbol : entry.symbols()) {
            if (symbol < 0 || symbol > 38) continue;
            ResourceLocation texture = new ResourceLocation("sgjourney",
                    "textures/symbol/universal/universal_" + symbol + ".png");
            graphics.blit(texture, glyphX, y, 12, 12, 0, 0, 32, 32, 32, 32);
            glyphX += 14;
        }
    }

    private record Layout(int left, int top, int width, int height, int screenLeft, int screenTop, int screenWidth,
                          int screenHeight, int rowsLeft, int rowsTop, int rowsWidth, int rowHeight, int gearLeft,
                          int gearTop, int gearSize, int gearCenterX, int gearBottom) {}
    @Override public boolean isPauseScreen() { return false; }
}
