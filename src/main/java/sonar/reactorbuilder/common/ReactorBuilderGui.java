package sonar.reactorbuilder.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;
import sonar.reactorbuilder.ReactorBuilder;
import sonar.reactorbuilder.client.gui.GuiIconButton;
import sonar.reactorbuilder.client.gui.GuiScroller;
import sonar.reactorbuilder.common.files.HellrageJSONOverhaulReader;
import sonar.reactorbuilder.common.files.HellrageJSONUnderhaulReader;
import sonar.reactorbuilder.common.files.ncpf.ThizNewNCPFReader;
import sonar.reactorbuilder.common.reactors.templates.AbstractTemplate;
import sonar.reactorbuilder.network.EnumSyncPacket;
import sonar.reactorbuilder.network.PacketHandler;
import sonar.reactorbuilder.network.PacketTileSync;
import sonar.reactorbuilder.network.templates.TemplateManager;
import sonar.reactorbuilder.registry.RBConfig;
import sonar.reactorbuilder.util.Translate;
import sonar.reactorbuilder.util.Util;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.List;

//TODO TRANSLATION
public class ReactorBuilderGui extends GuiContainer {

    public static final ResourceLocation background = new ResourceLocation(ReactorBuilder.MODID, "textures/gui/reactorbuilder.png");
    public static final ResourceLocation buttons = new ResourceLocation(ReactorBuilder.MODID, "textures/gui/buttons.png");

    //gui size
    public static final int WIDTH = 176, HEIGHT = 219;
    //window size
    public static final int WINDOW_LEFT = 8, WINDOW_TOP = 8, WINDOW_WIDTH = 156, WINDOW_HEIGHT = 75;

    public ReactorBuilderTileEntity builder;

    public ReactorBuilderGuiPages.EnumPages currentPage = ReactorBuilderGuiPages.EnumPages.REACTOR_STATS;
    public GuiScroller scroller;

    public String fileError = "";

    public ReactorBuilderGui(ReactorBuilderTileEntity builder, ReactorBuilderContainer container) {
        super(container);
        xSize = WIDTH;
        ySize = HEIGHT;

        this.builder = builder;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.add(new GuiIconButton(this, 0, guiLeft + 7, guiTop + 89, 72, 0, 18, 18, Translate.PASTE_FILE.t()));
        //buttonList.add(new GuiIconButton(this, 1, guiLeft + 25, guiTop + 89, 90, 0, 18, 18, "Export file"));
        buttonList.add(new GuiIconButton(this, 2, guiLeft + 133, guiTop + 89, 108, 0, 18, 18, Translate.BUILD_REACTOR.t()));
        buttonList.add(new GuiIconButton(this, 3, guiLeft + 151, guiTop + 89, 126, 0, 18, 18, Translate.DESTROY_REACTOR.t()));

        buttonList.add(new GuiIconButton(this, 4, guiLeft - 6, guiTop + 40, 180, 0, 5, 8, Translate.PREV_PAGE.t()));
        buttonList.add(new GuiIconButton(this, 5, guiLeft + WIDTH + 6 - 5, guiTop + 40, 198, 0, 5, 8, Translate.NEXT_PAGE.t()));

        scroller = new GuiScroller(this.guiLeft + 8 + 161 - 4, this.guiTop + 8, 75, 4);

        ///load page / scroll position from tile entity client cache
        currentPage = ReactorBuilderGuiPages.EnumPages.values()[builder.page];
        scroller.currentScroll = builder.scroll;

    }


    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {

        ///render page titles
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.75, 0.75, 0.75);
        drawCenteredString(fontRenderer, getBuilderTotalProgress(), (int) ((WIDTH / 2) / 0.75), (int) (94 / 0.75), 16777215);
        drawCenteredString(fontRenderer, getBuilderEnergyText(), (int) ((WIDTH / 2) / 0.75), (int) (124 / 0.75), 16777215);
        GlStateManager.popMatrix();

        ///start scissor
        ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
        double scaleW = Minecraft.getMinecraft().displayWidth / res.getScaledWidth_double();
        double scaleH = Minecraft.getMinecraft().displayHeight / res.getScaledHeight_double();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) ((guiLeft + WINDOW_LEFT) * scaleW), (int) (Minecraft.getMinecraft().displayHeight - ((guiTop + WINDOW_TOP + WINDOW_HEIGHT) * scaleH)), (int) (WINDOW_WIDTH * scaleW), (int) (WINDOW_HEIGHT * scaleH));

        ///render page
        GlStateManager.pushMatrix();
        int pageSize = Math.max(0, currentPage.page.getPageSize(this) - WINDOW_HEIGHT);
        GlStateManager.translate(WINDOW_LEFT, WINDOW_TOP - (scroller.getCurrentScroll() * pageSize), 0);
        currentPage.page.renderPage(this, mouseX - guiLeft - WINDOW_LEFT, mouseY - guiTop - WINDOW_TOP - (scroller.getCurrentScroll() * pageSize));
        GlStateManager.popMatrix();

        ///stop scissor
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        //draw hovering button texts
        for (GuiButton guiButton : this.buttonList) {
            guiButton.drawButtonForegroundLayer(mouseX - guiLeft, mouseY - guiTop);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

        int progress = builder.getProgress();
        int totalProgress = builder.getTotalProgress();

        mc.getTextureManager().bindTexture(background);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        if (progress != 0) {
            drawTexturedModalRectDouble(guiLeft + 6, guiTop + 110, 0, 219, (progress * 164D) / totalProgress, 6);
        }

        drawTexturedModalRectDouble(guiLeft + 6, guiTop + 122, 0, 225, (builder.shouldUseEnergy() ? builder.energyStorage.getEnergyStored() : builder.energyStorage.getMaxEnergyStored()) * 164D / builder.energyStorage.getMaxEnergyStored(), 10);
        drawTexturedModalRectDouble(scroller.left, scroller.top + (int) ((float) (scroller.length - 11) * scroller.getCurrentScroll()), 0, 235, 4, 11);
    }

    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled) {
            switch (button.id) {
                case 0:
                    pasteTemplateFile();
                    break;
                case 1:
                    ///EXPORT //TODO
                    break;
                case 2:
                    //BUILD
                    PacketHandler.INSTANCE.sendToServer(new PacketTileSync(builder, EnumSyncPacket.TOGGLE_BUILDING));
                    break;
                case 3:
                    //DESTROY
                    PacketHandler.INSTANCE.sendToServer(new PacketTileSync(builder, EnumSyncPacket.TOGGLE_DESTROYING));
                    break;
                case 4:
                    //prev
                    if (builder.template == null) {
                        return;
                    }
                    int prev = currentPage.ordinal() - 1;
                    if (prev >= 0) {
                        currentPage = ReactorBuilderGuiPages.EnumPages.values()[prev];
                    } else {
                        currentPage = ReactorBuilderGuiPages.EnumPages.values()[ReactorBuilderGuiPages.EnumPages.values().length - 1];
                    }
                    onPageChanged();

                    scroller.currentScroll = 0;
                    onScrollerChanged();
                    break;
                case 5:
                    //next
                    if (builder.template == null) {
                        return;
                    }
                    int next = currentPage.ordinal() + 1;
                    if (next < ReactorBuilderGuiPages.EnumPages.values().length) {
                        currentPage = ReactorBuilderGuiPages.EnumPages.values()[next];
                    } else {
                        currentPage = ReactorBuilderGuiPages.EnumPages.values()[0];
                    }
                    onPageChanged();

                    scroller.currentScroll = 0;
                    onScrollerChanged();
                    break;
            }
        }
    }

    public void pasteTemplateFile() {
        AbstractTemplate newTemplate = null;
        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                Object obj = transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (obj instanceof List && !((List<?>) obj).isEmpty() && ((List<?>) obj).get(0) instanceof File) {
                    File file = (File) ((List<?>) obj).get(0);
                    String extension = Util.getFileExtension(file);
                    if (!extension.isEmpty()) {
                        if (extension.equalsIgnoreCase("ncpf")) {
                            if (RBConfig.allowThizNCPF) {
                                newTemplate = ThizNewNCPFReader.INSTANCE.readTemplate(file);
                                if (newTemplate == null) {
                                    fileError = ThizNewNCPFReader.INSTANCE.error;
                                }
                            } else {
                                fileError = Translate.FEATURE_DISABLED.format("NCPF");
                            }
                        } else if (extension.equalsIgnoreCase("json")) {
                            if (RBConfig.allowHellrageJSON) {
                                if (ReactorBuilder.isOverhaul()) {
                                    newTemplate = HellrageJSONOverhaulReader.INSTANCE.readTemplate(file);
                                    if (newTemplate == null) {
                                        fileError = HellrageJSONOverhaulReader.INSTANCE.error;
                                    }
                                } else {
                                    newTemplate = HellrageJSONUnderhaulReader.INSTANCE.readTemplate(file);
                                    if (newTemplate == null) {
                                        fileError = HellrageJSONUnderhaulReader.INSTANCE.error;
                                    }
                                }
                            } else {
                                fileError = Translate.FEATURE_DISABLED.format("JSON");
                            }
                        } else {
                            fileError = Translate.FILE_INVALID_EXTENSION + ": " + extension;
                        }
                    } else {
                        fileError = Translate.FILE_MISSING_EXTENSION.t();
                    }
                }
            } else {
                fileError = Translate.FILE_NO_FILE.t();
            }
        } catch (Exception ignored) {
            fileError = Translate.FILE_ERROR.t();
            ReactorBuilder.logger.error("Error reading reactor file", ignored);
        }

        if (newTemplate != null) {
            newTemplate.templateID = newTemplate.fileName.hashCode(); //allows the server to receive to files simultaneously from different clients - this id won't ever be saved
            TemplateManager.getDownloadHandler(true).sendLocalTemplate(newTemplate, null, builder);
            fileError = "";
        }
    }


    /**
     * copy of vanilla method, but with doubles instead of ints
     */
    public void drawTexturedModalRectDouble(double x, double y, double textureX, double textureY, double width, double height) {
        float f = 0.00390625F;
        float f1 = 0.00390625F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos((x + 0), (y + height), this.zLevel).tex(((float) (textureX + 0) * f), ((float) (textureY + height) * f1)).endVertex();
        bufferbuilder.pos((x + width), (y + height), this.zLevel).tex(((float) (textureX + width) * f), ((float) (textureY + height) * f1)).endVertex();
        bufferbuilder.pos((x + width), (y + 0), this.zLevel).tex(((float) (textureX + width) * f), ((float) (textureY + 0) * f1)).endVertex();
        bufferbuilder.pos((x + 0), (y + 0), this.zLevel).tex(((float) (textureX + 0) * f), ((float) (textureY + 0) * f1)).endVertex();
        tessellator.draw();
    }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        scroller.handleMouse(true, currentPage.page.getPageSize(this));
        onScrollerChanged();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (guiLeft + WINDOW_LEFT <= mouseX && mouseX <= guiLeft + WINDOW_LEFT + WINDOW_WIDTH && guiTop + WINDOW_TOP <= mouseY && mouseY <= guiTop + WINDOW_TOP + WINDOW_HEIGHT) {
            currentPage.page.onClicked(this, mouseX - guiLeft - WINDOW_LEFT, mouseY - guiTop - WINDOW_TOP + (scroller.getCurrentScroll() * Math.max(0, currentPage.page.getPageSize(this) - WINDOW_HEIGHT)));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        super.renderHoveredToolTip(mouseX, mouseY);
        scroller.drawScreen(mouseX, mouseY, true);
        onScrollerChanged();
    }

    /// page position

    public void onScrollerChanged() {
        builder.scroll = scroller.currentScroll;
    }

    public void onPageChanged() {
        builder.page = currentPage.ordinal();
    }

    /// general

    public String getBuilderStatus() {
        if (!fileError.isEmpty())
            return TextFormatting.RED + fileError;
        if (builder.template == null)
            return Translate.FILE_NO_TEMPLATE.t();
        if (!builder.error.isEmpty())
            return TextFormatting.RED + builder.error;
        if (builder.isBuilding)
            return TextFormatting.GREEN + String.format("%s %s/%s", builder.getPassName(), builder.getPassProgress(), builder.getPassTotal());
        if (builder.isDestroying)
            return TextFormatting.GREEN + String.format("%s %s/%s", Translate.PASS_REMOVING_COMPONENTS.t(), builder.getProgress(), builder.getTotalProgress());
        if (builder.getProgress() == builder.getTotalProgress())
            return Translate.STATUS_FINISHED.t();
        return Translate.STATUS_IDLE.t();
    }

    public String getBuilderTotalProgress() {
        int totalProgress = builder.getTotalProgress();
        int percentage = totalProgress != 0 ? builder.getProgress() * 100 / totalProgress : 0;
        return (builder.isDestroying ? Translate.STATUS_DESTROYING.t() : Translate.STATUS_BUILDING.t()) + ": " + percentage + " %";
    }

    private String getBuilderEnergyText() {
        if (!builder.shouldUseEnergy()) {
            return Translate.ENERGY_INFINITE.t() + " RF";
        }
        return String.format("%s RF", builder.energyStorage.getEnergyStored());
    }

    /// helpers

    public FontRenderer getFont() {
        return fontRenderer;
    }

    public RenderItem getRenderItem() {
        return itemRender;
    }
}